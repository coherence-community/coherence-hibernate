/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service.impl;

/**
 * An exception indicating that a {@link com.oracle.coherence.hibernate.demo.model.Event} was not found.
 * @author Gunnar Hillert
 */
public class EventNotFoundException extends RuntimeException
    {
    private static final String EVENT_NOT_FOUND_MESSAGE = "Unable to find event with id '%s'.";

    /**
     * Create the exception.
     *
     * @param message reason for the exception.
     */
    public EventNotFoundException(String message)
        {
        super(message);
        }

    /**
     * Create the exception.
     *
     * @param eventId id of the {@link com.oracle.coherence.hibernate.demo.model.Event} that could not be found.
     */
    public EventNotFoundException(Long eventId)
        {
        this(String.format(EVENT_NOT_FOUND_MESSAGE, eventId));
        }
}