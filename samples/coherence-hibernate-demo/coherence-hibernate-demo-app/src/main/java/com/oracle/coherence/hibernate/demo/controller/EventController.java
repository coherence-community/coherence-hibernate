/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.controller;

import com.oracle.coherence.hibernate.demo.controller.dto.EventDto;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Explicit controller for retrieving Events.
 *
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping(path = "/api/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	public Page<EventDto> getEvents(Pageable pageable) {
		return eventService.listEvents(pageable).map(event ->
				new EventDto(event.getId(), event.getTitle(), event.getDate()));
	}

	@PostMapping
	public Long createEvent(
			@RequestParam("title") String title,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return eventService.createAndStoreEvent(title, date).getId();
	}

	@GetMapping("/{id}")
	public EventDto getEvent(@PathVariable Long id,
			@RequestParam(required = false, defaultValue = "false") boolean usingJpaQuery,
			@RequestParam(required = false, defaultValue = "false") boolean withParticipants) {
		Event event = eventService.getEvent(id, usingJpaQuery, withParticipants);
		EventDto eventDto = new EventDto(event.getId(), event.getTitle(), event.getDate());

		if (withParticipants) {
			eventDto.setParticipants(event.getParticipants().stream().map(e -> e.getId()).collect(Collectors.toSet()));
		}

		return eventDto;
	}
}
