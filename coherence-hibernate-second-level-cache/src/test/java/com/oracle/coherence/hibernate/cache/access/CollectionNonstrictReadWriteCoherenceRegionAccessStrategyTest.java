package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.CollectionRegion;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A CollectionNonstrictReadWriteCoherenceRegionAccessStrategyTest is a test of
 * CollectionNonstrictReadWriteCoherenceRegionAccessStrategy behavior.
 *
 * @author Randy Stafford
 */
public class CollectionNonstrictReadWriteCoherenceRegionAccessStrategyTest
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
        return newCollectionNonstrictReadWriteCoherenceRegionAccessStrategy();
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
