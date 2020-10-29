/*
 * File: EventManager.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
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
            List events = mgr.listEvents();
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

    public List listEvents() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query query = session.createQuery("from Event");
        query.setCacheable(true);
        List result = query.list();
        session.getTransaction().commit();
        return result;
    }

    public List listPersons() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query query = session.createQuery("from Person");
        query.setCacheable(true);
        List result = query.list();
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