/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.CollectionRegion;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * A CollectionReadWriteCohrerenceRegionAccessStrategyTest is a test of CollectionReadWriteCohrerenceRegionAccessStrategy
 * behavior.
 *
 * @author Randy Stafford
 */
public class CollectionReadWriteCohrerenceRegionAccessStrategyTest
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
        return newCoherenceCollectionRegion();
    }

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy()
    {
        return newCollectionReadWriteCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests EntityReadOnlyCoherenceRegionAccessStrategy.getRegion().
     */
    @Test
    public void testGetRegion()
    {
        CollectionRegion collectionRegion = getCollectionRegionAccessStrategy().getRegion();
        assertTrue("Expect instance of EntityRegion", collectionRegion instanceof CollectionRegion);
    }


}
