/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A NaturalIdReadOnlyCoherenceRegionAccessStrategyTest is a test of NaturalIdReadOnlyCoherenceRegionAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class NaturalIdReadOnlyCoherenceRegionAccessStrategyTest
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
        return newNaturalIdReadOnlyCoherenceRegionAccessStrategy();
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
        assertFalse("Expect no cache modification from read-only access strategy insert", cacheWasModified);
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
        assertTrue("Expect cache modification from read-only access strategy afterInsert", cacheWasModified);
        assertTrue("Expect cache to contain key after afterInsert", accessStrategy.getRegion().contains(key));
        assertEquals("Expect to get same value put", value, accessStrategy.get(key, System.currentTimeMillis()));
    }

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.update().
     */
    @Test
    public void testUpdate()
    {
        try
        {
            NaturalIdRegionAccessStrategy accessStrategy = getNaturalIdRegionAccessStrategy();

            Object key = "testUpdate";
            Object value = "testUpdate";

            accessStrategy.update(key, value);
            fail("Expect CacheException updating read-only access strategy");
        }
        catch (UnsupportedOperationException ex)
        {
            assertEquals("Expect writes not supported message", CoherenceRegionAccessStrategy.WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE, ex.getMessage());
        }
    }

    /**
     * Tests NaturalIdReadOnlyCoherenceRegionAccessStrategy.afterUpdate().
     */
    @Test
    public void testAfterUpdate()
    {
        try
        {
            NaturalIdRegionAccessStrategy accessStrategy = getNaturalIdRegionAccessStrategy();

            Object key = "testAfterUpdate";
            Object value = "testAfterUpdate";
            SoftLock softLock = null;

            accessStrategy.afterUpdate(key, value, softLock);
            fail("Expect CacheException updating read-only access strategy");
        }
        catch (UnsupportedOperationException ex)
        {
            assertEquals("Expect writes not supported message", CoherenceRegionAccessStrategy.WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE, ex.getMessage());
        }
    }


}
