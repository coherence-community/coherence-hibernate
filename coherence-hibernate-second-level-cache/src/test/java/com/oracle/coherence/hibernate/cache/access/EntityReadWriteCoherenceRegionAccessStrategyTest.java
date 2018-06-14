/*
 * File: EntityReadWriteCoherenceRegionAccessStrategyTest.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
    private SharedSessionContractImplementor implementor;


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

        boolean cacheWasModified = accessStrategy.insert(implementor, key, value, version);
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

        boolean cacheWasModified = accessStrategy.afterInsert(implementor, key, value, version);
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

        accessStrategy.putFromLoad(implementor, key, value, txTimestamp, version, minimalPutsInEffect);

        boolean cacheWasModified = accessStrategy.afterInsert(implementor, key, value, version);
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

        boolean cacheWasModified = accessStrategy.update(implementor, key, value, currentVersion, previousVersion);
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

        boolean cacheWasModified = accessStrategy.afterUpdate(implementor, key, value, currentVersion, previousVersion, softLock);
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

        accessStrategy.putFromLoad(implementor, key, value, txTimestamp, version, minimalPutsInEffect);
        SoftLock softLock = accessStrategy.lockItem(implementor, key, version);

        Object currentVersion = null;
        Object previousVersion = null;

        boolean cacheWasModified = accessStrategy.afterUpdate(implementor, key, value, currentVersion, previousVersion, softLock);
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

        accessStrategy.putFromLoad(implementor, key, value, txTimestamp, version, minimalPutsInEffect);
        SoftLock softLock = accessStrategy.lockItem(implementor, key, version);
        accessStrategy.lockItem(implementor, key, version);

        Object currentVersion = null;
        Object previousVersion = null;

        boolean cacheWasModified = accessStrategy.afterUpdate(implementor, key, value, currentVersion, previousVersion, softLock);
        assertFalse("Expect no cache update when entry present and concurrently locked", cacheWasModified);
        assertTrue("Expect value to still be locked after update", accessStrategy.getCoherenceRegion().getValue(key).isSoftLocked());
    }


}
