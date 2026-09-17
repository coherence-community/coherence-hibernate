/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.List;

import com.oracle.coherence.hibernate.cache.v7.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v7.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v7.support.Foo;
import com.tangosol.net.CacheFactory;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.use_query_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = Foo.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadWriteCacheTests {

    private Long idOfSavedItem = null;

    @AfterAll
    public static void after() {
        CacheFactory.shutdown();
    }

    @Test
    @Order(1)
    public void persistItem(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();

        final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) scope.getSessionFactory().getCache().getRegion("foo");
        final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

        assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(0);

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();
        final Foo itemToSave = new Foo("bar");
        session.persist(itemToSave);
        this.idOfSavedItem = itemToSave.getId();
        session.flush();
        session.getTransaction().commit();

        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");
        assertThat(itemStatistics.getPutCount()).isEqualTo(1);
        assertThat(itemStatistics.getHitCount()).isEqualTo(0);
        assertThat(itemStatistics.getMissCount()).isEqualTo(0);
    }

    @Test
    @Order(2)
    public void getPersistedItem(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");
        scope.getSessionFactory().getCache().getRegion("foo");
        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();
        final Foo itemFromCache = session.find(Foo.class, this.idOfSavedItem);
        session.getTransaction().commit();
        session.clear();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(1);
        assertThat(itemStatistics.getHitCount()).isEqualTo(1);
        assertThat(itemStatistics.getMissCount()).isEqualTo(0);
    }

    @Test
    @Order(3)
    public void updateAndRollbackPersistedItem(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();
        final Foo itemToUpdate = session.find(Foo.class, this.idOfSavedItem);
        itemToUpdate.setName("newdata");
        session.merge(itemToUpdate);
        session.flush();
        session.getTransaction().rollback();
        session.clear();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(1);
        assertThat(itemStatistics.getHitCount()).isEqualTo(2);
        assertThat(itemStatistics.getMissCount()).isEqualTo(0);
    }

    @Test
    @Order(4)
    public void retrievePersistedItemAfterRollBack(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        final Foo fooItem = session.find(Foo.class, this.idOfSavedItem);

        assertThat(itemStatistics.getPutCount()).isEqualTo(1);
        assertThat(itemStatistics.getHitCount()).isEqualTo(3);
        assertThat(itemStatistics.getMissCount()).isEqualTo(0);
        assertThat(fooItem.getName()).isEqualTo("bar");
    }

    @Test
    @Order(5)
    public void updateItem(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();
        final Foo itemToUpdate = session.find(Foo.class, this.idOfSavedItem);

        itemToUpdate.setName("coherence_rocks");
        session.merge(itemToUpdate);
        session.flush();
        session.getTransaction().commit();
        session.clear();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(2);
        assertThat(itemStatistics.getHitCount()).isEqualTo(4);
        assertThat(itemStatistics.getMissCount()).isEqualTo(0);
    }

    @Test
    @Order(6)
    public void deleteItem(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();
        final Foo itemToUpdate = session.find(Foo.class, this.idOfSavedItem);

        session.remove(itemToUpdate);
        session.getTransaction().commit();
        session.clear();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(2);
        assertThat(itemStatistics.getHitCount()).isEqualTo(5);
        assertThat(itemStatistics.getMissCount()).isEqualTo(0);
    }

    @Test
    @Order(7)
    public void getMissingItem(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();
        final Foo itemToUpdate = session.find(Foo.class, this.idOfSavedItem);
        session.getTransaction().commit();
        session.clear();
        session.close();

        assertThat(itemToUpdate).isNull();

        assertThat(itemStatistics.getPutCount()).isEqualTo(2);
        assertThat(itemStatistics.getHitCount()).isEqualTo(5);
        assertThat(itemStatistics.getMissCount()).isEqualTo(1);

    }

    @Test
    @Order(8)
    public void addMultipleItems(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();

        session.persist(new Foo("bar1"));
        session.persist(new Foo("bar2"));
        session.persist(new Foo("kenny1"));
        session.persist(new Foo("kenny2"));

        session.getTransaction().commit();
        session.clear();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(6);
        assertThat(itemStatistics.getHitCount()).isEqualTo(5);
        assertThat(itemStatistics.getMissCount()).isEqualTo(1);

    }

    @Test
    @Order(9)
    public void query(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();

        final Query<Foo> query = session.createNamedQuery("fooQuery", Foo.class);
        query.setCacheable(true);
        query.setCacheRegion("fooQueryCache");
        query.setParameter("name", "kenny%");
        final List<Foo> fooList = query.getResultList();
        session.getTransaction().commit();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(6);
        assertThat(itemStatistics.getHitCount()).isEqualTo(5);
        assertThat(itemStatistics.getMissCount()).isEqualTo(1);

        assertThat(fooList.size()).isEqualTo(2);

        final CacheRegionStatistics fooListStatistics = statistics.getDomainDataRegionStatistics("fooQueryCache");

        assertThat(fooListStatistics.getPutCount()).isEqualTo(1);
        assertThat(fooListStatistics.getHitCount()).isEqualTo(0);
        assertThat(fooListStatistics.getMissCount()).isEqualTo(1);
    }

    @Test
    @Order(10)
    public void querySecondTime(SessionFactoryScope scope) {
        final Statistics statistics = scope.getSessionFactory().getStatistics();
        final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

        final Session session = scope.getSessionFactory().openSession();
        session.beginTransaction();

        final Query<Foo> query = session.createNamedQuery("fooQuery", Foo.class);
        query.setCacheable(true);
        query.setCacheRegion("fooQueryCache");
        query.setParameter("name", "kenny%");
        final List<Foo> fooList = query.getResultList();
        session.getTransaction().commit();
        session.close();

        assertThat(itemStatistics.getPutCount()).isEqualTo(6);
        assertThat(itemStatistics.getHitCount()).isEqualTo(5);
        assertThat(itemStatistics.getMissCount()).isEqualTo(1);

        assertThat(fooList.size()).isEqualTo(2);

        final CacheRegionStatistics fooListStatistics = statistics.getDomainDataRegionStatistics("fooQueryCache");

        assertThat(fooListStatistics.getPutCount()).isEqualTo(1);
        assertThat(fooListStatistics.getHitCount()).isEqualTo(1);
        assertThat(fooListStatistics.getMissCount()).isEqualTo(1);
    }
}
