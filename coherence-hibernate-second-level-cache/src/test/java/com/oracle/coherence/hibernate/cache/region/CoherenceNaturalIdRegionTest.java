package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdReadOnlyCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdReadWriteCoherenceRegionAccessStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceNaturalIdRegionTest is test of CoherenceNaturalIdRegion behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceNaturalIdRegionTest
{


    // ---- Constants

    /**
     * The name of the CoherenceNaturalIdRegion used in tests.
     */
    private static final String REGION_NAME = "CoherenceNaturalIdRegionTest";


    // ---- Fields

    /**
     * The CoherenceNaturalIdRegion in the test fixture.
     */
    private CoherenceNaturalIdRegion coherenceNaturalIdRegion;


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUp()
    {
        //use a started CoherenceRegionFactory to build the CoherenceNaturalIdRegion in the test fixture, as a convenience
        //to ensure the cluster is joined and the cache factory is configured etc.
        Properties properties = new Properties();
        CoherenceRegionFactory coherenceRegionFactory = new CoherenceRegionFactory();
        coherenceRegionFactory.start(null, properties);

        CacheDataDescription cacheDataDescription = new CacheDataDescriptionImpl(true, false, null);
        coherenceNaturalIdRegion = (CoherenceNaturalIdRegion) coherenceRegionFactory.buildNaturalIdRegion(REGION_NAME, properties, cacheDataDescription);
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDown()
    {
        coherenceNaturalIdRegion.destroy();
        coherenceNaturalIdRegion = null;
    }


    // ---- Test cases

    /**
     * Tests CoherenceNaturalIdRegion.buildAccessStrategy() with AccessType.READ_ONLY.
     */
    @Test
    public void testBuildAccessStrategyReadOnly()
    {
        testBuildAccessStrategy(AccessType.READ_ONLY, NaturalIdReadOnlyCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceNaturalIdRegion.buildAccessStrategy() with AccessType.NONSTRICT_READ_WRITE.
     */
    @Test
    public void testBuildAccessStrategyNonstrictReadWrite()
    {
        testBuildAccessStrategy(AccessType.NONSTRICT_READ_WRITE, NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceNaturalIdRegion.buildAccessStrategy() with AccessType.READ_WRITE.
     */
    @Test
    public void testBuildAccessStrategyReadWrite()
    {
        testBuildAccessStrategy(AccessType.READ_WRITE, NaturalIdReadWriteCoherenceRegionAccessStrategy.class);
    }

    /**
     * Tests CoherenceNaturalIdRegion.buildAccessStrategy() with AccessType.TRANSACTIONAL.
     */
    @Test
    public void testBuildAccessStrategyTransactional()
    {
        try
        {
            coherenceNaturalIdRegion.buildAccessStrategy(AccessType.TRANSACTIONAL);
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
        NaturalIdRegionAccessStrategy strategy = coherenceNaturalIdRegion.buildAccessStrategy(accessType);
        assertTrue("Expect correct strategy type", expectedStrategyClass.isAssignableFrom(strategy.getClass()));
        assertEquals("Expect correct region", coherenceNaturalIdRegion, strategy.getRegion());
    }


}
