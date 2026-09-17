/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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
 * Verifies mutation invalidation for the nonstrict read/write strategy.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = NonstrictStrategyMutationTests.NonstrictEntity.class)
public class NonstrictStrategyMutationTests {

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
    public void updateInvalidatesAndReloadsNonstrictEntry(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final NonstrictEntity entity = new NonstrictEntity("before");
            session.persist(entity);
            return entity.id;
        });
        final org.hibernate.Cache cache = scope.getSessionFactory().getCache();
        cache.evictEntityData(NonstrictEntity.class);

        assertThat(loadName(scope, id)).isEqualTo("before");
        assertThat(cache.containsEntity(NonstrictEntity.class, id)).isTrue();

        scope.inTransaction((session) -> session.find(NonstrictEntity.class, id).name = "after");
        assertThat(cache.containsEntity(NonstrictEntity.class, id)).isFalse();
        scope.getSessionFactory().getStatistics().clear();

        assertThat(loadName(scope, id)).isEqualTo("after");
        assertThat(loadName(scope, id)).isEqualTo("after");
        final CacheRegionStatistics statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics("nonstrict.mutation");
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        assertThat(statistics.getHitCount()).isEqualTo(1);
    }

    private String loadName(SessionFactoryScope scope, Long id) {
        return scope.fromTransaction((session) -> session.find(NonstrictEntity.class, id).name);
    }

    @Entity(name = "NonstrictMutationEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "nonstrict.mutation")
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
