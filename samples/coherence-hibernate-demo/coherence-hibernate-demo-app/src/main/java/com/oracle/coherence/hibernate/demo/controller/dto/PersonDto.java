/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.controller.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Data transfer object for {@link com.oracle.coherence.hibernate.demo.model.Person}s.
 * @author Gunnar Hillert
 *
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = PersonDto.class
)
public class PersonDto implements Serializable {

	private Long id;
	private int age;
	private String firstname;
	private String lastname;

	@JsonIdentityReference
	@JsonIgnore
	private Set<EventDto> events = new HashSet<>();

	public PersonDto() {
	}

	public PersonDto(Long id) {
		this.id = id;
	}

	public PersonDto(Long id, String firstname, String lastname, int age) {
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
		this.age = age;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFirstname() {
		return this.firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return this.lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public Set<EventDto> getEvents() {
		return this.events;
	}

	protected void setEvents(Set<EventDto> events) {
		this.events = events;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PersonDto person = (PersonDto) o;
		return Objects.equals(this.id, person.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}
}
