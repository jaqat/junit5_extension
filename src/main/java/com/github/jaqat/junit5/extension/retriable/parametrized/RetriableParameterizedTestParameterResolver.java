package com.github.jaqat.junit5.extension.retriable.parametrized;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

/**
 * @since 5.0
 */
class RetriableParameterizedTestParameterResolver implements ParameterResolver {

	private final RetriableParameterizedTestMethodContext methodContext;
	private final Object[] arguments;

	RetriableParameterizedTestParameterResolver(RetriableParameterizedTestMethodContext methodContext, Object[] arguments) {
		this.methodContext = methodContext;
		this.arguments = arguments;
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Executable declaringExecutable = parameterContext.getDeclaringExecutable();
		Method testMethod = extensionContext.getTestMethod().orElse(null);

		// Not a @RetriableParameterizedTest method?
		if (!declaringExecutable.equals(testMethod)) {
			return false;
		}

		// Current parameter is an aggregator?
		if (this.methodContext.isAggregator(parameterContext.getIndex())) {
			return true;
		}

		// Ensure that the current parameter is declared before aggregators.
		// Otherwise, a different ParameterResolver should handle it.
		if (this.methodContext.indexOfFirstAggregator() != -1) {
			return parameterContext.getIndex() < this.methodContext.indexOfFirstAggregator();
		}

		// Else fallback to behavior for parameterized test methods without aggregators.
		return parameterContext.getIndex() < this.arguments.length;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {

		return this.methodContext.resolve(parameterContext, this.arguments);
	}

}
