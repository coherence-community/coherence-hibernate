/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

/**
 * An EntityReadWriteCoherenceRegionAccessStrategyTest is a test of EntityReadWriteCoherenceRegionAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class EntityReadWriteCoherenceRegionAccessStrategyTest
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
        return newEntityReadWriteCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.getRegion().
     */
    @Test
    public void testGetRegion()
    {
        EntityRegion entityRegion = getEntityRegionAccessStrategy().getRegion();
        assertTrue("Expect instance of EntityRegion", entityRegion instanceof EntityRegion);
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.insert().
     */
    @Test
    public void testInsert()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testInsert";
        Object value = "testInsert";
        Object version = null;

        boolean cacheWasModified = accessStrategy.insert(key, value, version);
        assertFalse("Expect no cache modification from read-write access strategy insert", cacheWasModified);
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.afterInsert() when the entry being inserted is absent from the cache.
     */
    @Test
    public void testAfterInsertEntryAbsent()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testAfterInsertEntryAbsent";
        Object value = "testAfterInsertEntryAbsent";
        Object version = null;

        assertFalse("Expect cache to not contain entry initially", accessStrategy.getRegion().contains(key));

        boolean cacheWasModified = accessStrategy.afterInsert(key, value, version);
        assertTrue("Expect successful insertion if entry was absent", cacheWasModified);
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.afterInsert() when the entry being inserted is present in the cache.
     */
    @Test
    public void testAfterInsertEntryPresent()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testAfterInsertEntryPresent";
        Object value = "testAfterInsertEntryPresent";
        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        Object version = null;
        boolean minimalPutsInEffect = false;

        accessStrategy.putFromLoad(key, value, txTimestamp, version, minimalPutsInEffect);

        boolean cacheWasModified = accessStrategy.afterInsert(key, value, version);
        assertFalse("Expect no insertion if entry was present", cacheWasModified);
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.update().
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
        assertFalse("Expect no cache modification from read-write access strategy update", cacheWasModified);
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.afterUpdate() when the entry being updated is absent.
     */
    @Test
    public void testAfterUpdateEntryAbsent()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testAfterUpdateEntryAbsent";
        Object value = "testAfterUpdateEntryAbsent";
        Object currentVersion = null;
        Object previousVersion = null;
        SoftLock softLock = null;

        assertFalse("Expect empty cache", accessStrategy.getRegion().contains(key));

        boolean cacheWasModified = accessStrategy.afterUpdate(key, value, currentVersion, previousVersion, softLock);
        assertFalse("Expect no cache modification when entry absent", cacheWasModified);
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.afterUpdate() when the entry being updated is present and
     * not concurrently locked.
     */
    @Test
    public void testAfterUpdateEntryPresentNotConcurrentlyLocked()
    {
        EntityReadWriteCoherenceRegionAccessStrategy accessStrategy = (EntityReadWriteCoherenceRegionAccessStrategy) getEntityRegionAccessStrategy();

        Object key = "testAfterUpdateEntryPresentNotConcurrentlyLocked";
        Object value = "testAfterUpdateEntryPresentNotConcurrentlyLocked";
        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        Object version = null;
        boolean minimalPutsInEffect=false;

        accessStrategy.putFromLoad(key, value, txTimestamp, version, minimalPutsInEffect);
        SoftLock softLock = accessStrategy.lockItem(key, version);

        Object currentVersion = null;
        Object previousVersion = null;

        boolean cacheWasModified = accessStrategy.afterUpdate(key, value, currentVersion, previousVersion, softLock);
        assertTrue("Expect cache update when entry present and not concurrently locked", cacheWasModified);
        assertFalse("Expect value to be unlocked after update", accessStrategy.getCoherenceRegion().getValue(key).isSoftLocked());
    }

    /**
     * Tests EntityReadWriteCoherenceRegionAccessStrategy.afterUpdate() when the entry being updated is present and
     * not concurrently locked.
     */
    @Test
    public void testAfterUpdateEntryPresentConcurrentlyLocked()
    {
        EntityReadWriteCoherenceRegionAccessStrategy accessStrategy = (EntityReadWriteCoherenceRegionAccessStrategy) getEntityRegionAccessStrategy();

        Object key = "testAfterUpdateEntryPresentConcurrentlyLocked";
        Object value = "testAfterUpdateEntryPresentConcurrentlyLocked";
        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        Object version = null;
        boolean minimalPutsInEffect=false;

        accessStrategy.putFromLoad(key, value, txTimestamp, version, minimalPutsInEffect);
        SoftLock softLock = accessStrategy.lockItem(key, version);
        accessStrategy.lockItem(key, version);

        Object currentVersion = null;
        Object previousVersion = null;

        boolean cacheWasModified = accessStrategy.afterUpdate(key, value, currentVersion, previousVersion, softLock);
        assertFalse("Expect no cache update when entry present and concurrently locked", cacheWasModified);
        assertTrue("Expect value to still be locked after update", accessStrategy.getCoherenceRegion().getValue(key).isSoftLocked());
    }


}
