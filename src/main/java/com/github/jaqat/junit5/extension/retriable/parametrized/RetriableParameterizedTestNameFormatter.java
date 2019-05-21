package com.github.jaqat.junit5.extension.retriable.parametrized;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.StringUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.IntStream;

import static com.github.jaqat.junit5.extension.retriable.parametrized.RetriableParameterizedTest.ARGUMENTS_PLACEHOLDER;
import static com.github.jaqat.junit5.extension.retriable.parametrized.RetriableParameterizedTest.DISPLAY_NAME_PLACEHOLDER;
import static com.github.jaqat.junit5.extension.retriable.parametrized.RetriableParameterizedTest.INDEX_PLACEHOLDER;
import static java.util.stream.Collectors.joining;

/**
 * @since 5.0
 */
class RetriableParameterizedTestNameFormatter {

	private final String pattern;
	private final String displayName;

	RetriableParameterizedTestNameFormatter(String pattern, String displayName) {
		this.pattern = pattern;
		this.displayName = displayName;
	}

	String format(int invocationIndex, boolean repeatableExceptionAppeared, int currentRepetition,  Object... arguments) {
		String pattern = prepareMessageFormatPattern(invocationIndex, repeatableExceptionAppeared, currentRepetition, arguments);
		Object[] humanReadableArguments = makeReadable(arguments);
		return formatSafely(pattern, humanReadableArguments);
	}

	private String prepareMessageFormatPattern(int invocationIndex,  boolean repeatableExceptionAppeared, int currentRepetition, Object[] arguments) {
		String result = pattern
				.replace(DISPLAY_NAME_PLACEHOLDER, this.displayName)
				.replace(INDEX_PLACEHOLDER, String.valueOf(invocationIndex));

		if (result.contains(ARGUMENTS_PLACEHOLDER)) {
			String replacement = IntStream.range(0, arguments.length)
					.mapToObj(index -> "{" + index + "}")
					.collect(joining(", "));
			result = result.replace(ARGUMENTS_PLACEHOLDER, replacement);
		}
		
		if (repeatableExceptionAppeared){
			result = result.concat(" [Retry " + currentRepetition + "] ");
		}
		
		return result;
	}

	private Object[] makeReadable(Object[] arguments) {
		// Note: humanReadableArguments must be an Object[] in order to
		// avoid varargs issues with non-Eclipse compilers.
		Object[] humanReadableArguments = //
			Arrays.stream(arguments).map(StringUtils::nullSafeToString).toArray(String[]::new);
		return humanReadableArguments;
	}

	private String formatSafely(String pattern, Object[] arguments) {
		try {
			return MessageFormat.format(pattern, arguments);
		}
		catch (IllegalArgumentException ex) {
			String message = "The display name pattern defined for the parameterized test is invalid. "
					+ "See nested exception for further details.";
			throw new JUnitException(message, ex);
		}
	}

}
