/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53;

import com.oracle.coherence.hibernate.cache.v53.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.v53.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v53.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v53.support.Foo;
import com.tangosol.net.CacheFactory;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
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
public class ReadWriteCacheExpirationTests extends BaseCoreFunctionalTestCase {

	private Long idOfSavedItem = null;

	@AfterClass
	public static void after() {
		CacheFactory.shutdown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty(Environment.DRIVER, org.hsqldb.jdbc.JDBCDriver.class.getName());
		cfg.setProperty(Environment.URL, "jdbc:hsqldb:mem:db1");
		cfg.setProperty(Environment.USER, "sa");
		cfg.setProperty(Environment.PASS, "");
		cfg.setProperty(Environment.CACHE_REGION_PREFIX, "");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");

		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.CACHE_REGION_FACTORY, CoherenceRegionFactory.class.getName());
		cfg.setProperty("com.oracle.coherence.hibernate.cache.v53.v53.cache_config_file_path", "tests-expiring-hibernate-second-level-cache-config.xml");
	}

	@Test
	public void test_01_addExpiringItem() {
		final Statistics statistics = this.sessionFactory().getStatistics();

		final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) this.sessionFactory().getCache().getRegion("foo");
		final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(0);

		final Session session = openSession();
		session.beginTransaction();
		final Foo itemToSave = new Foo( "bar" );
		this.idOfSavedItem = (Long) session.save( itemToSave );
		session.flush();
		session.getTransaction().commit();

		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		assertThat(itemStatistics.getPutCount()).isEqualTo(1);
		assertThat(itemStatistics.getHitCount()).isEqualTo(0);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_02_retrieveUnExpiredItem() throws InterruptedException {
		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) this.sessionFactory().getCache().getRegion("foo");
		final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(1);

		final Session session = openSession();
		session.beginTransaction();
		final Foo foo = session.get(Foo.class, this.idOfSavedItem);
		session.getTransaction().commit();

		assertThat(foo).isNotNull();

		assertThat(itemStatistics.getPutCount()).isEqualTo(1);
		assertThat(itemStatistics.getHitCount()).isEqualTo(1);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);
	}

	@Test
	public void test_03_retrieveExpiredItem() throws InterruptedException {

		final Statistics statistics = this.sessionFactory().getStatistics();
		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("foo");

		final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) this.sessionFactory().getCache().getRegion("foo");
		final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		Thread.sleep(1500);

		assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(0);

		final Session session = openSession();
		session.beginTransaction();
		final Foo foo = session.get(Foo.class, this.idOfSavedItem);
		session.getTransaction().commit();

		assertThat(foo).isNotNull();

		assertThat(itemStatistics.getPutCount()).isEqualTo(2);
		assertThat(itemStatistics.getHitCount()).isEqualTo(1);
		assertThat(itemStatistics.getMissCount()).isEqualTo(1);

	}
}
