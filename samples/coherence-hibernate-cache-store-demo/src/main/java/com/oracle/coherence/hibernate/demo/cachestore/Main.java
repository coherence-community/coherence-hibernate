/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.cachestore;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.SessionConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hsqldb.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {

		final Server hsqldbServer = new Server();
		hsqldbServer.setAddress("localhost");
		hsqldbServer.setPort(9001);
		hsqldbServer.setSilent(true);
		hsqldbServer.setDatabaseName(0, "mainDb");
		hsqldbServer.setDatabasePath(0, "mem:mainDb");
		hsqldbServer.start();

		final SessionConfiguration sessionConfig = SessionConfiguration.builder()
				.withConfigUri("my-coherence-cache-config.xml") // Specify your custom cache configuration
				.build();

		final CoherenceConfiguration coherenceConfiguration = CoherenceConfiguration.builder()
				.withSession(sessionConfig)
				.build();

		try (Coherence coherence = Coherence.clusterMember(coherenceConfiguration).start().join()) {
			final NamedMap<String, Book> books = coherence.getSession().getMap("books");
			books.put("9781847196125", new Book("9781847196125", "Oracle Coherence 3.5"));

			final SessionFactory sessionFactory = new Configuration()
					.configure("hibernate.cfg.xml")
					.buildSessionFactory();

			sessionFactory.inSession((session) -> {
				final Book coherenceBook = session.get(Book.class, "9781847196125");
				LOGGER.info("Book: {}", coherenceBook);
			});

			// Let's persist another book to the database
			sessionFactory.inTransaction((session) ->
					session.persist(new Book("9781932394153", "Hibernate in Action")));

			// Let's query the database for all books

			sessionFactory.inSession((session) ->
					session.createQuery("from Book", Book.class)
						.getResultList().forEach((book) -> LOGGER.info("Book: {}", book)));

			// At this point the size of the Coherence Map should only be 1

			LOGGER.info("Size of Coherence Map: {}", books.size());

			// Let's get the added book directly from the Coherence cache
			final Book hibernateBook = books.get("9781932394153");
			LOGGER.info("Book: {}", hibernateBook);

			// At this point the size of the Coherence Map should be 2
			LOGGER.info("Size of Coherence Map: {}", books.size());
		}

		hsqldbServer.stop();
	}
}
