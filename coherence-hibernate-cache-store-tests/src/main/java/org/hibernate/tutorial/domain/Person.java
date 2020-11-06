/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
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
 */
public class Person implements Serializable
{

    private Long id;
    private int age;
    private String firstname;
    private String lastname;

    private Set events = new HashSet();
    private Set emailAddresses = new HashSet();

    public Person() {}

    // Accessor methods for all properties, private setter for 'id'

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public int getAge()
    {
        return age;
    }

    public void setAge(int age)
    {
        this.age = age;
    }

    public String getFirstname()
    {
        return firstname;
    }

    public void setFirstname(String firstname)
    {
        this.firstname = firstname;
    }

    public String getLastname()
    {
        return lastname;
    }

    public void setLastname(String lastname)
    {
        this.lastname = lastname;
    }

    public Set getEvents() {
        return events;
    }

    protected void setEvents(Set events) {
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

    public Set getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(Set emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (age != person.age) return false;
        if (!firstname.equals(person.firstname)) return false;
        if (!id.equals(person.id)) return false;
        if (!lastname.equals(person.lastname)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id.hashCode();
        result = 31 * result + age;
        result = 31 * result + firstname.hashCode();
        result = 31 * result + lastname.hashCode();
        return result;
    }
}
