/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access.processor;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import org.junit.AfterClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 */
public class AfterInsertProcessorTests {

	@AfterClass
	public static void after() {
		CacheFactory.shutdown();
	}

	@Test
	public void insertValue() throws ExecutionException, InterruptedException {

		final ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory("tests-hibernate-second-level-cache-config.xml",
				getClass().getClassLoader());

		final NamedCache<Long, CoherenceRegionValue> fooCache = factory.ensureCache("foo", null);

		assertThat(fooCache.size()).isEqualTo(0);

		final CoherenceRegionValue coherenceRegionValue = new CoherenceRegionValue("bar", 1,  Instant.now().toEpochMilli());
		final AfterInsertProcessor afterInsertProcessor = new AfterInsertProcessor(coherenceRegionValue);
		final Boolean result = fooCache.<Boolean>invoke(1L, afterInsertProcessor);
		assertThat(result).isTrue();
		assertThat(fooCache.size()).isEqualTo(1);
		assertThat(fooCache.get(1L)).isEqualTo(coherenceRegionValue);

		CacheFactory.shutdown();
	}
}
