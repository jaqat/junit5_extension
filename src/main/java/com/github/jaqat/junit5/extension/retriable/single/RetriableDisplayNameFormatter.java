package com.github.jaqat.junit5.extension.retriable.single;


class RetriableDisplayNameFormatter {
    
    private final String pattern;
    private final String displayName;
    
    RetriableDisplayNameFormatter(String pattern, String displayName) {
        this.pattern = pattern;
        this.displayName = displayName;
    }
    
    String format(int currentRepetition, int totalRepetitions, boolean repeatableExceptionAppeared) {
        if (currentRepetition == 1) {
            return displayName;
        } else {
            return displayName.concat(" [Retry " + currentRepetition + "] ");
        }
    }
    
}
