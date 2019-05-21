package com.github.jaqat.junit5.extension.retriable.single;

import com.github.jaqat.junit5.extension.retriable.parametrized.RetriableParameterizedTest;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.util.Preconditions;
import org.opentest4j.TestAbortedException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.toIntExact;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

public class RetriableTestExtension implements TestTemplateInvocationContextProvider, BeforeTestExecutionCallback,
        AfterTestExecutionCallback, TestExecutionExceptionHandler {
    
    private Integer totalRepeats;
    private int minSuccess = 1;
    private List<Class<? extends Throwable>> repeatableExceptions;
    private boolean repeatableExceptionAppeared = false;
    private RetriableDisplayNameFormatter formatter;
    private List<Boolean> historyExceptionAppear;
    
    /**
     * Check that test method contain {@link RetriableTest} annotation
     *
     * @param extensionContext - encapsulates the context in which the current test or container is being executed
     * @return true/false
     */
    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return isAnnotated(extensionContext.getTestMethod(), RetriableTest.class);
    }
    
    
    /**
     * Context call TestTemplateInvocationContext
     *
     * @param extensionContext - Test Class Context
     * @return Stream of TestTemplateInvocationContext
     */
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        Preconditions.notNull(extensionContext.getTestMethod().orElse(null), "Test method must not be null");
        
        String displayName = extensionContext.getDisplayName();
        
        RetriableTest annotationParams = extensionContext.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, RetriableTest.class))
                .orElseThrow(() -> new RetriableException("The extension should not be executed "
                        + "unless the test method is annotated with @REtriableTest."));
        totalRepeats = annotationParams.repeats();
        formatter = displayNameFormatter(annotationParams, displayName);
        
        historyExceptionAppear = Collections.synchronizedList(new ArrayList<>());
        Preconditions.condition(totalRepeats > 0, "Total repeats must be higher than 0");
        Preconditions.condition(minSuccess >= 1, "Total minimum success must be higher or equals than 1");
        
        //Convert logic of repeated handler to spliterator
        Spliterator<TestTemplateInvocationContext> spliterator =
                spliteratorUnknownSize(new TestTemplateIterator(), Spliterator.NONNULL);
        return stream(spliterator, false);
    }
    
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        repeatableExceptions = Stream.of(context.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, RetriableTest.class))
                .orElseThrow(() -> new IllegalStateException("The extension should not be executed "))
                .exceptions()
        ).collect(Collectors.toList());
        repeatableExceptions.add(TestAbortedException.class);
    }
    
    /**
     * Check if exceptions that will appear in test same as we wait
     *
     * @param extensionContext - Test Class Context
     * @throws Exception - error if occurred
     */
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        boolean exceptionAppeared = exceptionAppeared(extensionContext);
        historyExceptionAppear.add(exceptionAppeared);
    }
    
    private boolean exceptionAppeared(ExtensionContext extensionContext) {
        Class<? extends Throwable> exception = extensionContext.getExecutionException()
                .orElse(new RetriableException("There is no exception in context")).getClass();
        return repeatableExceptions.stream()
                .anyMatch(ex -> ex.isAssignableFrom(exception) && !RetriableException.class.isAssignableFrom(exception));
    }
    
    /**
     * Handler for display name
     *
     * @param test        - RepeatedIfExceptionsTest annotation
     * @param displayName - Name that will be represent to report
     * @return RepeatedIfExceptionsDisplayNameFormatter {@link RetriableDisplayNameFormatter}
     */
    private RetriableDisplayNameFormatter displayNameFormatter(RetriableTest test, String displayName) {
        String pattern = Preconditions.notBlank(test.name().trim(),
                () -> "Configuration error: @RepeatedByExceptionsExtension must be declared with a non-empty name.");
        return new RetriableDisplayNameFormatter(pattern, displayName);
    }
    
    private RetriableDisplayNameFormatter displayNameFormatter(RetriableParameterizedTest test, String displayName) {
        String pattern = Preconditions.notBlank(test.name().trim(),
                () -> "Configuration error: @RepeatedByExceptionsExtension must be declared with a non-empty name.");
        return new RetriableDisplayNameFormatter(pattern, displayName);
    }
    
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (appearedExceptionDoesNotAllowRepetitions(throwable)) {
            throw throwable;
        }
        repeatableExceptionAppeared = true;
        long currentSuccessCount = historyExceptionAppear.stream().filter(exceptionAppeared -> !exceptionAppeared).count();
        if (currentSuccessCount < minSuccess) {
            if (isMinSuccessTargetStillReachable(minSuccess)) {
                throw new TestAbortedException("Do not fail completely but repeat the test", throwable);
            } else {
                throw throwable;
            }
        }
    }
    
    private boolean appearedExceptionDoesNotAllowRepetitions(Throwable appearedException) {
        return repeatableExceptions.stream().noneMatch(ex -> ex.isAssignableFrom(appearedException.getClass()));
    }
    
    private boolean isMinSuccessTargetStillReachable(long minSuccessCount) {
        return historyExceptionAppear.stream().filter(bool -> bool).count() < totalRepeats - minSuccessCount;
    }
    
    /**
     * TestTemplateIterator (Repeat test if it failed)
     */
    class TestTemplateIterator implements Iterator<TestTemplateInvocationContext> {
        int currentIndex = 0;
        
        @Override
        public boolean hasNext() {
            if (currentIndex == 0) {
                return true;
            }
            return historyExceptionAppear.stream().anyMatch(ex -> ex) && currentIndex < totalRepeats;
        }
        
        @Override
        public TestTemplateInvocationContext next() {
            int successfulTestRepetitionsCount = toIntExact(historyExceptionAppear.stream().filter(b -> !b).count());
            if (hasNext()) {
                currentIndex++;
                return new RetriableInvocationContext(currentIndex, totalRepeats,
                        successfulTestRepetitionsCount, minSuccess, repeatableExceptionAppeared, formatter);
            }
            throw new NoSuchElementException();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
}
