/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service.impl;

import com.oracle.coherence.hibernate.demo.dao.EventRepository;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * @author Gunnar Hillert
 */
@Transactional
@Service
public class DefaultEventService implements EventService {

	@Autowired
	private EventRepository eventRepository;

	@Override
	public Page<Event> listEvents(Pageable pageable) {
		return this.eventRepository.findAll(pageable);
	}

	@Override
	public Long createAndStoreEvent(String title, LocalDate date) {
		final Event event = new Event();
		event.setTitle(title);
		event.setDate(date);

		final Event savedEvent = this.eventRepository.save(event);
		return savedEvent.getId();
	}

	@Override
	public Event getEvent(Long id, boolean withParticipants) {

		final Event event;
		Event eventToReturn = new Event();

		if (withParticipants) {
			event = this.eventRepository.getEventWithParticipants(id).orElseThrow(() ->
					new EventNotFoundException(id));
			eventToReturn.getParticipants().addAll(event.getParticipants());
			return event;
		}
		else {
			event = this.eventRepository.findById(id).orElseThrow(() ->
					new EventNotFoundException(id));
		}

		eventToReturn.setDate(event.getDate());
		eventToReturn.setTitle(event.getTitle());
		eventToReturn.setId(event.getId());

		return eventToReturn;
	}

}
