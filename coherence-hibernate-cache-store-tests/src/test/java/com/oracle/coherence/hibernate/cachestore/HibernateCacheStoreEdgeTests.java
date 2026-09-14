/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tutorial.domain.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Direct in-process coverage for cache-store paths not isolated by the remote functional suite.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.connection.driver_class", value = "org.hsqldb.jdbcDriver"),
        @Setting(name = "hibernate.connection.url", value = "jdbc:hsqldb:mem:cache-store-edge-tests"),
        @Setting(name = "hibernate.connection.username", value = "sa"),
        @Setting(name = "hibernate.connection.password", value = "")
})
@org.hibernate.testing.orm.junit.SessionFactory
@DomainModel(xmlMappings = {
        "org/hibernate/tutorial/domain/Event.mapping.xml",
        "org/hibernate/tutorial/domain/Person.mapping.xml"
})
public class HibernateCacheStoreEdgeTests {

    @AfterEach
    public void cleanup(SessionFactoryScope scope) {
        scope.dropData();
    }

    @Test
    public void loadReturnsPresentEntityAndNullForMissingKey(SessionFactoryScope scope) {
        final Person person = person(1L, "Ada");
        scope.inTransaction((session) -> session.persist(person));
        final HibernateCacheLoader loader = new HibernateCacheLoader(Person.class.getName(), scope.getSessionFactory());

        final Person loaded = (Person) loader.load(1L);
        assertThat(loaded.getId()).isEqualTo(1L);
        assertThat(loaded.getFirstname()).isEqualTo("Ada");
        assertThat(loaded.getLastname()).isEqualTo("Lovelace");
        assertThat(loaded.getAge()).isEqualTo(36);
        assertThat(loader.load(999L)).isNull();
    }

    @Test
    public void customQueryLoadAllReturnsOnlyMatchingEntities(SessionFactoryScope scope) {
        scope.inTransaction((session) -> {
            session.persist(person(1L, "Ada"));
            session.persist(person(2L, "Grace"));
        });
        final CustomQueryLoader loader = new CustomQueryLoader(scope);

        final Map results = loader.loadAll(List.of(2L, 999L, 1L));

        assertThat(results).containsOnlyKeys(1L, 2L);
        assertThat(results.get(1L)).isInstanceOf(Person.class);
        assertThat(results.get(2L)).isInstanceOf(Person.class);
    }

    @Test
    public void storeRejectsConflictingExplicitAndEntityIdentifiers(SessionFactoryScope scope) {
        final HibernateCacheStore store = new HibernateCacheStore(Person.class.getName(), scope.getSessionFactory());

        assertThatThrownBy(() -> store.store(2L, person(1L, "Ada")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conflicting identifier information");
        assertThat(countPeople(scope)).isZero();
    }

    @Test
    public void storeAllRollsBackEarlierMergesAndPropagatesFailure(SessionFactoryScope scope) {
        final HibernateCacheStore store = new HibernateCacheStore(Person.class.getName(), scope.getSessionFactory());
        final Map<Long, Object> entries = new LinkedHashMap<>();
        entries.put(1L, person(1L, "Ada"));
        entries.put(2L, new Object());

        assertThatThrownBy(() -> store.storeAll(entries)).isInstanceOf(RuntimeException.class);
        assertThat(countPeople(scope)).isZero();
    }

    @Test
    public void eraseAndEraseAllHandleMissingAndRepeatedKeys(SessionFactoryScope scope) {
        scope.inTransaction((session) -> {
            session.persist(person(1L, "Ada"));
            session.persist(person(2L, "Grace"));
            session.persist(person(3L, "Katherine"));
        });
        final HibernateCacheStore store = new HibernateCacheStore(Person.class.getName(), scope.getSessionFactory());

        store.erase(1L);
        assertThat(store.load(1L)).isNull();
        assertThat(countPeople(scope)).isEqualTo(2L);
        store.erase(1L);
        store.erase(999L);
        store.eraseAll(List.of(2L, 999L, 2L, 3L));
        assertThat(countPeople(scope)).isZero();
        store.eraseAll(List.of(2L, 3L));
        store.eraseAll(List.of());
    }

    @Test
    public void eraseAllRollsBackEarlierDeletesOnInvalidKey(SessionFactoryScope scope) {
        scope.inTransaction((session) -> session.persist(person(1L, "Ada")));
        final HibernateCacheStore store = new HibernateCacheStore(Person.class.getName(), scope.getSessionFactory());

        assertThatThrownBy(() -> store.eraseAll(List.of(1L, new Object())))
                .isInstanceOf(RuntimeException.class);
        assertThat(countPeople(scope)).isEqualTo(1L);
        assertThat(((Person) store.load(1L)).getFirstname()).isEqualTo("Ada");
    }

    private long countPeople(SessionFactoryScope scope) {
        return scope.fromTransaction((session) -> session.createSelectionQuery(
                "select count(*) from " + Person.class.getName(), Long.class).getSingleResult());
    }

    private Person person(Long id, String firstname) {
        final Person person = new Person();
        person.setId(id);
        person.setFirstname(firstname);
        person.setLastname("Lovelace");
        person.setAge(36);
        return person;
    }

    private static class CustomQueryLoader extends HibernateCacheLoader {
        CustomQueryLoader(SessionFactoryScope scope) {
            super(Person.class.getName(), scope.getSessionFactory());
            setLoadAllQuery("from " + Person.class.getName() + " where id in (:ids)");
        }
    }
}
