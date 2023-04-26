/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.configuration.support;

/**
 * Helper class for some common String utilities.
 * @author Gunnar Hillert
 * @since 2.1
 */
public abstract class StringUtils {

	private StringUtils() {
		throw new AssertionError();
	}

	public static boolean hasText(String string) {
		return (string != null && !string.isEmpty() && containsText(string));
	}

	private static boolean containsText(CharSequence string) {
		int stringLength = string.length();
		for (int i = 0; i < stringLength; i++) {
			if (!Character.isWhitespace(string.charAt(i))) {
				return true;
			}
		}
		return false;
	}
}
