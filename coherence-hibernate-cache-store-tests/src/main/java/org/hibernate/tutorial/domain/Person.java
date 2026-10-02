/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package org.hibernate.tutorial.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Source copied from http://docs.jboss.org/hibernate/orm/4.2/manual/en-US/html/ch01.html
 * and adapted for functional testing of coherence-hibernate-second-level-cache.
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class Person implements Serializable {

    private Long id;
    private int age;
    private String firstname;
    private String lastname;

    private Set<Event> events = new HashSet<>(0);
    private Set<String> emailAddresses = new HashSet<>(0);

    public Person() {
    }

    // Accessor methods for all properties, private setter for 'id'

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

    public Set getEvents() {
        return this.events;
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
        return this.emailAddresses;
    }

    public void setEmailAddresses(Set<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Person person = (Person) o;

        if (this.age != person.age) {
            return false;
        }
        if (!this.firstname.equals(person.firstname)) {
            return false;
        }
        if (!this.id.equals(person.id)) {
            return false;
        }
        if (!this.lastname.equals(person.lastname)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.age;
        result = 31 * result + this.firstname.hashCode();
        result = 31 * result + this.lastname.hashCode();
        return result;
    }
}
