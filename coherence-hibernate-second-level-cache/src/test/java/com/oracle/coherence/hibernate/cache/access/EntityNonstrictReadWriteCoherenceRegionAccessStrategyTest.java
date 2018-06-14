/*
 * File: EntityNonstrictReadWriteCoherenceRegionAccessStrategy.java
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

/**
 * An EntityNonstrictReadWriteCoherenceRegionAccessStrategyTest is a test of
 * EntityNonstrictReadWriteCoherenceRegionAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class EntityNonstrictReadWriteCoherenceRegionAccessStrategyTest
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

        boolean cacheWasModified = accessStrategy.insert(implementor,key, value, version);
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

        boolean cacheWasModified = accessStrategy.afterInsert(implementor, key, value, version);
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

        boolean cacheWasModified = accessStrategy.update(implementor, key, value, currentVersion, previousVersion);
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

        boolean cacheWasModified = accessStrategy.afterUpdate(implementor, key, value, currentVersion, previousVersion, softLock);
        assertFalse("Expect no cache modification from nonstrict read-write access strategy afterUpdate", cacheWasModified);
        assertFalse("Expect cache not to contain key after afterUpdate", accessStrategy.getRegion().contains(key));
    }


}
