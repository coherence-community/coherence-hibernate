/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies raw entity and collection identifiers with the configured simple factory.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "hibernate.cache.keys_factory", value = "simple"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = SimpleCacheKeysFactoryTests.CachedEntity.class)
public class SimpleCacheKeysFactoryTests {

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
    public void rawIdentifiersRoundTripThroughEntityAndCollectionCaches(SessionFactoryScope scope) {
        final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
        final CoherenceRegionFactory factory = (CoherenceRegionFactory) sessionFactory.getCache().getRegionFactory();
        final String collectionRole = CachedEntity.class.getName() + ".labels";

        scope.inTransaction((session) -> {
            final CachedEntity entity = new CachedEntity();
            entity.id = 7L;
            entity.value = "value";
            entity.labels.add("tag");
            session.persist(entity);
        });
        sessionFactory.getCache().evictEntityData(CachedEntity.class);
        sessionFactory.getCache().evictCollectionData(collectionRole);
        sessionFactory.getStatistics().clear();

        assertCachedValues(scope);
        assertCachedValues(scope);

        for (String region : List.of("simple.keys.entity", "simple.keys.labels")) {
            assertThat(factory.ensureNamedCache(region).keySet()).isEqualTo(Set.of(7L));
            final CacheRegionStatistics statistics = sessionFactory.getStatistics().getDomainDataRegionStatistics(region);
            assertThat(statistics.getMissCount()).isEqualTo(1);
            assertThat(statistics.getPutCount()).isEqualTo(1);
            assertThat(statistics.getHitCount()).isEqualTo(1);
        }
        assertThat(sessionFactory.getCache().containsEntity(CachedEntity.class, 7L)).isTrue();
        assertThat(sessionFactory.getCache().containsCollection(collectionRole, 7L)).isTrue();
        sessionFactory.getCache().evictEntityData(CachedEntity.class, 7L);
        sessionFactory.getCache().evictCollectionData(collectionRole, 7L);
        assertThat(sessionFactory.getCache().containsEntity(CachedEntity.class, 7L)).isFalse();
        assertThat(sessionFactory.getCache().containsCollection(collectionRole, 7L)).isFalse();
    }

    private void assertCachedValues(SessionFactoryScope scope) {
        scope.inTransaction((session) -> {
            final CachedEntity entity = session.find(CachedEntity.class, 7L);
            assertThat(entity.value).isEqualTo("value");
            assertThat(entity.labels).containsExactly("tag");
        });
    }

    @Entity(name = "SimpleKeyCacheEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "simple.keys.entity")
    public static class CachedEntity {
        @Id
        private Long id;

        private String value;

        @ElementCollection
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "simple.keys.labels")
        private List<String> labels = new ArrayList<>();
    }
}
