/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service;

import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.service.impl.EventNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * The event service is responsible for retrieving and persisting {@link Event}s.
 * @author Gunnar Hillert
 */
public interface EventService {

	/**
	 * Queries and returns a paginated list of events.
	 * @param pageable the pagination information
	 * @return a paginated list of Events
	 */
	Page<Event> listEvents(Pageable pageable);

	/**
	 * Creates and persists an {@link Event}.
	 * @param title the title of the Event
	 * @param date the date of the Event
	 * @return an Event. Never null.
	 */
	Event createAndStoreEvent(String title, LocalDate date);

	/**
	 * Returns an {@link Event}.
	 * @param id the of the id
	 * @param withParticipants if true, the event will be queried using a Jpa query
	 * @return an Event. Never null.
	 * @throws EventNotFoundException in case no Event was found
	 */
	Event getEvent(Long id, boolean usingJpaQuery, boolean withParticipants);

}
