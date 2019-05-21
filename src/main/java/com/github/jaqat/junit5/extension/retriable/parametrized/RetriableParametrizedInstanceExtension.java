package com.github.jaqat.junit5.extension.retriable.parametrized;

import com.github.jaqat.junit5.extension.retriable.single.RetriableException;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class RetriableParametrizedInstanceExtension implements AfterTestExecutionCallback, TestExecutionExceptionHandler {
    
    private AtomicInteger currentRepetition;
    private Integer maximumRepeats;
    private int minSuccess;
    private List<Class<? extends Throwable>> repeatableExceptions;
    private AtomicBoolean repeatableExceptionAppeared;
    private final List<Boolean> historyExceptionAppear;
    
    RetriableParametrizedInstanceExtension(
            AtomicInteger currentRepetition,
            int maximumRepeats,
            int minSuccess,
            List<Class<? extends Throwable>> assertionClasses,
            AtomicBoolean repeatableExceptionAppeared,
            List<Boolean> historyExceptionAppear
    ) {
        this.currentRepetition = currentRepetition;
        this.maximumRepeats = maximumRepeats;
        this.minSuccess = minSuccess;
        this.repeatableExceptions = assertionClasses;
        this.repeatableExceptionAppeared = repeatableExceptionAppeared;
        this.historyExceptionAppear = historyExceptionAppear;
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
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
    
    private boolean appearedExceptionDoesNotAllowRepetitions(Throwable appearedException) {
        return repeatableExceptions.stream().noneMatch(ex -> ex.isAssignableFrom(appearedException.getClass()));
    }

    private boolean isMinSuccessTargetStillReachable(long minSuccessCount) {
        return historyExceptionAppear.stream().filter(bool -> bool).count() < maximumRepeats - minSuccessCount;
    }
    
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (appearedExceptionDoesNotAllowRepetitions(throwable)) {
            throw throwable;
        }
        this.repeatableExceptionAppeared.set(true);
        long currentSuccessCount = historyExceptionAppear.stream().filter(exceptionAppeared -> !exceptionAppeared).count();
        if (currentSuccessCount < minSuccess) {
            if (isMinSuccessTargetStillReachable(minSuccess)) {
                throw new TestAbortedException("Do not fail completely but repeat the test", throwable);
            } else {
                throw throwable;
            }
        }
    }
}
