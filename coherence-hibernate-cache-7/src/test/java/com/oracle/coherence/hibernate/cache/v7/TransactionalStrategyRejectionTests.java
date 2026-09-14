/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.hibernate.cache.v7.access.AbstractCoherenceEntityDataAccess;
import com.oracle.coherence.hibernate.cache.v7.access.CoherenceDomainDataRegionImpl;
import com.tangosol.net.CacheFactory;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the unsupported transactional strategy is rejected during bootstrap and region construction.
 */
public class TransactionalStrategyRejectionTests {

    @AfterEach
    public void shutdownCoherence() {
        CacheFactory.shutdown();
    }

    @Test
    public void transactionalEntityStrategyFailsWithDocumentedCacheException() {
        assertBootstrapRejected(TransactionalEntity.class);
    }

    @Test
    public void transactionalCollectionStrategyFailsWithDocumentedCacheException() {
        assertBootstrapRejected(TransactionalCollectionOwner.class);
    }

    @Test
    public void transactionalNaturalIdStrategyFailsWithDocumentedCacheException() {
        final NaturalIdDataCachingConfig naturalIdConfig = configurationProxy(NaturalIdDataCachingConfig.class,
                Map.of("getAccessType", AccessType.TRANSACTIONAL,
                        "getNavigableRole", new NavigableRole("TransactionalNaturalIdEntity")));
        // Isolate natural-id configuration so entity-cache rejection cannot mask this path.
        final DomainDataRegionConfig regionConfig = configurationProxy(DomainDataRegionConfig.class,
                Map.of("getRegionName", "transactional.natural-id",
                        "getEntityCaching", List.of(),
                        "getNaturalIdCaching", List.of(naturalIdConfig),
                        "getCollectionCaching", List.of()));

        assertThatThrownBy(() -> new CoherenceDomainDataRegionImpl(regionConfig,
                new CoherenceRegionFactory(),
                configurationProxy(DomainDataStorageAccess.class, Map.of()),
                DefaultCacheKeysFactory.INSTANCE,
                configurationProxy(DomainDataRegionBuildingContext.class, Map.of())))
                .isInstanceOf(CacheException.class)
                .hasMessage(AbstractCoherenceEntityDataAccess.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
    }

    private void assertBootstrapRejected(Class<?> entityClass) {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver")
                .applySetting("hibernate.connection.url", "jdbc:hsqldb:mem:transactional-rejection")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.cache.use_second_level_cache", "true")
                .applySetting("hibernate.cache.region.factory_class", CoherenceRegionFactory.class.getName())
                .applySetting("com.oracle.coherence.hibernate.cache.cache_config_file_path",
                        "tests-hibernate-second-level-cache-config.xml")
                .build();

        try {
            assertThatThrownBy(() -> {
                try (SessionFactory ignored = new MetadataSources(registry)
                        .addAnnotatedClass(entityClass)
                        .buildMetadata()
                        .buildSessionFactory()) {
                    // Bootstrap must fail before a usable SessionFactory is returned.
                }
            })
                    .isInstanceOf(CacheException.class)
                    .hasMessage(AbstractCoherenceEntityDataAccess.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE)
                    .hasMessageContaining("Supported strategies: READ_ONLY, READ_WRITE, NONSTRICT_READ_WRITE.");
        }
        finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static <T> T configurationProxy(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, arguments) -> values.get(method.getName())));
    }

    @Entity(name = "TransactionalCacheEntity")
    @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "transactional.entity")
    public static class TransactionalEntity {
        @Id
        private Long id;
    }

    @Entity(name = "TransactionalCollectionOwner")
    public static class TransactionalCollectionOwner {
        @Id
        private Long id;

        @ElementCollection
        @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = "transactional.collection")
        private List<String> values = new ArrayList<>();
    }
}
