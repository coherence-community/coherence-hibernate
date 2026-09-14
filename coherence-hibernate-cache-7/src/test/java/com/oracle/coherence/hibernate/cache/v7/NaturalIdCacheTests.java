/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import com.oracle.coherence.hibernate.cache.v7.access.CoherenceDomainDataRegionImpl;
import com.oracle.coherence.hibernate.cache.v7.access.CoherenceStorageAccessImpl;
import com.oracle.coherence.hibernate.cache.v7.support.Book;
import com.tangosol.net.CacheFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.NaturalIdStatistics;
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
@DomainModel(annotatedClasses = Book.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NaturalIdCacheTests {

	private Long idOfBook1 = null;
	private Long idOfBook2 = null;
	private Long idOfBook3 = null;

	@AfterAll
	public static void after() {
		CacheFactory.shutdown();
	}

	@Test
	@Order(1)
	public void persistSeveralBooks(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();

		final CoherenceDomainDataRegionImpl region = (CoherenceDomainDataRegionImpl) scope.getSessionFactory().getCache().getRegion("book");
		final CoherenceStorageAccessImpl coherenceStorageAccess = (CoherenceStorageAccessImpl) region.getCacheStorageAccess();

		Assertions.assertThat(coherenceStorageAccess.getDelegate().getElementCountInMemory()).isEqualTo(0);

		final Session session = scope.getSessionFactory().openSession();
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

		session.persist(book1);
		session.persist(book2);
		session.persist(book3);
		this.idOfBook1 = book1.getId();
		this.idOfBook2 = book2.getId();
		this.idOfBook3 = book3.getId();

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
	@Order(2)
	public void retrieveBookByNaturalId(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();

		final Session session = scope.getSessionFactory().openSession();
		session.beginTransaction();

		final Book book = session.byNaturalId(Book.class)
				.using("isbn10", "0061146668")
				.load();

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
