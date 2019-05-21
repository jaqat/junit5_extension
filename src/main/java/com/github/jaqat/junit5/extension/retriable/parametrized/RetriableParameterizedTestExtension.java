package com.github.jaqat.junit5.extension.retriable.parametrized;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.junit.platform.commons.util.AnnotationUtils.*;

/**
 * @since 5.0
 */
class RetriableParameterizedTestExtension implements TestTemplateInvocationContextProvider {
    
    private static final String METHOD_CONTEXT_KEY = "context";
    
    private List<RetriableParameterizedTestInvocationContext> parameterizedTestInvocationContextList;
    
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        if (!context.getTestMethod().isPresent()) {
            return false;
        }
        
        Method testMethod = context.getTestMethod().get();
        if (!isAnnotated(testMethod, RetriableParameterizedTest.class)) {
            return false;
        }
        
        RetriableParameterizedTestMethodContext methodContext = new RetriableParameterizedTestMethodContext(testMethod);
        
        Preconditions.condition(methodContext.hasPotentiallyValidSignature(),
                () -> String.format(
                        "@RetriableParameterizedTest method [%s] declares formal parameters in an invalid order: "
                                + "argument aggregators must be declared after any indexed arguments "
                                + "and before any arguments resolved by another ParameterResolver.",
                        testMethod.toGenericString()));
        
        getStore(context).put(METHOD_CONTEXT_KEY, methodContext);
        return true;
    }
    
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {
        
        // Get parameters from @RetriableParameterizedTest
        RetriableParameterizedTest retriableParameterizedTestAnnotation =
                extensionContext
                        .getTestMethod()
                        .flatMap(testMethods -> findAnnotation(testMethods, RetriableParameterizedTest.class))
                        .orElseThrow(
                                () -> new IllegalStateException("The extension should not be executed ")
                        );
        
        List<Class<? extends Throwable>> repeatableExceptions = new ArrayList<>(Arrays.asList(retriableParameterizedTestAnnotation.exceptions()));
        repeatableExceptions.add(TestAbortedException.class);
        int repetitions = retriableParameterizedTestAnnotation.repeats();
        
        
        Method templateMethod = extensionContext.getRequiredTestMethod();
        String displayName = extensionContext.getDisplayName();
        RetriableParameterizedTestMethodContext methodContext = getStore(extensionContext)//
                .get(METHOD_CONTEXT_KEY, RetriableParameterizedTestMethodContext.class);
        RetriableParameterizedTestNameFormatter formatter = createNameFormatter(templateMethod, displayName);
        AtomicLong invocationCount = new AtomicLong(0);
        
        // @formatter:off
        parameterizedTestInvocationContextList =
                findRepeatableAnnotations(templateMethod, ArgumentsSource.class)
                        .stream()
                        .map(ArgumentsSource::value)
                        .map(this::instantiateArgumentsProvider)
                        .map(provider -> AnnotationConsumerInitializer.initialize(templateMethod, provider))
                        .flatMap(provider -> arguments(provider, extensionContext))
                        .map(Arguments::get)
                        .map(arguments -> consumedArguments(arguments, methodContext))
                        .map(arguments -> new RetriableParameterizedTestInvocationContext(formatter, methodContext, repeatableExceptions, repetitions, arguments))
                        .peek(invocationContext -> invocationCount.incrementAndGet())
                        .onClose(() ->
                                Preconditions.condition(invocationCount.get() > 0,
                                        "Configuration error: You must configure at least one set of arguments for this @RetriableParameterizedTest"))
                        .collect(Collectors.toList());
        // @formatter:on
        
        Spliterator<TestTemplateInvocationContext> spliterator =
                spliteratorUnknownSize(new RetriableParameterizedTestExtension.TestTemplateIterator(), Spliterator.NONNULL);
        return stream(spliterator, false);
    }
    
    @SuppressWarnings("ConstantConditions")
    private ArgumentsProvider instantiateArgumentsProvider(Class<? extends ArgumentsProvider> clazz) {
        try {
            return ReflectionUtils.newInstance(clazz);
        } catch (Exception ex) {
            if (ex instanceof NoSuchMethodException) {
                String message = String.format("Failed to find a no-argument constructor for ArgumentsProvider [%s]. "
                                + "Please ensure that a no-argument constructor exists and "
                                + "that the class is either a top-level class or a static nested class",
                        clazz.getName());
                throw new JUnitException(message, ex);
            }
            throw ex;
        }
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(RetriableParameterizedTestExtension.class, context.getRequiredTestMethod()));
    }
    
    private RetriableParameterizedTestNameFormatter createNameFormatter(Method templateMethod, String displayName) {
        RetriableParameterizedTest retriableParameterizedTest = findAnnotation(templateMethod, RetriableParameterizedTest.class).get();
        String pattern = Preconditions.notBlank(retriableParameterizedTest.name().trim(),
                () -> String.format(
                        "Configuration error: @RetriableParameterizedTest on method [%s] must be declared with a non-empty name.",
                        templateMethod));
        return new RetriableParameterizedTestNameFormatter(pattern, displayName);
    }
    
    protected static Stream<? extends Arguments> arguments(ArgumentsProvider provider, ExtensionContext context) {
        try {
            return provider.provideArguments(context);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsUncheckedException(e);
        }
    }
    
    private Object[] consumedArguments(Object[] arguments, RetriableParameterizedTestMethodContext methodContext) {
        int parameterCount = methodContext.getParameterCount();
        return methodContext.hasAggregator() ? arguments
                : (arguments.length > parameterCount ? Arrays.copyOf(arguments, parameterCount) : arguments);
    }
    
    /**
     * TestTemplateIterator
     */
    class TestTemplateIterator implements Iterator<TestTemplateInvocationContext> {
        int currentIndex = 0;
        
        @Override
        public boolean hasNext() {
            if (currentIndex < parameterizedTestInvocationContextList.size()) {
                if (parameterizedTestInvocationContextList.get(currentIndex).hasNext()) {
                    return true;
                } else {
                    if (++currentIndex < parameterizedTestInvocationContextList.size()) {
                        return parameterizedTestInvocationContextList.get(currentIndex).hasNext();
                    }
                }
            }
            return false;
        }
        
        @Override
        public TestTemplateInvocationContext next() {
            return parameterizedTestInvocationContextList.get(currentIndex).withDisplayNameInvocationIndex(currentIndex + 1).next();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
}
