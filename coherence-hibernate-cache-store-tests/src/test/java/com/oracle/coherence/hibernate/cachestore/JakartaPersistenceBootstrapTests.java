/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import java.util.Date;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.tutorial.domain.Event;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Jakarta Persistence 3.2 bootstrap with Hibernate 7 transformed XML mappings.
 */
public class JakartaPersistenceBootstrapTests {

    @Test
    public void bootstrapsTransformedMappings() {
        try (EntityManagerFactory factory = Persistence.createEntityManagerFactory(
                "coherence-hibernate-cache-store-test")) {
            final Long id;
            try (EntityManager entityManager = factory.createEntityManager()) {
                entityManager.getTransaction().begin();
                final Event event = new Event();
                event.setDate(new Date());
                event.setTitle("Jakarta Persistence 3.2");
                entityManager.persist(event);
                entityManager.getTransaction().commit();
                id = event.getId();
            }

            try (EntityManager entityManager = factory.createEntityManager()) {
                final Event event = entityManager.find(Event.class, id);
                assertThat(event).isNotNull();
                assertThat(event.getTitle()).isEqualTo("Jakarta Persistence 3.2");
            }
        }
    }
}
