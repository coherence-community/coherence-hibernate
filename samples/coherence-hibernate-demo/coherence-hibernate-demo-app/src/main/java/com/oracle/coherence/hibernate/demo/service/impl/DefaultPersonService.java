/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.service.impl;

import com.oracle.coherence.hibernate.demo.dao.EventRepository;
import com.oracle.coherence.hibernate.demo.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oracle.coherence.hibernate.demo.dao.PersonRepository;
import com.oracle.coherence.hibernate.demo.model.Person;
import com.oracle.coherence.hibernate.demo.service.PersonService;

@Transactional
@Service
public class DefaultPersonService implements PersonService {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private EventRepository eventRepository;

	@Override
	public Page<Person> listPeople(Pageable pageable) {
		return personRepository.findAll(pageable);
	}

	@Override
	public Long createAndStorePerson(String firstName, String lastName, int age) {
		final Person person = new Person();
		person.setFirstname(firstName);
		person.setLastname(lastName);
		person.setAge(age);
		final Person savedPrson = this.personRepository.save(person);
		return savedPrson.getId();
	}

	@Override
	public void addPersonToEvent(Long personId, Long eventId) {
		Person person = this.personRepository.findById(personId).orElseThrow(() ->
				new PersonNotFoundException(personId));
		Event event = this.eventRepository.findById(eventId).orElseThrow(() ->
				new EventNotFoundException(eventId));
		person.addToEvent(event);
		event.addParticipant(person);
		this.eventRepository.save(event);
	}

}
