package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceGeneralDataRegionTest is test of CoherenceGeneralDataRegion behavior.  For convenience an instance
 * of a subclass (CoherenceTimestampsRegion) is the fixture used for testing the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceGeneralDataRegionTest
{


    // ---- Fields

    /**
     * The CoherenceTimestampsRegion in the test fixture.
     */
    private CoherenceTimestampsRegion coherenceTimestampsRegion;


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
        coherenceTimestampsRegion = (CoherenceTimestampsRegion) coherenceRegionFactory.buildTimestampsRegion(regionName, properties);
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDown()
    {
        coherenceTimestampsRegion.destroy();
        coherenceTimestampsRegion = null;
    }


    // ---- Test cases

    /**
     * Tests CoherenceGeneralDataRegion.put() and CoherenceGeneralDataRegion.get().
     */
    @Test
    public void testPutGet()
    {
        Object key = "testPutGet";
        Object objectPut = "testObject";
        coherenceTimestampsRegion.put(key, objectPut);
        Object objectGot = coherenceTimestampsRegion.get(key);
        assertEquals("Expect got same object as put", objectPut, objectGot);
    }


}
