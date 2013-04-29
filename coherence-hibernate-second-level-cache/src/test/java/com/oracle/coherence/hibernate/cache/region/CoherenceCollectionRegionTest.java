package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadOnlyCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadWriteCoherenceRegionAccessStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceCollectionRegionTest is test of CoherenceCollectionRegion behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceCollectionRegionTest
{


    // ---- Constants

    /**
     * The name of the CoherenceCollectionRegion used in tests.
     */
    private static final String REGION_NAME = "CoherenceCollectionRegionTest";


    // ---- Fields

    /**
     * The CoherenceCollectionRegion in the test fixture.
     */
    private CoherenceCollectionRegion coherenceCollectionRegion;


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUp()
    {
        //use a started CoherenceRegionFactory to build the CoherenceCollectionRegion in the test fixture, as a convenience
        //to ensure the cluster is joined and the cache factory is configured etc.
        Properties properties = new Properties();
        CoherenceRegionFactory coherenceRegionFactory = new CoherenceRegionFactory();
        coherenceRegionFactory.start(null, properties);

        CacheDataDescription cacheDataDescription = new CacheDataDescriptionImpl(true, false, null);
        coherenceCollectionRegion = (CoherenceCollectionRegion) coherenceRegionFactory.buildCollectionRegion(REGION_NAME, properties, cacheDataDescription);
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDown()
    {
        coherenceCollectionRegion.destroy();
        coherenceCollectionRegion = null;
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
            coherenceCollectionRegion.buildAccessStrategy(AccessType.TRANSACTIONAL);
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
        CollectionRegionAccessStrategy strategy = coherenceCollectionRegion.buildAccessStrategy(accessType);
        assertTrue("Expect correct strategy type", expectedStrategyClass.isAssignableFrom(strategy.getClass()));
        assertEquals("Expect correct region", coherenceCollectionRegion, strategy.getRegion());
    }


}
