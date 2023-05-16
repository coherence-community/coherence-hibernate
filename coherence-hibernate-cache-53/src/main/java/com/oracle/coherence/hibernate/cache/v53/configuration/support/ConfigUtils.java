/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.configuration.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.tangosol.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for some common Configuration utilities.
 * @author Gunnar Hillert
 * @since 2.3
 */
public abstract class ConfigUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

    private static final String WITH_NAME_CLASS_NAME = "com.tangosol.net.options.WithName";
    private static final String WITH_NAME_CLASS_OF_METHOD_NAME = "of";

    private ConfigUtils() {
        throw new AssertionError();
    }

    /**
     * The {@link Session} name can only be specified using Coherence CE 21.12 and higher. Specifying a sessionNames for
     * older Coherence versions will result in an {@link IllegalArgumentException} to be thrown.
     * @param sessionName must not be null
     * @return the Session.Option
     */
    public static Session.Option getSessionNameOption(String sessionName) {
        Assert.notNull(sessionName, "The sessionName must not be null.");
        try {
            final Class<?> withNameClass = Class.forName(WITH_NAME_CLASS_NAME);
            final Method ofMethod = withNameClass.getMethod(WITH_NAME_CLASS_OF_METHOD_NAME, String.class);
            final Session.Option sessionNameOption = (Session.Option) ofMethod.invoke(null, sessionName);
            return sessionNameOption;
        }
		catch (ClassNotFoundException ex) {
            final String errorMessage = String.format("The required Class '%s' was not found. Most likely you are using a Coherence version" +
                    "older than '21.12' which does not support specifying a session name.", WITH_NAME_CLASS_NAME);
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(errorMessage);
            }
            throw new IllegalArgumentException(errorMessage, ex);
        }
        catch (NoSuchMethodException ex) {
            throw new IllegalStateException(String.format(
                    "Method '%s' of class '%s' was not found.", WITH_NAME_CLASS_OF_METHOD_NAME, WITH_NAME_CLASS_NAME), ex);
        }
        catch (InvocationTargetException ex) {
            throw new IllegalStateException(String.format(
                    "Executing the '%s' method of class '%s' threw an unexpected exception.", WITH_NAME_CLASS_OF_METHOD_NAME, WITH_NAME_CLASS_NAME), ex);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException(String.format(
                    "Method '%s' of class '%s' was not accessible.", WITH_NAME_CLASS_OF_METHOD_NAME, WITH_NAME_CLASS_NAME), ex);
        }
    }
}
