package com.github.jaqat.junit5.extension.retriable.parametrized;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "5.0")
@TestTemplate
@ExtendWith(RetriableParameterizedTestExtension.class)
public @interface RetriableParameterizedTest {
    
    String DISPLAY_NAME_PLACEHOLDER = "{displayName}";
    
    String INDEX_PLACEHOLDER = "{index}";
    
    String ARGUMENTS_PLACEHOLDER = "{arguments}";
    
    String DEFAULT_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] " + ARGUMENTS_PLACEHOLDER;
    
    /**
     * The display name to be used for individual invocations of the
     * parameterized test; never blank or consisting solely of whitespace.
     * <p>
     * <p>Defaults to {@link #DEFAULT_DISPLAY_NAME}.
     * <p>
     * <h4>Supported placeholders</h4>
     * <ul>
     * <li>{@link #DISPLAY_NAME_PLACEHOLDER}</li>
     * <li>{@link #INDEX_PLACEHOLDER}</li>
     * <li>{@link #ARGUMENTS_PLACEHOLDER}</li>
     * <li><code>{0}</code>, <code>{1}</code>, etc.: an individual argument (0-based)</li>
     * </ul>
     * <p>
     * <p>For the latter, you may use {@link java.text.MessageFormat} patterns
     * to customize formatting.
     *
     * @see java.text.MessageFormat
     */
    String name() default DEFAULT_DISPLAY_NAME;
    
    /**
     * Pool of exceptions
     *
     * @return Exception that handlered
     */
    Class<? extends Throwable>[] exceptions() default Throwable.class;
    
    /**
     * Number of repeats
     *
     * @return N-times repeat test if it failed
     */
    int repeats();
    
}
