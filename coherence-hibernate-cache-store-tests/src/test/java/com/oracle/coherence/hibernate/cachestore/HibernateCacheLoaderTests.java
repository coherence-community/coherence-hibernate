/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tutorial.domain.Event;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A JUnit5-based suite of functional tests that directly operates on the {@link HibernateCacheLoader}.
 * @author Gunnar Hillert
 */
@ServiceRegistry(
	settings = {
		@Setting(name = "hibernate.connection.driver_class", value = "org.hsqldb.jdbcDriver"),
		@Setting(name = "hibernate.connection.url", value = "jdbc:hsqldb:mem:testdb"),
		@Setting(name = "hibernate.connection.username", value = "sa"),
		@Setting(name = "hibernate.connection.password", value = ""),
		@Setting(name = "hibernate.show_sql", value = "true")
	}
)
@org.hibernate.testing.orm.junit.SessionFactory(
		generateStatistics = true
)
@DomainModel(
		xmlMappings = {
				"org/hibernate/tutorial/domain/Event.mapping.xml",
				"org/hibernate/tutorial/domain/Person.mapping.xml"
		}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HibernateCacheLoaderTests {

	@Test
	@Order(1)
	public void loadAllEntities(SessionFactoryScope scope) {

		final List<Long> eventIds = new ArrayList<>();

		scope.inTransaction((session) -> {
			final Event event = new Event();
			event.setDate(new Date());
			event.setTitle("Event_1");
            session.persist(event);
			eventIds.add(event.getId());
		});

		scope.inTransaction((session) -> {
			final Event event = new Event();
			event.setDate(new Date());
			event.setTitle("Event_2");
			session.persist(event);
			eventIds.add(event.getId());
		});

		final HibernateCacheLoader hibernateCacheLoader = new HibernateCacheLoader(
				"org.hibernate.tutorial.domain.Event", scope.getSessionFactory());

        final long missingEventId = eventIds.get(1) + 100L;
        final List<Long> listOfEventIds = List.of(eventIds.get(1), missingEventId, eventIds.get(0));
        final Map events = hibernateCacheLoader.loadAll(listOfEventIds);

        assertThat(events).size().isEqualTo(2);
        assertThat(events).containsKey(eventIds.get(0));
        assertThat(events).containsKey(eventIds.get(1));
        assertThat(events).doesNotContainKey(missingEventId);

        final Iterator<?> loadedIds = events.keySet().iterator();
        assertThat(loadedIds.next()).isEqualTo(eventIds.get(1));
        assertThat(loadedIds.next()).isEqualTo(eventIds.get(0));

		for (Object entry : events.values()) {
			assertThat(entry).isInstanceOf(Event.class);
		}
	}
}
