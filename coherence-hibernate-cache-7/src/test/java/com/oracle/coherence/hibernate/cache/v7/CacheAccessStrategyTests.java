/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.ArrayList;
import java.util.List;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for cache access strategies and collection regions.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
        CacheAccessStrategyTests.CachedParent.class,
        CacheAccessStrategyTests.CachedChild.class,
        CacheAccessStrategyTests.NonstrictEntity.class,
        CacheAccessStrategyTests.ReadOnlyEntity.class
})
public class CacheAccessStrategyTests {

    @AfterAll
    public static void afterAll() {
        CacheFactory.shutdown();
    }

    @Test
    public void cachesCollectionState(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final CachedParent parent = new CachedParent();
            parent.children.add(new CachedChild(parent, "child"));
            session.persist(parent);
            return parent.id;
        });
        scope.getSessionFactory().getCache().evictCollectionData();
        scope.getSessionFactory().getStatistics().clear();

        scope.inTransaction((session) -> assertThat(
                session.find(CachedParent.class, id).children).hasSize(1));
        CacheRegionStatistics statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics("collection.children");
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);

        scope.inTransaction((session) -> assertThat(
                session.find(CachedParent.class, id).children).hasSize(1));
        statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics("collection.children");
        assertThat(statistics.getHitCount()).isEqualTo(1);
    }

    @Test
    public void cachesNonstrictReadWriteState(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final NonstrictEntity entity = new NonstrictEntity();
            entity.name = "before";
            session.persist(entity);
            return entity.id;
        });
        scope.getSessionFactory().getCache().evictEntityData(NonstrictEntity.class);
        scope.getSessionFactory().getStatistics().clear();

        scope.inTransaction((session) -> assertThat(
                session.find(NonstrictEntity.class, id).name).isEqualTo("before"));
        scope.inTransaction((session) -> assertThat(
                session.find(NonstrictEntity.class, id).name).isEqualTo("before"));

        final CacheRegionStatistics statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics("nonstrict");
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        assertThat(statistics.getHitCount()).isEqualTo(1);
    }

    @Test
    public void cachesReadOnlyState(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final ReadOnlyEntity entity = new ReadOnlyEntity();
            entity.name = "read-only";
            session.persist(entity);
            return entity.id;
        });
        scope.getSessionFactory().getCache().evictEntityData(ReadOnlyEntity.class);
        scope.getSessionFactory().getStatistics().clear();

        scope.inTransaction((session) -> assertThat(
                session.find(ReadOnlyEntity.class, id).name).isEqualTo("read-only"));
        scope.inTransaction((session) -> assertThat(
                session.find(ReadOnlyEntity.class, id).name).isEqualTo("read-only"));

        final CacheRegionStatistics statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics("read-only");
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        assertThat(statistics.getHitCount()).isEqualTo(1);
    }

    @Entity(name = "CachedParent")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "collection.parent")
    public static class CachedParent {
        @Id
        @GeneratedValue
        private Long id;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "collection.children")
        private List<CachedChild> children = new ArrayList<>();
    }

    @Entity(name = "CachedChild")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "collection.child")
    public static class CachedChild {
        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        private CachedParent parent;

        private String name;

        public CachedChild() {
        }

        public CachedChild(CachedParent parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }

    @Entity(name = "NonstrictEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "nonstrict")
    public static class NonstrictEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;
    }

    @Entity(name = "ReadOnlyEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "read-only")
    public static class ReadOnlyEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;
    }
}
