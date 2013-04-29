package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * A CoherenceTransactionalDataRegionTest is test of CoherenceTransactionalDataRegion behavior.  For convenience an
 * instance of a subclass (CoherenceEntityRegion) is the fixture used for testing the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceTransactionalDataRegionTest
{


    // ---- Constants

    /**
     * The CacheDataDescription used in tests.
     */
    private static final CacheDataDescription CACHE_DATA_DESCRIPTION = new CacheDataDescriptionImpl(true, false, null);


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
        //use a started CoherenceRegionFactory to build the CoherenceTimestampsRegion in the test fixture, as a convenience
        //to ensure the cluster is joined and the cache factory is configured etc.
        Properties properties = new Properties();
        CoherenceRegionFactory coherenceRegionFactory = new CoherenceRegionFactory();
        coherenceRegionFactory.start(null, properties);

        String regionName = "CoherenceGeneralDataRegionTest";
        coherenceEntityRegion = (CoherenceEntityRegion) coherenceRegionFactory.buildEntityRegion(regionName, properties, CACHE_DATA_DESCRIPTION);
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
     * Tests CoherenceTransactionalDataRegion.isTransactionAware().
     */
    @Test
    public void testIsTransactionAware()
    {
        assertFalse("Expect not transaction aware", coherenceEntityRegion.isTransactionAware());
    }

    /**
     * Tests CoherenceTransactionalDataRegion.getCacheDataDescription().
     */
    @Test
    public void testGetCacheDataDescription()
    {
        assertEquals("Expect same CacheDataDescription", CACHE_DATA_DESCRIPTION, coherenceEntityRegion.getCacheDataDescription());
    }


}
