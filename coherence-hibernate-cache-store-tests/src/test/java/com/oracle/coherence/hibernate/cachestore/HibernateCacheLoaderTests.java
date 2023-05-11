/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tutorial.domain.Event;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A JUnit5-based suite of funtional tests that directly operates on the {@link HibernateCacheLoader}.
 * @author Gunnar Hillert
 */
@ServiceRegistry(
	settings={
		@Setting(name="hibernate.connection.driver_class", value="org.hsqldb.jdbcDriver"),
		@Setting(name="hibernate.connection.url", value="jdbc:hsqldb:mem:testdb"),
		@Setting(name="hibernate.connection.username", value="sa"),
		@Setting(name="hibernate.connection.password", value = ""),
		@Setting(name="hibernate.show_sql", value="true")
	}
)
@org.hibernate.testing.orm.junit.SessionFactory(
		generateStatistics=true
)
@DomainModel(
		xmlMappings= {
				"org/hibernate/tutorial/domain/Event.hbm.xml",
				"org/hibernate/tutorial/domain/Person.hbm.xml"
		}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HibernateCacheLoaderTests {

	@Test
	@Order(1)
	public void loadAllEntities(SessionFactoryScope scope) {

		final List<Long> eventIds = new ArrayList<>();

		scope.inTransaction( (session) -> {
			final Event event = new Event();
			event.setDate(new Date());
			event.setTitle("Event_1");
            session.persist(event);
			eventIds.add(event.getId());
		} );

		scope.inTransaction( (session) -> {
			final Event event = new Event();
			event.setDate(new Date());
			event.setTitle("Event_2");
			session.persist(event);
			eventIds.add(event.getId());
		} );

		final HibernateCacheLoader hibernateCacheLoader = new HibernateCacheLoader(
				"org.hibernate.tutorial.domain.Event", scope.getSessionFactory());

		final List listOfEventIds = new ArrayList();
		listOfEventIds.add(1L);
		listOfEventIds.add(2L);
		final Map events = hibernateCacheLoader.loadAll(listOfEventIds);

		assertThat(events).size().isEqualTo(2);
		assertThat(events).containsKey(1L);
		assertThat(events).containsKey(2L);

		for (Object entry : events.values()) {
			assertThat(entry).isInstanceOf(Event.class);
		}
	}
}
