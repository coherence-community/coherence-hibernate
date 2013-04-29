package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityReadOnlyCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityReadWriteCoherenceRegionAccessStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceEntityRegionTest is test of CoherenceEntityRegion behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceEntityRegionTest
{


    // ---- Constants

    /**
     * The name of the CoherenceEntityRegion used in tests.
     */
    private static final String REGION_NAME = "CoherenceEntityRegionTest";


    // ---- Fields

    /**
     * The CoherenceEntityRegion in the test fixture.
     */
    private CoherenceEntityRegion coherenceEntityRegion;


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUp()
    {
        //use a started CoherenceRegionFactory to build the CoherenceEntityRegion in the test fixture, as a convenience
        //to ensure the cluster is joined and the cache factory is configured etc.
        Properties properties = new Properties();
        CoherenceRegionFactory coherenceRegionFactory = new CoherenceRegionFactory();
        coherenceRegionFactory.start(null, properties);

        CacheDataDescription cacheDataDescription = new CacheDataDescriptionImpl(true, false, null);
        coherenceEntityRegion = (CoherenceEntityRegion) coherenceRegionFactory.buildEntityRegion(REGION_NAME, properties, cacheDataDescription);
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDown()
    {
        coherenceEntityRegion.destroy();
        coherenceEntityRegion = null;
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
            coherenceEntityRegion.buildAccessStrategy(AccessType.TRANSACTIONAL);
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
        EntityRegionAccessStrategy strategy = coherenceEntityRegion.buildAccessStrategy(accessType);
        assertTrue("Expect correct strategy type", expectedStrategyClass.isAssignableFrom(strategy.getClass()));
        assertEquals("Expect correct region", coherenceEntityRegion, strategy.getRegion());
    }


}
