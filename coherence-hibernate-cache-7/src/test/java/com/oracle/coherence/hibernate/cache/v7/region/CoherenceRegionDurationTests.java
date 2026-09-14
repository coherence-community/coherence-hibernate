/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7.region;

import java.lang.reflect.Proxy;
import java.util.Map;

import com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory;
import com.tangosol.net.NamedCache;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Coherence region duration-property parsing and defaults.
 */
public class CoherenceRegionDurationTests {

    @Test
    public void absentDurationUsesDefault() {
        final CoherenceRegion region = createRegion(Map.of());

        assertThat(region.getTimeout()).isEqualTo((long) CoherenceRegion.DEFAULT_LOCK_LEASE_DURATION * SimpleTimestamper.ONE_MS);
    }

    @Test
    public void validDurationIsParsed() {
        final CoherenceRegion region = createRegion(Map.of(
                CoherenceRegion.LOCK_LEASE_DURATION_PROPERTY_NAME, "2s"));

        assertThat(region.getTimeout()).isEqualTo(2_000L * SimpleTimestamper.ONE_MS);
    }

    @Test
    public void malformedDurationUsesDefault() {
        final CoherenceRegion region = createRegion(Map.of(
                CoherenceRegion.LOCK_LEASE_DURATION_PROPERTY_NAME, "not-a-duration"));

        assertThat(region.getTimeout()).isEqualTo((long) CoherenceRegion.DEFAULT_LOCK_LEASE_DURATION * SimpleTimestamper.ONE_MS);
    }

    @Test
    public void maximumDurationConversionDoesNotOverflow() {
        final CoherenceRegion region = createRegion(Map.of(
                CoherenceRegion.LOCK_LEASE_DURATION_PROPERTY_NAME, Integer.MAX_VALUE + "ms"));

        assertThat(region.getTimeout()).isEqualTo((long) Integer.MAX_VALUE * SimpleTimestamper.ONE_MS);
    }

    @Test
    public void softLockExpirationUsesTimestampUnits() {
        final CoherenceRegionFactory factory = new CoherenceRegionFactory();
        final CoherenceRegion region = new CoherenceRegion(factory, noOpProxy(NamedCache.class), Map.of(
                CoherenceRegion.LOCK_LEASE_DURATION_PROPERTY_NAME, "2s"));
        final long before = factory.nextTimestamp();
        final long expiration = region.newSoftLockExpirationTime();
        final long after = factory.nextTimestamp();

        assertThat(expiration).isBetween(before + 2_000L * SimpleTimestamper.ONE_MS,
                after + 2_000L * SimpleTimestamper.ONE_MS);
        assertThat(factory.getTimeout()).isEqualTo(60_000L * SimpleTimestamper.ONE_MS);
    }

    @Test
    public void durationOverMaximumIsCapped() {
        final CoherenceRegion region = createRegion(Map.of());

        assertThat(region.getDurationProperty(
                Map.of(CoherenceRegion.LOCK_LEASE_DURATION_PROPERTY_NAME, "2s"),
                CoherenceRegion.LOCK_LEASE_DURATION_PROPERTY_NAME,
                500,
                1_000))
                .isEqualTo(1_000);
    }

    private CoherenceRegion createRegion(Map<String, Object> properties) {
        return new CoherenceRegion(
                noOpProxy(RegionFactory.class),
                noOpProxy(NamedCache.class),
                properties);
    }

    private <T> T noOpProxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] {type},
                (proxy, method, arguments) -> null));
    }
}
