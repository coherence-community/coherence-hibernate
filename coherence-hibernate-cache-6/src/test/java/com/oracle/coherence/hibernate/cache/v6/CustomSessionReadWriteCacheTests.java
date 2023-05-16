/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6;

import java.util.List;

import com.oracle.coherence.hibernate.cache.v6.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v6.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v6.support.Foo;
import com.tangosol.net.CacheFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CustomSessionReadWriteCacheTests extends BaseCoreFunctionalTestCase {

	private Long idOfSavedItem = null;

	@AfterClass
	public static void after() {
		CacheFactory.shutdown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty(Environment.CACHE_REGION_PREFIX, "");
		cfg.setProperty(Environment.SHOW_SQL, "true");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.CACHE_REGION_FACTORY, CoherenceRegionFactory.class.getName());
		cfg.setProperty("com.oracle.coherence.hibernate.cache.cache_config_file_path", "tests-hibernate-second-level-cache-config.xml");
	}

	@Test
	public void test_01_persistItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();

		final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) this.sessionFactory().getCache().getRegion("foo");
		final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		Assertions.assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(0);

		final Session session = openSession();
		session.beginTransaction();
		final Foo itemToSave = new Foo("bar");
		this.idOfSavedItem = (Long) session.save(itemToSave);
		session.flush();
		session.getTransaction().commit();

		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");
		assertThat(itemStatistics.getPutCount()).isEqualTo(1);
		assertThat(itemStatistics.getHitCount()).isEqualTo(0);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_02_getPersistedItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");
		this.sessionFactory().getCache().getRegion("foo");
		final Session session = openSession();
		session.beginTransaction();
		final Foo itemFromCache = session.get(Foo.class, this.idOfSavedItem);
		session.getTransaction().commit();
		session.clear();
		session.close();

		assertThat(itemStatistics.getPutCount()).isEqualTo(1);
		assertThat(itemStatistics.getHitCount()).isEqualTo(1);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_03_updateAndRollbackPersistedItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		session.beginTransaction();
		final Foo itemToUpdate = session.get(Foo.class, this.idOfSavedItem);
		itemToUpdate.setName("newdata");
		session.update(itemToUpdate);
		session.flush();
		session.getTransaction().rollback();
		session.clear();
		session.close();

		assertThat(itemStatistics.getPutCount()).isEqualTo(1);
		assertThat(itemStatistics.getHitCount()).isEqualTo(2);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_04_retrievePersistedItemAfterRollBack() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		final Foo fooItem = session.get(Foo.class, this.idOfSavedItem);

		assertThat(itemStatistics.getPutCount()).isEqualTo(1);
		assertThat(itemStatistics.getHitCount()).isEqualTo(3);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
		assertThat(fooItem.getName()).isEqualTo("bar");
	}

	@Test
	public void test_05_updateItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		session.beginTransaction();
		final Foo itemToUpdate = session.get(Foo.class, this.idOfSavedItem);

		itemToUpdate.setName("coherence_rocks");
		session.update(itemToUpdate);
		session.flush();
		session.getTransaction().commit();
		session.clear();
		session.close();

		assertThat(itemStatistics.getPutCount()).isEqualTo(2);
		assertThat(itemStatistics.getHitCount()).isEqualTo(4);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_06_deleteItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		session.beginTransaction();
		final Foo itemToUpdate = session.get(Foo.class, this.idOfSavedItem);

		session.delete(itemToUpdate);
		session.getTransaction().commit();
		session.clear();
		session.close();

		assertThat(itemStatistics.getPutCount()).isEqualTo(2);
		assertThat(itemStatistics.getHitCount()).isEqualTo(5);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_07_getMissingItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		session.beginTransaction();
		final Foo itemToUpdate = session.get(Foo.class, this.idOfSavedItem);
		session.getTransaction().commit();
		session.clear();
		session.close();

		assertThat(itemToUpdate).isNull();

		assertThat(itemStatistics.getPutCount()).isEqualTo(2);
		assertThat(itemStatistics.getHitCount()).isEqualTo(5);
		assertThat(itemStatistics.getMissCount()).isEqualTo(1);

	}

	@Test
	public void test_08_addMultipleItems() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
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
	public void test_09_Query() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		session.beginTransaction();

		final Query<Foo> query = session.getNamedQuery("fooQuery");
		query.setCacheable(true);
		query.setCacheRegion("fooQueryCache");
		query.setParameter("name", "kenny%");
		final List<Foo> fooList = query.getResultList();

		final CacheRegionStatistics fooListStatistics = statistics.getDomainDataRegionStatistics("fooQueryCache");

		final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) this.sessionFactory().getCache().getRegion("foo");
		final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		session.getTransaction().commit();
		session.close();

		/*
		 * Compared to Hibernate 5.6.x, this test behaves slightly different. The returned entities of the query will
		 * also be updated. The minimalPuts in AbstractEntityInitializer are hard-coded to false when calling
		 * {@link org.hibernate.cache.spi.access.EntityDataAccess#putFromLoad(SharedSessionContractImplementor, Object, Object, Object)}.
		 *
		 * see:
		 * - https://github.com/hibernate/hibernate-orm/blob/main/hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/AbstractEntityInitializer.java#L979
		 * - https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#query-result-cache
		 */
		// assertThat(itemStatistics.getPutCount()).isEqualTo(6);
		assertThat(itemStatistics.getPutCount()).isEqualTo(8);

		assertThat(itemStatistics.getHitCount()).isEqualTo(5);
		assertThat(itemStatistics.getMissCount()).isEqualTo(1);

		assertThat(fooList.size()).isEqualTo(2);

		assertThat(fooListStatistics.getPutCount()).isEqualTo(1);
		assertThat(fooListStatistics.getHitCount()).isEqualTo(0);
		assertThat(fooListStatistics.getMissCount()).isEqualTo(1);
	}

	@Test
	public void test_10_QuerySecondTime() {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final Session session = openSession();
		session.beginTransaction();

		final Query<Foo> query = session.getNamedQuery("fooQuery");
		query.setCacheable(true);
		query.setCacheRegion("fooQueryCache");
		query.setParameter("name", "kenny%");
		final List<Foo> fooList = query.getResultList();
		session.getTransaction().commit();
		session.close();

		assertThat(itemStatistics.getPutCount()).isEqualTo(8);
		assertThat(itemStatistics.getHitCount()).isEqualTo(5);
		assertThat(itemStatistics.getMissCount()).isEqualTo(1);

		assertThat(fooList.size()).isEqualTo(2);

		final CacheRegionStatistics fooListStatistics = statistics.getDomainDataRegionStatistics("fooQueryCache");

		assertThat(fooListStatistics.getPutCount()).isEqualTo(1);
		assertThat(fooListStatistics.getHitCount()).isEqualTo(1);
		assertThat(fooListStatistics.getMissCount()).isEqualTo(1);
	}
}
