/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service.impl;

import com.oracle.coherence.hibernate.demo.dao.EventRepository;
import com.oracle.coherence.hibernate.demo.dao.PersonRepository;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.model.Person;
import com.oracle.coherence.hibernate.demo.service.PersonService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PersonService}.
 * @author Gunnar Hillert
 */
@Transactional
@Service
public class DefaultPersonService implements PersonService {

	private final PersonRepository personRepository;

	private final EventRepository eventRepository;

	public DefaultPersonService(PersonRepository personRepository, EventRepository eventRepository) {
		this.personRepository = personRepository;
		this.eventRepository = eventRepository;
	}

	@Override
	public Page<Person> listPeople(Pageable pageable) {
		return this.personRepository.findAll(pageable);
	}

	@Override
	public Long createAndStorePerson(String firstName, String lastName, int age) {
		Assert.hasText(firstName, "FirstName must not be null or empty.");
		Assert.hasText(lastName, "LastName must not be null or empty.");
		Assert.isTrue(age > 0, "The age must be a positive number.");
		final Person person = new Person();
		person.setFirstname(firstName);
		person.setLastname(lastName);
		person.setAge(age);
		final Person savedPrson = this.personRepository.save(person);
		return savedPrson.getId();
	}

	@Override
	public void addPersonToEvent(Long personId, Long eventId) {
		Assert.notNull(personId, "PersonId must not be null.");
		Assert.notNull(eventId, "EventId must not be null.");

		final Person person = this.personRepository.findById(personId).orElseThrow(() ->
				new PersonNotFoundException(personId));
		final Event event = this.eventRepository.findById(eventId).orElseThrow(() ->
				new EventNotFoundException(eventId));
		event.addParticipant(person);
		this.eventRepository.save(event);
	}

	@Override
	public Person getPerson(Long personId) {
		Assert.notNull(personId, "PersonId must not be null.");
		return this.personRepository.findById(personId).orElseThrow(() ->
				new PersonNotFoundException(personId));
	}

}
