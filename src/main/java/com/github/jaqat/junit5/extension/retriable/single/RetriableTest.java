package com.github.jaqat.junit5.extension.retriable.single;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(RetriableTestExtension.class)
public @interface RetriableTest {
    
    String DISPLAY_NAME_PLACEHOLDER = "{displayName}";
    
    String DEFAULT_DISPLAY_NAME = DISPLAY_NAME_PLACEHOLDER;

    /**
     * Pool of exceptions
     * @return Exception that handlered
     */
    Class<? extends Throwable>[] exceptions() default Throwable.class;

    /**
     * Number of repeats
     * @return N-times repeat test if it failed
     */
    int repeats();

    /**
     * Minimum success
     * @return After n-times of passed tests will disable all remaining repeats.
     */
    int minSuccess() default 1;

    /**
     * Display name for test method
     * @return Short name
     */
    String name() default DEFAULT_DISPLAY_NAME;
}
