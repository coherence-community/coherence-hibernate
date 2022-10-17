/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service;

import com.oracle.coherence.hibernate.demo.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * @author Gunnar Hillert
 */
public interface EventService {

	/**
	 * @return
	 */
	Page<Event> listEvents(Pageable pageable);

	/**
	 * @param title
	 * @param date
	 * @return
	 */
	Long createAndStoreEvent(String title, LocalDate date);

	Event getEvent(Long id, boolean withParticipants);

}
