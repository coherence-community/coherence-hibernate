/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package org.hibernate.tutorial.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Source copied from http://docs.jboss.org/hibernate/orm/4.2/manual/en-US/html/ch01.html
 * and adapted for functional testing of coherence-hibernate-second-level-cache.
 */
public class Event {
    private Long id;

    private String title;

    private Date date;

    private Set<Person> participants = new HashSet<>();

    public Event() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
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

}