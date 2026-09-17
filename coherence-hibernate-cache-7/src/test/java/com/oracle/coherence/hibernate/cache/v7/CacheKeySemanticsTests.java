/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.io.Serializable;
import java.util.Objects;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.access.EntityDataAccess;
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
 * Verifies composite and tenant-sensitive default cache keys.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = CacheKeySemanticsTests.CompositeEntity.class)
public class CacheKeySemanticsTests {

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
    public void compositeIdentifiersRoundTripThroughSecondLevelCache(SessionFactoryScope scope) {
        final CompositeId id = new CompositeId("group", 7);
        scope.inTransaction((session) -> session.persist(new CompositeEntity(id, "value")));
        scope.getSessionFactory().getCache().evictEntityData(CompositeEntity.class);
        scope.getSessionFactory().getStatistics().clear();

        scope.inTransaction((session) -> assertThat(
                session.find(CompositeEntity.class, new CompositeId("group", 7)).value).isEqualTo("value"));
        scope.inTransaction((session) -> assertThat(
                session.find(CompositeEntity.class, new CompositeId("group", 7)).value).isEqualTo("value"));

        final CacheRegionStatistics statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics("composite.entity");
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        assertThat(statistics.getHitCount()).isEqualTo(1);
    }

    @Test
    public void defaultCacheKeysKeepTenantIdentifiersDistinct(SessionFactoryScope scope) {
        final EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
                .getEntityDescriptor(CompositeEntity.class);
        final EntityDataAccess access = persister.getCacheAccessStrategy();
        final CompositeId id = new CompositeId("group", 7);

        final Object tenantOneKey = access.generateCacheKey(id, persister, scope.getSessionFactory(), "tenant-one");
        final Object tenantTwoKey = access.generateCacheKey(id, persister, scope.getSessionFactory(), "tenant-two");
        final Object equivalentTenantOneKey = access.generateCacheKey(
                new CompositeId("group", 7), persister, scope.getSessionFactory(), "tenant-one");

        assertThat(tenantOneKey).isEqualTo(equivalentTenantOneKey);
        assertThat(tenantOneKey).isNotEqualTo(tenantTwoKey);
        assertThat(access.getCacheKeyId(tenantOneKey)).isInstanceOf(Object[].class);
        assertThat((Object[]) access.getCacheKeyId(tenantOneKey)).containsExactly("group", 7);
    }

    @Embeddable
    public static class CompositeId implements Serializable {
        private String groupName;
        private int sequence;

        public CompositeId() {
        }

        public CompositeId(String groupName, int sequence) {
            this.groupName = groupName;
            this.sequence = sequence;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof CompositeId)) {
                return false;
            }
            final CompositeId that = (CompositeId) object;
            return this.sequence == that.sequence && Objects.equals(this.groupName, that.groupName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.groupName, this.sequence);
        }
    }

    @Entity(name = "CompositeCacheEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "composite.entity")
    public static class CompositeEntity {
        @EmbeddedId
        private CompositeId id;

        private String value;

        public CompositeEntity() {
        }

        public CompositeEntity(CompositeId id, String value) {
            this.id = id;
            this.value = value;
        }
    }
}
