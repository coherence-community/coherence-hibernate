/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package org.hibernate.tutorial;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.tutorial.domain.Event;
import org.hibernate.tutorial.domain.Person;
import org.hibernate.tutorial.util.HibernateUtil;

/**
 * Source copied from http://docs.jboss.org/hibernate/orm/4.2/manual/en-US/html/ch01.html.
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class EventManager {

    public static void main(String[] args) {
        final EventManager mgr = new EventManager();

        if (args[0].equals("store")) {
            mgr.createAndStoreEvent("My Event", new Date());
        }
        else if (args[0].equals("list")) {
            final List<Event> events = mgr.listEvents();
            for (int i = 0; i < events.size(); i++) {
                final Event theEvent = events.get(i);
                System.out.println(
                        "Event: " + theEvent.getTitle() + " Time: " + theEvent.getDate()
                );
            }
        }
        else if (args[0].equals("addpersontoevent")) {
                final Long eventId = mgr.createAndStoreEvent("My Event", new Date());
                final Long personId = mgr.createAndStorePerson("Foo", "Bar", 10);
                mgr.addPersonToEvent(personId, eventId);
                System.out.println("Added person " + personId + " to event " + eventId);
            }
        else if (args[0].equals("addemailtoperson")) {
                final Long personId = mgr.createAndStorePerson("Foo", "Bar", 10);
                final String emailAddress = "foobar@hibernate.org";
                mgr.addEmailToPerson(personId, emailAddress);
                System.out.println("Added email " + emailAddress + " to person " + personId);
            }
        HibernateUtil.getSessionFactory().close();
    }

    public Long createAndStoreEvent(String title, Date theDate) {
        final Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        final Event theEvent = new Event();
        theEvent.setTitle(title);
        theEvent.setDate(theDate);
        session.save(theEvent);

        session.getTransaction().commit();

        return theEvent.getId();
    }

    @SuppressWarnings("unchecked")
	public List<Event> listEvents() {
        final Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        final Query query = session.createQuery("from Event");
        query.setCacheable(true);
        final List<Event> result = query.list();
        session.getTransaction().commit();
        return result;
    }

    @SuppressWarnings("unchecked")
	public List<Person> listPersons() {
        final Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        final Query query = session.createQuery("from Person");
        query.setCacheable(true);
        final List<Person> result = query.list();
        session.getTransaction().commit();
        return result;
    }

    public Long createAndStorePerson(String firstName, String lastName, int age) {
        final Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        final Person thePerson = new Person();
        thePerson.setFirstname(firstName);
        thePerson.setLastname(lastName);
        thePerson.setAge(age);
        session.save(thePerson);

        session.getTransaction().commit();

        return thePerson.getId();
    }

    public void addPersonToEvent(Long personId, Long eventId) {
        final Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        final Query query = session
                .createQuery("select p from Person p left join fetch p.events where p.id = :pid")
                .setParameter("pid", personId);
        query.setCacheable(true);
        final Person aPerson = (Person) query.uniqueResult(); // Eager fetch the collection so we can use it detached
        final Event anEvent = session.load(Event.class, eventId);

        session.getTransaction().commit();

        // End of first unit of work

        aPerson.getEvents().add(anEvent); // aPerson (and its collection) is detached

        // Begin second unit of work

        final Session session2 = HibernateUtil.getSessionFactory().getCurrentSession();
        session2.beginTransaction();
        session2.update(aPerson); // Reattachment of aPerson

        session2.getTransaction().commit();
    }

    public void addEmailToPerson(Long personId, String emailAddress) {
        final Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        final Person aPerson = session.load(Person.class, personId);
        // adding to the emailAddress collection might trigger a lazy load of the collection
        aPerson.getEmailAddresses().add(emailAddress);

        session.getTransaction().commit();
    }

}
