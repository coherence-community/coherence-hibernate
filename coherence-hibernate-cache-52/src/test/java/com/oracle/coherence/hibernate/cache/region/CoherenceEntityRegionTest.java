package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityReadOnlyCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityReadWriteCoherenceRegionAccessStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceEntityRegionTest is test of CoherenceEntityRegion behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceEntityRegionTest
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
        return newCoherenceEntityRegion();
    }


    // ---- Test cases

    /**
     * Tests CoherenceEntityRegion.buildAccessStrategy() with AccessType.READ_ONLY.
     */
    @Test
    public void testBuildAccessStrategyReadOnly()
    {
        testBuildAccessStrategy(AccessType.READ_ONLY, EntityReadOnlyCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceEntityRegion.buildAccessStrategy() with AccessType.NONSTRICT_READ_WRITE.
     */
    @Test
    public void testBuildAccessStrategyNonstrictReadWrite()
    {
        testBuildAccessStrategy(AccessType.NONSTRICT_READ_WRITE, EntityNonstrictReadWriteCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceEntityRegion.buildAccessStrategy() with AccessType.READ_WRITE.
     */
    @Test
    public void testBuildAccessStrategyReadWrite()
    {
        testBuildAccessStrategy(AccessType.READ_WRITE, EntityReadWriteCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceEntityRegion.buildAccessStrategy() with AccessType.TRANSACTIONAL.
     */
    @Test
    public void testBuildAccessStrategyTransactional()
    {
        try
        {
            getCoherenceEntityRegion().buildAccessStrategy(AccessType.TRANSACTIONAL);
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
        CoherenceEntityRegion coherenceEntityRegion = getCoherenceEntityRegion();
        EntityRegionAccessStrategy strategy = coherenceEntityRegion.buildAccessStrategy(accessType);
        assertTrue("Expect correct strategy type", expectedStrategyClass.isAssignableFrom(strategy.getClass()));
        assertEquals("Expect correct region", coherenceEntityRegion, strategy.getRegion());
    }


}
