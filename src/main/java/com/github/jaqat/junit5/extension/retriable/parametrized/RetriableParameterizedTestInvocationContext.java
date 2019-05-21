package com.github.jaqat.junit5.extension.retriable.parametrized;

import com.github.jaqat.junit5.extension.retriable.common.RepeatExecutionCondition;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.toIntExact;

/**
 * @since 5.0
 */
class RetriableParameterizedTestInvocationContext implements TestTemplateInvocationContext, Iterator<TestTemplateInvocationContext> {
    
    private final int minSuccess = 1;
    
    private final Integer maxRepetitions;
    private final List<Class<? extends Throwable>> repeatableExceptions;
    
    private AtomicInteger currentRepetition = new AtomicInteger(0);
    private AtomicBoolean repeatableExceptionAppeared = new AtomicBoolean();
    private List<Boolean> historyExceptionAppear = Collections.synchronizedList(new ArrayList<>());
    
    private final RetriableParameterizedTestNameFormatter formatter;
    private final RetriableParameterizedTestMethodContext methodContext;
    private final Object[] arguments;
    
    private int displayNameInvocationIndex;
    
    RetriableParameterizedTestInvocationContext(
            RetriableParameterizedTestNameFormatter formatter,
            RetriableParameterizedTestMethodContext methodContext,
            List<Class<? extends Throwable>> repeatableExceptions,
            int repetitions,
            Object[] arguments
    ) {
        this.formatter = formatter;
        this.methodContext = methodContext;
        this.repeatableExceptions = repeatableExceptions;
        this.maxRepetitions = repetitions;
        this.arguments = arguments;
    }
    
    RetriableParameterizedTestInvocationContext withDisplayNameInvocationIndex(int index){
        this.displayNameInvocationIndex = index;
        return this;
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
    
    @Override
    public String getDisplayName(int invocationIndex) {
        return this.formatter.format(displayNameInvocationIndex, repeatableExceptionAppeared.get(), currentRepetition.get(), this.arguments);
    }
    
    @Override
    public List<Extension> getAdditionalExtensions() {
        List<Extension> additionalExtensions = new ArrayList<>();
        
        additionalExtensions.add(
                new RetriableParameterizedTestParameterResolver(this.methodContext, this.arguments)
        );
        
        additionalExtensions.add(
                new RepeatExecutionCondition(
                        currentRepetition.get(),
                        maxRepetitions,
                        minSuccess,
                        toIntExact(historyExceptionAppear.stream().filter(b -> !b).count()),
                        repeatableExceptionAppeared.get()
                )
        );
        
        additionalExtensions.add(
                new RetriableParametrizedInstanceExtension(
                        currentRepetition,
                        maxRepetitions,
                        minSuccess,
                        repeatableExceptions,
                        repeatableExceptionAppeared,
                        historyExceptionAppear
                )
        );
        
        return additionalExtensions;
    }
    
    @Override
    public boolean hasNext() {
        if (currentRepetition.get() == 0) {
            return true;
        }
        return historyExceptionAppear.stream().anyMatch(ex -> ex) && currentRepetition.get() < maxRepetitions;
    }
    
    @Override
    public TestTemplateInvocationContext next() {
        if (hasNext()) {
            currentRepetition.incrementAndGet();
            return this;
        }
        throw new NoSuchElementException();
    }
    
}
