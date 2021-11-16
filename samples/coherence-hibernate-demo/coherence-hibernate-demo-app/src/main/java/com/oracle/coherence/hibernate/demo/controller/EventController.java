/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//import com.hillert.s1.plants.controller.problems.PlantNotFoundProblem;
//import com.hillert.s1.plants.dto.PlantDto;
//import com.hillert.s1.plants.dto.PlantDtoDistanceComparator;
//import com.hillert.s1.plants.model.Image;
//import com.hillert.s1.plants.model.Plant;
//import com.hillert.s1.plants.service.PlantService;
//import com.hillert.s1.plants.support.DistanceUtils;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.service.EventService;

/**
 * Explicit controller for retrieving Events.
 *
 * @author Gunnar Hillert
 *
 */
@RestController
@RequestMapping(path="/api/events")
public class EventController {

	@Autowired
	private EventService eventService;

	@GetMapping
	public Page<Event> getEvents(Pageable pageable) {
		return eventService.listEvents(pageable);
	}

	@PostMapping
	public Long createEvent(
		@RequestParam("title") String title,
		@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
		return eventService.createAndStoreEvent(title, date);
	}

	@GetMapping("/{id}")
	public Event getEvent(@PathVariable Long id) {
		return eventService.getEvent(id);
	}
}
