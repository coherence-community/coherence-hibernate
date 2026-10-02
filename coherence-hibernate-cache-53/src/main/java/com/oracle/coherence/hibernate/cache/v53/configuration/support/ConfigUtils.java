/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.configuration.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for some common Configuration utilities.
 * @author Gunnar Hillert
 * @since 2.3
 * //TODO
 */
public abstract class ConfigUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

    private static final String WITH_NAME_CLASS_NAME = "com.tangosol.net.options.WithName";
    private static final String WITH_NAME_CLASS_OF_METHOD_NAME = "of";

    private ConfigUtils() {
        throw new AssertionError();
    }
}
