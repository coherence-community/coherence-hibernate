/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.List;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.query.SelectionQuery;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that entity mutations invalidate cached query results.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.use_query_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = QueryCacheInvalidationTests.QueryEntity.class)
public class QueryCacheInvalidationTests {

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
    public void entityUpdateInvalidatesCachedQueryResults(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final QueryEntity entity = new QueryEntity("before");
            session.persist(entity);
            return entity.id;
        });
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        statistics.clear();

        assertThat(findByName(scope, "before")).extracting((entity) -> entity.id).containsExactly(id);
        assertThat(findByName(scope, "before")).extracting((entity) -> entity.id).containsExactly(id);
        assertThat(statistics.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(statistics.getQueryCachePutCount()).isEqualTo(1);
        assertThat(statistics.getQueryCacheHitCount()).isEqualTo(1);

        scope.inTransaction((session) -> session.find(QueryEntity.class, id).name = "after");

        assertThat(findByName(scope, "before")).isEmpty();
        assertThat(findByName(scope, "after")).extracting((entity) -> entity.id).containsExactly(id);
        assertThat(statistics.getQueryCacheMissCount()).isEqualTo(3);
        assertThat(statistics.getQueryCachePutCount()).isEqualTo(3);
    }

    private List<QueryEntity> findByName(SessionFactoryScope scope, String name) {
        return scope.fromTransaction((session) -> {
            final SelectionQuery<QueryEntity> query = session.createSelectionQuery(
                    "from QueryInvalidationEntity where name = :name", QueryEntity.class);
            query.setParameter("name", name);
            query.setCacheable(true);
            query.setCacheRegion("query.invalidation");
            return query.getResultList();
        });
    }

    @Entity(name = "QueryInvalidationEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "query.entity")
    public static class QueryEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String name;

        public QueryEntity() {
        }

        public QueryEntity(String name) {
            this.name = name;
        }
    }
}
