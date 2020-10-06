/*
 * File: NaturalIdReadOnlyCoherenceRegionAccessStrategyTest.java
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
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
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

    private SessionImplementor sessionImplementor;

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

        boolean cacheWasModified = accessStrategy.insert(sessionImplementor, key, value);
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

        boolean cacheWasModified = accessStrategy.afterInsert(sessionImplementor, key, value);
        assertTrue("Expect cache modification from read-only access strategy afterInsert", cacheWasModified);
        assertTrue("Expect cache to contain key after afterInsert", accessStrategy.getRegion().contains(key));
        assertEquals("Expect to get same value put", value, accessStrategy.get(sessionImplementor, key, System.currentTimeMillis()));
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

            accessStrategy.update(sessionImplementor, key, value);
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

            accessStrategy.afterUpdate(sessionImplementor, key, value, softLock);
            fail("Expect CacheException updating read-only access strategy");
        }
        catch (UnsupportedOperationException ex)
        {
            assertEquals("Expect writes not supported message", CoherenceRegionAccessStrategy.WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE, ex.getMessage());
        }
    }


}
