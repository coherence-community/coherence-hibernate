/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.List;

import com.oracle.coherence.hibernate.cache.v7.support.Foo;
import com.tangosol.net.CacheFactory;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies second-level cache behavior added or changed in Hibernate 7.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = Foo.class)
public class Hibernate7CacheSemanticsTests {

    @AfterAll
    public static void afterAll() {
        CacheFactory.shutdown();
    }

    @Test
    public void statelessSessionUsesSecondLevelCacheByDefault(SessionFactoryScope scope) {
        final Long id = persist(scope, "stateless");
        resetCacheAndStatistics(scope);

        try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
            session.beginTransaction();
            assertThat(session.get(Foo.class, id).getName()).isEqualTo("stateless");
            session.getTransaction().commit();
        }

        CacheRegionStatistics statistics = regionStatistics(scope);
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);

        try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
            session.beginTransaction();
            assertThat(session.get(Foo.class, id).getName()).isEqualTo("stateless");
            session.getTransaction().commit();
        }

        statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isEqualTo(1);
        deleteAll(scope);
    }

    @Test
    public void statelessSessionCacheModeGetReadsButDoesNotPopulateSecondLevelCache(SessionFactoryScope scope) {
        final Long id = persist(scope, "stateless-get");
        resetCacheAndStatistics(scope);

        assertThat(loadWithStatelessCacheMode(scope, id, CacheMode.GET)).isEqualTo("stateless-get");

        CacheRegionStatistics statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isZero();
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isZero();

        assertThat(loadWithStatelessCacheMode(scope, id, CacheMode.GET)).isEqualTo("stateless-get");

        statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isZero();
        assertThat(statistics.getMissCount()).isEqualTo(2);
        assertThat(statistics.getPutCount()).isZero();

        assertThat(loadWithStatelessCacheMode(scope, id, CacheMode.NORMAL)).isEqualTo("stateless-get");
        scope.getSessionFactory().getStatistics().clear();

        assertThat(loadWithStatelessCacheMode(scope, id, CacheMode.GET)).isEqualTo("stateless-get");

        statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isEqualTo(1);
        assertThat(statistics.getMissCount()).isZero();
        assertThat(statistics.getPutCount()).isZero();
        deleteAll(scope);
    }

    @Test
    public void statelessSessionCacheModeIgnoreBypassesSecondLevelCache(SessionFactoryScope scope) {
        final Long id = persist(scope, "stateless-ignore");
        resetCacheAndStatistics(scope);

        assertThat(loadWithStatelessCacheMode(scope, id, CacheMode.IGNORE)).isEqualTo("stateless-ignore");

        CacheRegionStatistics statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isZero();
        assertThat(statistics.getMissCount()).isZero();
        assertThat(statistics.getPutCount()).isZero();

        assertThat(loadWithStatelessCacheMode(scope, id, CacheMode.NORMAL)).isEqualTo("stateless-ignore");

        statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isZero();
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        deleteAll(scope);
    }

    @Test
    public void cacheModeIgnoreBypassesSecondLevelCache(SessionFactoryScope scope) {
        final Long id = persist(scope, "ignored");
        resetCacheAndStatistics(scope);

        try (Session session = scope.getSessionFactory().openSession()) {
            assertThat(session.find(Foo.class, id, CacheMode.IGNORE).getName()).isEqualTo("ignored");
        }

        CacheRegionStatistics statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isZero();
        assertThat(statistics.getMissCount()).isZero();
        assertThat(statistics.getPutCount()).isZero();

        try (Session session = scope.getSessionFactory().openSession()) {
            assertThat(session.find(Foo.class, id).getName()).isEqualTo("ignored");
        }

        statistics = regionStatistics(scope);
        assertThat(statistics.getMissCount()).isEqualTo(1);
        assertThat(statistics.getPutCount()).isEqualTo(1);
        deleteAll(scope);
    }

    @Test
    public void cacheModeRefreshSessionRefreshesManagedAndCachedState(SessionFactoryScope scope) {
        final Long id = persist(scope, "before-refresh");

        try (Session session = scope.getSessionFactory().openSession()) {
            final Foo managed = session.find(Foo.class, id);
            assertThat(managed.getName()).isEqualTo("before-refresh");

            scope.inTransaction((updateSession) -> updateSession.createMutationQuery(
                            "update Foo set name = :name where id = :id")
                    .setParameter("name", "after-refresh")
                    .setParameter("id", id)
                    .executeUpdate());

            session.beginTransaction();
            final Foo refreshed = session.find(Foo.class, id, CacheMode.REFRESH_SESSION);
            session.getTransaction().commit();
            assertThat(refreshed).isSameAs(managed);
            assertThat(refreshed.getName()).isEqualTo("after-refresh");
        }

        scope.getSessionFactory().getStatistics().clear();
        try (Session session = scope.getSessionFactory().openSession()) {
            session.beginTransaction();
            assertThat(session.find(Foo.class, id).getName()).isEqualTo("after-refresh");
            session.getTransaction().commit();
        }
        try (Session session = scope.getSessionFactory().openSession()) {
            session.beginTransaction();
            assertThat(session.find(Foo.class, id).getName()).isEqualTo("after-refresh");
            session.getTransaction().commit();
        }
        final CacheRegionStatistics statistics = regionStatistics(scope);
        assertThat(statistics.getMissCount()).isZero();
        assertThat(statistics.getPutCount()).isZero();
        assertThat(statistics.getHitCount()).isEqualTo(2);
        deleteAll(scope);
    }

    @Test
    public void findMultiplePreservesOrderAndMissingEntries(SessionFactoryScope scope) {
        final Long firstId = persist(scope, "first");
        final Long secondId = persist(scope, "second");
        final long missingId = secondId + 100L;

        try (Session session = scope.getSessionFactory().openSession()) {
            final List<Foo> results = session.findMultiple(
                    Foo.class,
                    List.of(secondId, missingId, firstId),
                    CacheMode.IGNORE);

            assertThat(results).hasSize(3);
            assertThat(results.get(0).getId()).isEqualTo(secondId);
            assertThat(results.get(1)).isNull();
            assertThat(results.get(2).getId()).isEqualTo(firstId);
        }
        deleteAll(scope);
    }

    @Test
    public void findMultipleUsesSecondLevelCacheAndPreservesMissingEntries(SessionFactoryScope scope) {
        final Long firstId = persist(scope, "first-cached");
        final Long secondId = persist(scope, "second-cached");
        final long missingId = secondId + 100L;
        resetCacheAndStatistics(scope);

        assertFindMultipleResults(scope, secondId, missingId, firstId);
        CacheRegionStatistics statistics = regionStatistics(scope);
        assertThat(statistics.getPutCount()).isEqualTo(2);
        assertThat(statistics.getMissCount()).isEqualTo(3);

        assertFindMultipleResults(scope, secondId, missingId, firstId);
        statistics = regionStatistics(scope);
        assertThat(statistics.getHitCount()).isEqualTo(2);
        deleteAll(scope);
    }

    @Test
    public void selectionQueryRefreshSessionRefreshesManagedAndCachedState(SessionFactoryScope scope) {
        final Long id = persist(scope, "before-query-refresh");

        try (Session session = scope.getSessionFactory().openSession()) {
            final Foo managed = session.find(Foo.class, id);
            scope.inTransaction((updateSession) -> updateSession.createMutationQuery(
                            "update Foo set name = :name where id = :id")
                    .setParameter("name", "after-query-refresh")
                    .setParameter("id", id)
                    .executeUpdate());

            session.beginTransaction();
            final SelectionQuery<Foo> query = session.createSelectionQuery(
                    "from Foo where id = :id", Foo.class);
            query.setParameter("id", id);
            query.setCacheMode(CacheMode.REFRESH_SESSION);
            final Foo refreshed = query.getSingleResult();
            session.getTransaction().commit();

            assertThat(refreshed).isSameAs(managed);
            assertThat(refreshed.getName()).isEqualTo("after-query-refresh");
        }

        scope.inTransaction((session) -> assertThat(session.find(Foo.class, id).getName())
                .isEqualTo("after-query-refresh"));
        deleteAll(scope);
    }

    @Test
    public void statelessBulkMutationsDoNotLeaveStaleSecondLevelEntries(SessionFactoryScope scope) {
        final Foo first = new Foo("bulk-first");
        final Foo second = new Foo("bulk-second");

        try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
            session.beginTransaction();
            session.insertMultiple(List.of(first, second));
            session.getTransaction().commit();
        }
        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
        assertThat(loadName(scope, first.getId())).isEqualTo("bulk-first");
        assertThat(loadName(scope, second.getId())).isEqualTo("bulk-second");

        first.setName("bulk-first-updated");
        second.setName("bulk-second-updated");
        try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
            session.beginTransaction();
            session.updateMultiple(List.of(first, second));
            session.getTransaction().commit();
        }
        assertThat(loadName(scope, first.getId())).isEqualTo("bulk-first-updated");
        assertThat(loadName(scope, second.getId())).isEqualTo("bulk-second-updated");

        try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
            session.beginTransaction();
            session.deleteMultiple(List.of(first, second));
            session.getTransaction().commit();
        }
        scope.inTransaction((session) -> {
            assertThat(session.find(Foo.class, first.getId())).isNull();
            assertThat(session.find(Foo.class, second.getId())).isNull();
        });
    }

    private void assertFindMultipleResults(SessionFactoryScope scope, Long secondId, long missingId, Long firstId) {
        scope.inTransaction((session) -> {
            final List<Foo> results = session.findMultiple(
                    Foo.class,
                    List.of(secondId, missingId, firstId));
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getId()).isEqualTo(secondId);
            assertThat(results.get(1)).isNull();
            assertThat(results.get(2).getId()).isEqualTo(firstId);
        });
    }

    private String loadName(SessionFactoryScope scope, Long id) {
        return scope.fromTransaction((session) -> session.find(Foo.class, id).getName());
    }

    private String loadWithStatelessCacheMode(SessionFactoryScope scope, Long id, CacheMode cacheMode) {
        try (StatelessSession session = scope.getSessionFactory().openStatelessSession()) {
            session.setCacheMode(cacheMode);
            session.beginTransaction();
            final String name = session.get(Foo.class, id).getName();
            session.getTransaction().commit();
            return name;
        }
    }

    private Long persist(SessionFactoryScope scope, String name) {
        final Long[] id = new Long[1];
        scope.inTransaction((session) -> {
            final Foo foo = new Foo(name);
            session.persist(foo);
            id[0] = foo.getId();
        });
        return id[0];
    }

    private void resetCacheAndStatistics(SessionFactoryScope scope) {
        scope.getSessionFactory().getCache().evictEntityData(Foo.class);
        scope.getSessionFactory().getStatistics().clear();
    }

    private CacheRegionStatistics regionStatistics(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        return statistics.getDomainDataRegionStatistics("foo");
    }

    private void deleteAll(SessionFactoryScope scope) {
        scope.inTransaction((session) -> session.createMutationQuery("delete from Foo").executeUpdate());
        scope.getSessionFactory().getCache().evictEntityData(Foo.class);
    }
}
