/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service.impl;

/**
 * An exception indicating that a {@link com.oracle.coherence.hibernate.demo.model.Person} was not found.
 * @author Gunnar Hillert
 */
public class PersonNotFoundException extends RuntimeException {

    private static final String EVENT_NOT_FOUND_MESSAGE = "Unable to find event with id '%s'.";

    /**
     * Create the exception.
     *
     * @param message reason for the exception.
     */
    public PersonNotFoundException(String message) {
        super(message);
    }

    /**
     * Create the exception.
     *
     * @param personId id of the {@link com.oracle.coherence.hibernate.demo.model.Person} that could not be found.
     */
    public PersonNotFoundException(Long personId) {
        this(String.format(EVENT_NOT_FOUND_MESSAGE, personId));
    }
}
