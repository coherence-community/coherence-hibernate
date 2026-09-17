/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that high timestamp throughput does not advance the clock ahead of wall time.
 */
public class CoherenceTimestampTests {

    @Test
    public void timestampBurstStaysWithinWallClockBounds() {
        final CoherenceRegionFactory factory = new CoherenceRegionFactory();
        final long before = System.currentTimeMillis();
        long timestamp = factory.nextTimestamp();
        for (int i = 0; i < 100_000; i++) {
            final long next = factory.nextTimestamp();
            assertThat(next).isGreaterThan(timestamp);
            timestamp = next;
        }
        final long after = System.currentTimeMillis();

        assertThat(timestamp / SimpleTimestamper.ONE_MS).isBetween(before, after);
        assertThat(factory.getTimeout()).isEqualTo(60_000L * SimpleTimestamper.ONE_MS);
    }
}
