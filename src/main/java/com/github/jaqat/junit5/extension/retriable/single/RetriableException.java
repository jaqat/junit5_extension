package com.github.jaqat.junit5.extension.retriable.single;

public class RetriableException extends RuntimeException {

    public RetriableException(String message) {
        super(message);
    }
}
