/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service;

import com.oracle.coherence.hibernate.demo.model.Person;
import com.oracle.coherence.hibernate.demo.service.impl.PersonNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The person service is responsible for retrieving and persisting {@link Person}s.
 * @author Gunnar Hillert
 *
 */
public interface PersonService {

    /**
     * Queries and returns a paginated list of participants.
     * @param pageable the pagination information.
     * @return a paginated list of people.
     */
    Page<Person> listPeople(Pageable pageable);

    /**
     * Persist a {@link Person} to the database.
     * @param firstName must not be null or empty.
     * @param lastName must not be null or empty.
     * @param age must be a positive number.
     * @return the id of the created Person
     */
    Long createAndStorePerson(String firstName, String lastName, int age);

    /**
     * Add a {@link Person} to the {@link com.oracle.coherence.hibernate.demo.model.Event}.
     * @param personId must not be null.
     * @param eventId must not be null.
     */
    void addPersonToEvent(Long personId, Long eventId);

    /**
     * Find and return a Person for the given person id.
     * @param personId the person id for which to retrieve the Person for. Must not be null.
     * @return the Person. Never null.
     * @throws PersonNotFoundException if a person was not found
     */
    Person getPerson(Long personId);
}
