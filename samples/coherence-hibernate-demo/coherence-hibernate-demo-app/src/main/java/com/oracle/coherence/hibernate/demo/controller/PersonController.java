/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.controller;

import com.oracle.coherence.hibernate.demo.controller.dto.PersonDto;
import com.oracle.coherence.hibernate.demo.model.Person;
import com.oracle.coherence.hibernate.demo.service.PersonService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explicit controller for retrieving People.
 *
 * @author Gunnar Hillert
 *
 */
@RestController
@RequestMapping(path = "/api/people")
@Transactional
public class PersonController {

	private final PersonService personService;

	public PersonController(PersonService personService) {
		this.personService = personService;
	}

	@GetMapping
	public Page<PersonDto> getPeople(Pageable pageable) {
		return this.personService.listPeople(pageable).map((person) ->
			new PersonDto(person.getId(), person.getFirstname(), person.getLastname(), person.getAge()));
	}

	@PostMapping
	public Long createPerson(
		@RequestParam("firstName") String firstName,
		@RequestParam("lastName") String lastName,
		@RequestParam("age") int age) {
		return this.personService.createAndStorePerson(firstName, lastName, age);
	}

	@GetMapping("/{personId}")
	public PersonDto getSinglePerson(@PathVariable("personId") Long personId) {
		final Person person = this.personService.getPerson(personId);
		return new PersonDto(person.getId(), person.getFirstname(), person.getLastname(), person.getAge());
	}

	@PostMapping("/{personId}/add-to-event/{eventId}")
	public void addPersonToEvent(
		@PathVariable("personId") Long personId,
		@PathVariable("eventId") Long eventId) {
		this.personService.addPersonToEvent(personId, eventId);
	}
}
