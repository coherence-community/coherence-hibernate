/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Gunnar Hillert
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "EVENTS")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@NaturalId
	private String title;

	@NaturalId
	private LocalDate date;

	@ManyToMany(targetEntity = Person.class)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@JsonIdentityReference(alwaysAsId = true)
	private Set<Person> participants = new HashSet<>();

	public Event() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<Person> getParticipants() {
		return participants;
	}

	protected void setParticipants(Set<Person> participants) {
		this.participants = participants;
	}

	public void addParticipant(Person person) {
		this.getParticipants().add(person);
		person.getEvents().add(this);
	}

	public void removeParticipant(Person person) {
		this.getParticipants().remove(person);
		person.getEvents().remove(this);
	}

	@JsonProperty("participants")
	public void setParticipantIds(Set<Long> participantIds) {
		this.participants.addAll(participantIds.stream().map(id -> {
			return new Person(id);
		}).collect(Collectors.toList()));
	}
}
