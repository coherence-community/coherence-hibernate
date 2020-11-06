/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * An EntityNonstrictReadWriteCoherenceRegionAccessStrategyTest is a test of
 * EntityNonstrictReadWriteCoherenceRegionAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class EntityNonstrictReadWriteCoherenceRegionAccessStrategyTest
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
        return newCoherenceEntityRegion();
    }

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy()
    {
        return newEntityNonstrictReadWriteCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests EntityNonstrictReadWriteCoherenceRegionAccessStrategy.getRegion().
     */
    @Test
    public void testGetRegion()
    {
        EntityRegion entityRegion = getEntityRegionAccessStrategy().getRegion();
        assertTrue("Expect instance of EntityRegion", entityRegion instanceof EntityRegion);
    }

    /**
     * Tests EntityNonstrictReadWriteCoherenceRegionAccessStrategy.insert().
     */
    @Test
    public void testInsert()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testInsert";
        Object value = "testInsert";
        Object version = null;

        boolean cacheWasModified = accessStrategy.insert(key, value, version);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy insert", cacheWasModified);
    }

    /**
     * Tests EntityNonstrictReadWriteCoherenceRegionAccessStrategy.afterInsert().
     */
    @Test
    public void testAfterInsert()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testAfterInsert";
        Object value = "testAfterInsert";
        Object version = null;

        boolean cacheWasModified = accessStrategy.afterInsert(key, value, version);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy afterInsert", cacheWasModified);
    }

    /**
     * Tests EntityNonstrictReadWriteCoherenceRegionAccessStrategy.update().
     */
    @Test
    public void testUpdate()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testUpdate";
        Object value = "testUpdate";
        Object currentVersion = null;
        Object previousVersion = null;

        boolean cacheWasModified = accessStrategy.update(key, value, currentVersion, previousVersion);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy update", cacheWasModified);
    }

    /**
     * Tests EntityNonstrictReadWriteCoherenceRegionAccessStrategy.afterUpdate().
     */
    @Test
    public void testAfterUpdate()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testAfterUpdate";
        Object value = "testAfterUpdate";
        Object currentVersion = null;
        Object previousVersion = null;
        SoftLock softLock = null;

        boolean cacheWasModified = accessStrategy.afterUpdate(key, value, currentVersion, previousVersion, softLock);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy afterUpdate", cacheWasModified);
        assertFalse("Expect cache not to contain key after afterUpdate", accessStrategy.getRegion().contains(key));
    }


}
