/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service.impl;

import java.time.LocalDate;

import com.oracle.coherence.hibernate.demo.dao.EventRepository;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.service.EventService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link EventService}.
 * @author Gunnar Hillert
 */
@Transactional
@Service
public class DefaultEventService implements EventService {

	private final EventRepository eventRepository;

	public DefaultEventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Override
	public Page<Event> listEvents(Pageable pageable) {
		return this.eventRepository.findAll(pageable);
	}

	@Override
	public Event createAndStoreEvent(String title, LocalDate date) {
		final Event event = new Event();
		event.setTitle(title);
		event.setDate(date);
		return this.eventRepository.save(event);
	}

	@Override
	public Event getEvent(Long id, boolean usingJpaQuery, boolean withParticipants) {

		final Event event;

		if (usingJpaQuery) {
			event = this.eventRepository.getEventWithParticipants(id).orElseThrow(() ->
					new EventNotFoundException(id));
			return event;
		}
		else {
			event = this.eventRepository.findById(id).orElseThrow(() ->
					new EventNotFoundException(id));
			if (withParticipants) {
				System.out.println(event.getParticipants().size());
			}
		}
		return event;
	}

}
