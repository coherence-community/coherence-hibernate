/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 *
 * @author Gunnar Hillert
 *
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="PEOPLE")
public class Person {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	private int age;
	private String firstname;
	private String lastname;

	@ManyToMany(targetEntity = Event.class)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Set<Event> events = new HashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "PERSON_EMAIL_ADDR", joinColumns = @JoinColumn(name = "PERSON_ID"))
	@Column(name = "EMAIL_ADDR")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Set<String> emailAddresses = new HashSet<>();

	public Person() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public Set<Event> getEvents() {
		return events;
	}

	protected void setEvents(Set<Event> events) {
		this.events = events;
	}

	public void addToEvent(Event event) {
		this.getEvents().add(event);
		event.getParticipants().add(this);
	}

	public void removeFromEvent(Event event) {
		this.getEvents().remove(event);
		event.getParticipants().remove(this);
	}

	public Set<String> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set<String> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

}
