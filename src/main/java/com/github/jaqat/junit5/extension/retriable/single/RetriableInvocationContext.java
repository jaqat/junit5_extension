package com.github.jaqat.junit5.extension.retriable.single;

import com.github.jaqat.junit5.extension.retriable.common.RepeatExecutionCondition;
import org.junit.jupiter.api.extension.*;

import java.util.List;

import static java.util.Collections.singletonList;

public class RetriableInvocationContext implements TestTemplateInvocationContext {

    private final int currentRepetition;
    private final int totalRepetitions;
    private final int successfulTestRepetitionsCount;
    private final int minSuccess;
    private final boolean repeatableExceptionAppeared;
    private final RetriableDisplayNameFormatter formatter;

    RetriableInvocationContext(int currentRepetition, int totalRepetitions, int successfulTestRepetitionsCount,
                               int minSuccess, boolean repeatableExceptionAppeared,
                               RetriableDisplayNameFormatter formatter) {
        this.currentRepetition = currentRepetition;
        this.totalRepetitions = totalRepetitions;
        this.successfulTestRepetitionsCount = successfulTestRepetitionsCount;
        this.minSuccess = minSuccess;
        this.repeatableExceptionAppeared = repeatableExceptionAppeared;
        this.formatter = formatter;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        String name =  this.formatter.format(this.currentRepetition, this.totalRepetitions, this.repeatableExceptionAppeared);
        return name;
//        return this.formatter.format(this.currentRepetition, this.totalRepetitions, this.repeatableExceptionAppeared);
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return singletonList(new RepeatExecutionCondition(currentRepetition, totalRepetitions, minSuccess,
                successfulTestRepetitionsCount, repeatableExceptionAppeared));
    }
    
}
