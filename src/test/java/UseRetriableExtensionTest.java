import com.github.jaqat.junit5.extension.retriable.parametrized.RetriableParameterizedTest;
import com.github.jaqat.junit5.extension.retriable.single.RetriableTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;

public class UseRetriableExtensionTest {
    
    @RetriableTest(repeats = 3)
    @DisplayName("@RetriableTest: check retries by default exceptions")
    void retriableByDefaultExceptions(){
        assertTrue(false);
    }
    
    @RetriableTest(repeats = 3, exceptions = {IllegalStateException.class})
    @DisplayName("@RetriableTest: check retries if exception don't match")
    void doNotRetryIfFailsWIthAnotherExtension(){
        assertTrue(false);
    }
    
    @RetriableTest(repeats = 3)
    @DisplayName("@RetriableTest: check no retry if test passed")
    void doNotRetryIfPassed(){
        assertTrue(true);
    }
    
    private static Stream<Arguments> parametrizedCases(){
        return Stream.of(
                of(
                        "First case",
                        true
                ),
                of(
                        "Second case",
                        false
                )
        );
    }
    
    @RetriableParameterizedTest(name = "@RetriableParameterizedTest: check retry by default exceptions. {0}",repeats = 3, exceptions = AssertionError.class)
    @MethodSource("parametrizedCases")
    void parametrizedCheck(String title, boolean condition){
        assertTrue(condition);
    }
}
