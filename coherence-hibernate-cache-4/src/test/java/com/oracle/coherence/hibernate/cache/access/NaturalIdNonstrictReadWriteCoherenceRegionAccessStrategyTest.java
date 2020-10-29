/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategyTest is a test of
 * NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategyTest
extends AbstractCoherenceRegionAccessStrategyTest
{


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegion newCoherenceRegion()
    {
        return newCoherenceNaturalIdRegion();
    }

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy()
    {
        return newNaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.getRegion().
     */
    @Test
    public void testGetRegion()
    {
        NaturalIdRegion naturalIdRegion = getNaturalIdRegionAccessStrategy().getRegion();
        assertTrue("Expect instance of NaturalIdRegion", naturalIdRegion instanceof NaturalIdRegion);
    }

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.insert().
     */
    @Test
    public void testInsert()
    {
        NaturalIdRegionAccessStrategy accessStrategy = getNaturalIdRegionAccessStrategy();

        Object key = "testInsert";
        Object value = "testInsert";

        boolean cacheWasModified = accessStrategy.insert(key, value);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy insert", cacheWasModified);
    }

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.afterInsert().
     */
    @Test
    public void testAfterInsert()
    {
        NaturalIdRegionAccessStrategy accessStrategy = getNaturalIdRegionAccessStrategy();

        Object key = "testAfterInsert";
        Object value = "testAfterInsert";

        boolean cacheWasModified = accessStrategy.afterInsert(key, value);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy afterInsert", cacheWasModified);
    }

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.update().
     */
    @Test
    public void testUpdate()
    {
        NaturalIdRegionAccessStrategy accessStrategy = getNaturalIdRegionAccessStrategy();

        Object key = "testUpdate";
        Object value = "testUpdate";

        boolean cacheWasModified = accessStrategy.insert(key, value);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy update", cacheWasModified);
    }

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.afterUpdate().
     */
    @Test
    public void testAfterUpdate()
    {
        NaturalIdRegionAccessStrategy accessStrategy = getNaturalIdRegionAccessStrategy();

        Object key = "testAfterUpdate";
        Object value = "testAfterUpdate";
        SoftLock softLock = null;

        boolean cacheWasModified = accessStrategy.afterUpdate(key, value, softLock);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy afterUpdate", cacheWasModified);
        assertFalse("Expect cache not to contain key after afterUpdate", accessStrategy.getRegion().contains(key));
    }


}
