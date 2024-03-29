/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.controller.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Data transfer object for {@link com.oracle.coherence.hibernate.demo.model.Event}s.
 * @author Gunnar Hillert
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class EventDto implements Serializable {

	private Long id;

	private String title;

	private LocalDate date;

	private Set<Long> participants = new HashSet<>();

	public EventDto() {
	}

	public EventDto(Long id, String title, LocalDate date) {
		this.id = id;
		this.title = title;
		this.date = date;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<Long> getParticipants() {
		return this.participants;
	}

	public void setParticipants(Set<Long> participantIds) {
		this.participants.addAll(participantIds);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final EventDto event = (EventDto) o;
		return Objects.equals(this.id, event.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}
}
