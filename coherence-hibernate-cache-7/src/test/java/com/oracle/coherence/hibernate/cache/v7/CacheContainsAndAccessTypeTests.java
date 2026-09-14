/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import com.oracle.coherence.hibernate.cache.v7.access.CoherenceNonstrictReadWriteNaturalIdAccess;
import com.oracle.coherence.hibernate.cache.v7.support.Book;
import com.oracle.coherence.hibernate.cache.v7.support.Foo;
import com.tangosol.net.CacheFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Verifies cache containment and access-type contracts exposed by Hibernate's cache SPI.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
        Foo.class,
        Book.class,
        CacheContainsAndAccessTypeTests.ReadOnlyEntity.class,
        CacheContainsAndAccessTypeTests.NonstrictEntity.class
})
public class CacheContainsAndAccessTypeTests {

    @AfterEach
    public void cleanup(SessionFactoryScope scope) {
        scope.dropData();
        scope.getSessionFactory().getCache().evictAllRegions();
    }

    @AfterAll
    public static void shutdownCoherence() {
        CacheFactory.shutdown();
    }

    @Test
    public void readWriteEntityContainmentTracksPopulationAndEviction(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final Foo entity = new Foo("read-write");
            session.persist(entity);
            return entity.getId();
        });

        assertEntityContainmentLifecycle(scope, Foo.class, id);
    }

    @Test
    public void readOnlyEntityContainmentTracksPopulationAndEviction(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final ReadOnlyEntity entity = new ReadOnlyEntity("read-only");
            session.persist(entity);
            return entity.id;
        });

        assertEntityContainmentLifecycle(scope, ReadOnlyEntity.class, id);
    }

    @Test
    public void nonstrictEntityContainmentTracksPopulationAndEviction(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final NonstrictEntity entity = new NonstrictEntity("nonstrict");
            session.persist(entity);
            return entity.id;
        });

        assertEntityContainmentLifecycle(scope, NonstrictEntity.class, id);
    }

    @Test
    public void readWriteNaturalIdContainmentTracksPopulationAndEviction(SessionFactoryScope scope) {
        final Book book = scope.fromTransaction((session) -> {
            final Book entity = new Book("Dune", "Frank Herbert", "0441172717");
            session.persist(entity);
            return entity;
        });
        final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
        final EntityPersister persister = sessionFactory.getMappingMetamodel().getEntityDescriptor(Book.class);
        final NaturalIdDataAccess access = persister.getNaturalIdCacheAccessStrategy();
        final Object naturalIdValues = persister.getNaturalIdMapping().extractNaturalIdFromEntity(book);
        final Object cacheKey = scope.fromSession(
                (session) -> access.generateCacheKey(naturalIdValues, persister, session));

        assertThat(access.contains(cacheKey)).isTrue();
        sessionFactory.getCache().evictNaturalIdData(Book.class);
        assertThat(access.contains(cacheKey)).isFalse();
    }

    @Test
    public void nonstrictNaturalIdReportsItsConfiguredAccessType() {
        final CoherenceNonstrictReadWriteNaturalIdAccess access =
                new CoherenceNonstrictReadWriteNaturalIdAccess(
                        mock(DomainDataRegion.class), mock(DomainDataStorageAccess.class));

        assertThat(access.getAccessType()).isEqualTo(AccessType.NONSTRICT_READ_WRITE);
    }

    @Test
    public void readOnlyEntityAccessRejectsSpiUpdates(SessionFactoryScope scope) {
        final EntityDataAccess access = scope.getSessionFactory().getMappingMetamodel()
                .getEntityDescriptor(ReadOnlyEntity.class)
                .getCacheAccessStrategy();

        assertThatThrownBy(() -> access.update(null, "key", "value", null, null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Write operations are not supported in the read-only cache concurrency strategy.");
        assertThatThrownBy(() -> access.afterUpdate(null, "key", "value", null, null, null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Write operations are not supported in the read-only cache concurrency strategy.");
    }

    private void assertEntityContainmentLifecycle(SessionFactoryScope scope, Class<?> entityType, Long id) {
        final org.hibernate.Cache hibernateCache = scope.getSessionFactory().getCache();
        final jakarta.persistence.Cache jpaCache = scope.getSessionFactory().getCache();
        hibernateCache.evictEntityData(entityType, id);

        assertThat(hibernateCache.containsEntity(entityType, id)).isFalse();
        assertThat(jpaCache.contains(entityType, id)).isFalse();

        scope.inTransaction((session) -> assertThat(session.find(entityType, id)).isNotNull());

        assertThat(hibernateCache.containsEntity(entityType, id)).isTrue();
        assertThat(jpaCache.contains(entityType, id)).isTrue();

        hibernateCache.evictEntityData(entityType, id);
        assertThat(hibernateCache.containsEntity(entityType, id)).isFalse();
        assertThat(jpaCache.contains(entityType, id)).isFalse();
    }

    @Entity(name = "ContainsReadOnlyEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "contains.read-only")
    public static class ReadOnlyEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;

        public ReadOnlyEntity() {
        }

        public ReadOnlyEntity(String name) {
            this.name = name;
        }
    }

    @Entity(name = "ContainsNonstrictEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "contains.nonstrict")
    public static class NonstrictEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;

        public NonstrictEntity() {
        }

        public NonstrictEntity(String name) {
            this.name = name;
        }
    }
}
