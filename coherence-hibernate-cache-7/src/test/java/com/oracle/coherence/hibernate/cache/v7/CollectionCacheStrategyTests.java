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
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.access.AccessType;
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
 * Integration coverage for every supported collection cache concurrency strategy.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = CollectionCacheStrategyTests.CollectionOwner.class)
public class CollectionCacheStrategyTests {

    private static final String READ_ONLY_ROLE = CollectionOwner.class.getName() + ".readOnlyValues";
    private static final String READ_WRITE_ROLE = CollectionOwner.class.getName() + ".readWriteValues";
    private static final String NONSTRICT_ROLE = CollectionOwner.class.getName() + ".nonstrictValues";

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
    public void cachesCollectionsWithEverySupportedStrategy(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final CollectionOwner owner = new CollectionOwner();
            owner.readOnlyValues.add("read-only");
            owner.readWriteValues.add("read-write");
            owner.nonstrictValues.add("nonstrict");
            session.persist(owner);
            return owner.id;
        });

        assertCollectionStrategy(scope, id, READ_ONLY_ROLE, "collection.read-only", "read-only",
                AccessType.READ_ONLY);
        assertCollectionStrategy(scope, id, READ_WRITE_ROLE, "collection.read-write", "read-write",
                AccessType.READ_WRITE);
        assertCollectionStrategy(scope, id, NONSTRICT_ROLE, "collection.nonstrict", "nonstrict",
                AccessType.NONSTRICT_READ_WRITE);
    }

    private void assertCollectionStrategy(SessionFactoryScope scope, Long id, String role, String region,
                                          String expectedValue, AccessType expectedAccessType) {
        final org.hibernate.Cache cache = scope.getSessionFactory().getCache();
        cache.evictCollectionData(role);
        scope.getSessionFactory().getStatistics().clear();

        assertThat(scope.getSessionFactory().getMappingMetamodel().getCollectionDescriptor(role)
                .getCacheAccessStrategy().getAccessType()).isEqualTo(expectedAccessType);
        assertThat(cache.containsCollection(role, id)).isFalse();

        assertCollectionValue(scope, id, role, expectedValue);
        assertThat(cache.containsCollection(role, id)).isTrue();
        assertCollectionValue(scope, id, role, expectedValue);

        final CacheRegionStatistics statistics = scope.getSessionFactory().getStatistics()
                .getDomainDataRegionStatistics(region);
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        assertThat(statistics.getHitCount()).isEqualTo(1);
    }

    private void assertCollectionValue(SessionFactoryScope scope, Long id, String role, String expectedValue) {
        scope.inTransaction((session) -> {
            final CollectionOwner owner = session.find(CollectionOwner.class, id);
            final List<String> values;
            if (role.equals(READ_ONLY_ROLE)) {
                values = owner.readOnlyValues;
            }
            else if (role.equals(READ_WRITE_ROLE)) {
                values = owner.readWriteValues;
            }
            else {
                values = owner.nonstrictValues;
            }
            assertThat(values).containsExactly(expectedValue);
        });
    }

    @Entity(name = "CollectionStrategyOwner")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "collection.owner")
    public static class CollectionOwner {
        @Id
        @GeneratedValue
        private Long id;

        @ElementCollection
        @Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "collection.read-only")
        private List<String> readOnlyValues = new ArrayList<>();

        @ElementCollection
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "collection.read-write")
        private List<String> readWriteValues = new ArrayList<>();

        @ElementCollection
        @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "collection.nonstrict")
        private List<String> nonstrictValues = new ArrayList<>();
    }
}
