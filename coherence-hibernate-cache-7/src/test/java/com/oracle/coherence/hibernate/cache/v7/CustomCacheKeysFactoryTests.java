/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.io.Serializable;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
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
 * Verifies that a configured factory is used by the region's cache access strategy.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "hibernate.cache.keys_factory", value = "com.oracle.coherence.hibernate.cache.v7.CustomCacheKeysFactoryTests$WrappingCacheKeysFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = CustomCacheKeysFactoryTests.CachedEntity.class)
public class CustomCacheKeysFactoryTests {

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
    public void customKeysRoundTripThroughSecondLevelCache(SessionFactoryScope scope) {
        final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
        final EntityPersister persister = sessionFactory.getMappingMetamodel().getEntityDescriptor(CachedEntity.class);
        final EntityDataAccess access = persister.getCacheAccessStrategy();
        final Object key = access.generateCacheKey(7L, persister, sessionFactory, null);
        assertThat(key).isInstanceOf(WrappedKey.class);
        assertThat(access.getCacheKeyId(key)).isEqualTo(7L);

        scope.inTransaction((session) -> session.persist(new CachedEntity(7L, "value")));
        sessionFactory.getCache().evictEntityData(CachedEntity.class);
        sessionFactory.getStatistics().clear();

        scope.inTransaction((session) -> assertThat(session.find(CachedEntity.class, 7L).value).isEqualTo("value"));
        scope.inTransaction((session) -> assertThat(session.find(CachedEntity.class, 7L).value).isEqualTo("value"));

        final CacheRegionStatistics statistics = sessionFactory.getStatistics()
                .getDomainDataRegionStatistics("custom.keys.entity");
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        assertThat(statistics.getHitCount()).isEqualTo(1);
        assertThat(sessionFactory.getCache().containsEntity(CachedEntity.class, 7L)).isTrue();
        sessionFactory.getCache().evictEntityData(CachedEntity.class, 7L);
        assertThat(sessionFactory.getCache().containsEntity(CachedEntity.class, 7L)).isFalse();
    }

    public static class WrappingCacheKeysFactory extends DefaultCacheKeysFactory {
        @Override
        public Object createEntityKey(Object id, EntityPersister persister, SessionFactoryImplementor factory,
                                      String tenantIdentifier) {
            return new WrappedKey(super.createEntityKey(id, persister, factory, tenantIdentifier));
        }

        @Override
        public Object getEntityId(Object cacheKey) {
            return super.getEntityId(((WrappedKey) cacheKey).delegate());
        }
    }

    public record WrappedKey(Object delegate) implements Serializable {
    }

    @Entity(name = "CustomKeyCacheEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "custom.keys.entity")
    public static class CachedEntity {
        @Id
        private Long id;

        private String value;

        public CachedEntity() {
        }

        public CachedEntity(Long id, String value) {
            this.id = id;
            this.value = value;
        }
    }
}
