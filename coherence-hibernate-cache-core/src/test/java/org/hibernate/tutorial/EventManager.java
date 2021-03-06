/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package org.hibernate.tutorial;

import org.hibernate.Query;
import org.hibernate.Session;

import java.util.*;

import org.hibernate.tutorial.domain.Event;
import org.hibernate.tutorial.domain.Person;
import org.hibernate.tutorial.util.HibernateUtil;

/**
 * Source copied from http://docs.jboss.org/hibernate/orm/4.2/manual/en-US/html/ch01.html.
 */
public class EventManager {

    public static void main(String[] args) {
        EventManager mgr = new EventManager();

        if (args[0].equals("store")) {
            mgr.createAndStoreEvent("My Event", new Date());
        }
        else if (args[0].equals("list")) {
            List<Event> events = mgr.listEvents();
            for (int i = 0; i < events.size(); i++) {
                Event theEvent = (Event) events.get(i);
                System.out.println(
                        "Event: " + theEvent.getTitle() + " Time: " + theEvent.getDate()
                );
            }
        }
        else if (args[0].equals("addpersontoevent")) {
                Long eventId = mgr.createAndStoreEvent("My Event", new Date());
                Long personId = mgr.createAndStorePerson("Foo", "Bar", 10);
                mgr.addPersonToEvent(personId, eventId);
                System.out.println("Added person " + personId + " to event " + eventId);
            }
        else if (args[0].equals("addemailtoperson")) {
                Long personId = mgr.createAndStorePerson("Foo", "Bar", 10);
                String emailAddress = "foobar@hibernate.org";
                mgr.addEmailToPerson(personId, emailAddress);
                System.out.println("Added email " + emailAddress + " to person " + personId);
            }
        HibernateUtil.getSessionFactory().close();
    }

    public Long createAndStoreEvent(String title, Date theDate) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Event theEvent = new Event();
        theEvent.setTitle(title);
        theEvent.setDate(theDate);
        session.save(theEvent);

        session.getTransaction().commit();

        return theEvent.getId();
    }

    @SuppressWarnings("unchecked")
	public List<Event> listEvents() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query query = session.createQuery("from Event");
        query.setCacheable(true);
        List<Event> result = query.list();
        session.getTransaction().commit();
        return result;
    }

    @SuppressWarnings("unchecked")
	public List<Person> listPersons() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query query = session.createQuery("from Person");
        query.setCacheable(true);
        List<Person> result = query.list();
        session.getTransaction().commit();
        return result;
    }

    public Long createAndStorePerson(String firstName, String lastName, int age) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person thePerson = new Person();
        thePerson.setFirstname(firstName);
        thePerson.setLastname(lastName);
        thePerson.setAge(age);
        session.save(thePerson);

        session.getTransaction().commit();

        return thePerson.getId();
    }

    public void addPersonToEvent(Long personId, Long eventId) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Query query = session
                .createQuery("select p from Person p left join fetch p.events where p.id = :pid")
                .setParameter("pid", personId);
        query.setCacheable(true);
        Person aPerson = (Person) query.uniqueResult(); // Eager fetch the collection so we can use it detached
        Event anEvent = (Event) session.load(Event.class, eventId);

        session.getTransaction().commit();

        // End of first unit of work

        aPerson.getEvents().add(anEvent); // aPerson (and its collection) is detached

        // Begin second unit of work

        Session session2 = HibernateUtil.getSessionFactory().getCurrentSession();
        session2.beginTransaction();
        session2.update(aPerson); // Reattachment of aPerson

        session2.getTransaction().commit();
    }

    public void addEmailToPerson(Long personId, String emailAddress) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person aPerson = (Person) session.load(Person.class, personId);
        // adding to the emailAddress collection might trigger a lazy load of the collection
        aPerson.getEmailAddresses().add(emailAddress);

        session.getTransaction().commit();
    }

}