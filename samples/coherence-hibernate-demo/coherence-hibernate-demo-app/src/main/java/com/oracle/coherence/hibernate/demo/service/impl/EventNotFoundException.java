/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
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
    /**
     * Create the exception.
     *
     * @param message reason for the exception.
     */
    public EventNotFoundException(String message)
        {
        super(message);
        }
    }
