/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6;

import com.oracle.coherence.hibernate.cache.v6.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v6.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v6.support.Book;
import com.tangosol.net.CacheFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.NaturalIdStatistics;
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
public class NaturalIdCacheTests extends BaseCoreFunctionalTestCase {

	private Long idOfBook1 = null;
	private Long idOfBook2 = null;
	private Long idOfBook3 = null;

	@AfterClass
	public static void after() {
		CacheFactory.shutdown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty(Environment.CACHE_REGION_PREFIX, "");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.CACHE_REGION_FACTORY, CoherenceRegionFactory.class.getName());
		cfg.setProperty("com.oracle.coherence.hibernate.cache.cache_config_file_path", "tests-hibernate-second-level-cache-config.xml");
	}

	@Test
	public void test_01_persistSeveralBooks() {
		final Statistics statistics = this.sessionFactory().getStatistics();

		CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) this.sessionFactory().getCache().getRegion("book");
		CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		Assertions.assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(0);

		final Session session = openSession();
		session.beginTransaction();

		final Book book1 = new Book(
				"The Sleepwalkers",
				"Christopher Clark",
				"0061146668");

		final Book book2 = new Book(
				"Dune",
				"Frank Herbert",
				"0441172717");

		final Book book3 = new Book(
				"Tropical Ecology",
				"John Kricher",
				"0691115133");

		this.idOfBook1 = (Long) session.save(book1);
		this.idOfBook2 = (Long) session.save(book2);
		this.idOfBook3 = (Long) session.save(book3);

		session.flush();
		session.getTransaction().commit();

		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("book");

		assertThat(itemStatistics.getPutCount()).isEqualTo(3);
		assertThat(itemStatistics.getHitCount()).isEqualTo(0);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);

		final NaturalIdStatistics naturalIdStatistics = statistics.getNaturalIdStatistics(Book.class.getName());
		assertThat(naturalIdStatistics.getCachePutCount()).isEqualTo(3);
		assertThat(naturalIdStatistics.getCacheHitCount()).isEqualTo(0);
		assertThat(naturalIdStatistics.getCacheMissCount()).isEqualTo(0);
	}

	@Test
	public void test_02_retrieveBookByNaturalId() {
		final Statistics statistics = this.sessionFactory().getStatistics();

		final Session session = openSession();
		session.beginTransaction();

		final Book book = session.bySimpleNaturalId(Book.class)
			.load("0061146668");

		assertThat(book.getId()).isSameAs(this.idOfBook1);

		final CacheRegionStatistics itemStatistics = statistics.getDomainDataRegionStatistics("book");

		assertThat(itemStatistics.getPutCount()).isEqualTo(3);
		assertThat(itemStatistics.getHitCount()).isEqualTo(1);
		assertThat(itemStatistics.getMissCount()).isEqualTo(0);

		final NaturalIdStatistics naturalIdStatistics = statistics.getNaturalIdStatistics(Book.class.getName());
		assertThat(naturalIdStatistics.getCachePutCount()).isEqualTo(3);
		assertThat(naturalIdStatistics.getCacheHitCount()).isEqualTo(1);
		assertThat(naturalIdStatistics.getCacheMissCount()).isEqualTo(0);
	}
}
