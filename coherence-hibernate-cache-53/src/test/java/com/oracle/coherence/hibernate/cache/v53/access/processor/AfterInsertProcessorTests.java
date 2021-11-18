/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access.processor;

import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.AfterClass;
import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

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

		// Create the Coherence instance from the configuration
		final Coherence coherence = Coherence.clusterMember();
		coherence.start().get();

		final Session coherenceSession = coherence.getSession();
		final NamedCache<Long, CoherenceRegionValue> fooCache = coherenceSession.getCache("foo");

		assertThat(fooCache.size()).isEqualTo(0);

		final CoherenceRegionValue coherenceRegionValue = new CoherenceRegionValue("bar", 1,  Instant.now().toEpochMilli());
		final AfterInsertProcessor afterInsertProcessor = new AfterInsertProcessor(coherenceRegionValue);
		Boolean result = fooCache.<Boolean>invoke(1L, afterInsertProcessor);
		assertThat(result).isTrue();
		assertThat(fooCache.size()).isEqualTo(1);
		assertThat(fooCache.get(1L)).isEqualTo(coherenceRegionValue);

		coherence.close();
	}
}
