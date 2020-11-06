/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadOnlyCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadWriteCoherenceRegionAccessStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceCollectionRegionTest is test of CoherenceCollectionRegion behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceCollectionRegionTest
extends AbstractCoherenceRegionTest
{


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegion newCoherenceRegion()
    {
        return newCoherenceCollectionRegion();
    }


    // ---- Test cases

    /**
     * Tests CoherenceCollectionRegion.buildAccessStrategy() with AccessType.READ_ONLY.
     */
    @Test
    public void testBuildAccessStrategyReadOnly()
    {
        testBuildAccessStrategy(AccessType.READ_ONLY, CollectionReadOnlyCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceCollectionRegion.buildAccessStrategy() with AccessType.NONSTRICT_READ_WRITE.
     */
    @Test
    public void testBuildAccessStrategyNonstrictReadWrite()
    {
        testBuildAccessStrategy(AccessType.NONSTRICT_READ_WRITE, CollectionNonstrictReadWriteCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceCollectionRegion.buildAccessStrategy() with AccessType.READ_WRITE.
     */
    @Test
    public void testBuildAccessStrategyReadWrite()
    {
        testBuildAccessStrategy(AccessType.READ_WRITE, CollectionReadWriteCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceCollectionRegion.buildAccessStrategy() with AccessType.TRANSACTIONAL.
     */
    @Test
    public void testBuildAccessStrategyTransactional()
    {
        try
        {
            getCoherenceCollectionRegion().buildAccessStrategy(AccessType.TRANSACTIONAL);
            fail("Expect CacheException building AccessType.TRANSACTIONAL strategy");
        }
        catch (CacheException ex)
        {
            assertEquals("Expect correct exception message", CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE, ex.getMessage());
        }
    }


    // ---- Internal

    protected void testBuildAccessStrategy(AccessType accessType, Class expectedStrategyClass)
    {
        CoherenceCollectionRegion coherenceCollectionRegion = getCoherenceCollectionRegion();
        CollectionRegionAccessStrategy strategy = coherenceCollectionRegion.buildAccessStrategy(accessType);
        assertTrue("Expect correct strategy type", expectedStrategyClass.isAssignableFrom(strategy.getClass()));
        assertEquals("Expect correct region", coherenceCollectionRegion, strategy.getRegion());
    }


}
