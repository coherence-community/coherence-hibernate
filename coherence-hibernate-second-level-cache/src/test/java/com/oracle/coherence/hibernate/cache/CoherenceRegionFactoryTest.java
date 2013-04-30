package com.oracle.coherence.hibernate.cache;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceEntityRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceQueryResultsRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceTimestampsRegion;
import com.tangosol.net.CacheFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.AccessType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * A CoherenceRegionFactoryTest is a test of CoherenceRegionFactory behavior.
 *
 * @author Randy Stafford
 */
@RunWith(JUnit4.class)
public class CoherenceRegionFactoryTest
extends AbstractCoherenceRegionFactoryTest
{


    // ---- Test cases

    /**
     * Tests CoherenceRegionFactory.start() with no properties supplied.
     */
    @Test
    public void testStartWithNoProperties()
    {
        //the CoherenceRegionFactory is started in AbstractCoherenceRegionFactoryTest.setup()
        assertEquals("Expect cluster of one after start", 1, CacheFactory.getCluster().getMemberSet().size());
        assertNotNull("Expect non-null cache factory", getCoherenceRegionFactory().getConfigurableCacheFactory());
    }

    /**
     * Tests CoherenceRegionFactory.stop().
     */
    @Test
    public void testStop()
    {
        getCoherenceRegionFactory().stop();
        assertNull("Expect null cache factory after stop", getCoherenceRegionFactory().getConfigurableCacheFactory());
    }

    /**
     * Tests CoherenceRegionFactory.isMinimalPutsEnabledByDefault().
     */
    @Test
    public void testMinimalPutsEnabledByDefault()
    {
        assertTrue("Expect minimal puts enabled by default", getCoherenceRegionFactory().isMinimalPutsEnabledByDefault());
    }

    /**
     * Tests CoherenceRegionFactory.getDefaultAccessType().
     */
    @Test
    public void testDefaultAccessType()
    {
        assertEquals("Expect default access type READ_WRITE", AccessType.READ_WRITE, getCoherenceRegionFactory().getDefaultAccessType());
    }

    /**
     * Tests CoherenceRegionFactory.nextTimestamp().
     */
    @Test
    public void testNextTimestamp()
    {
        long currentTime = getCoherenceRegionFactory().nextTimestamp();
        assertTrue("Expect positive current time value", currentTime > 0);
    }

    /**
     * Tests CoherenceRegionFactory.buildEntityRegion() with no properties.
     */
    @Test
    public void testBuildEntityRegionWithNoProperties()
    {
        testBuildRegionWithNoProperties(CoherenceEntityRegion.class);
    }

    /**
     * Tests CoherenceRegionFactory.buildNaturalIdRegion() with no properties.
     */
    @Test
    public void testBuildNaturalIdRegionWithNoProperties()
    {
        testBuildRegionWithNoProperties(CoherenceNaturalIdRegion.class);
    }

    /**
     * Tests CoherenceRegionFactory.buildCollectionRegion() with no properties.
     */
    @Test
    public void testBuildCollectionRegionWithNoProperties()
    {
        testBuildRegionWithNoProperties(CoherenceCollectionRegion.class);
    }

    /**
     * Tests CoherenceRegionFactory.buildQueryResultsRegion() with no properties.
     */
    @Test
    public void testBuildQueryResultsRegionWithNoProperties()
    {
        testBuildRegionWithNoProperties(CoherenceQueryResultsRegion.class);
    }

    /**
     * Tests CoherenceRegionFactory.buildTimestampsRegion() with no properties.
     */
    @Test
    public void testBuildTimestampsRegionWithNoProperties()
    {
        testBuildRegionWithNoProperties(CoherenceTimestampsRegion.class);
    }


    // ---- Internal

    /**
     * Tests a build*Region method of CoherenceRegionFactory corresponding to the argument class.
     *
     * @param coherenceRegionSubclass the implementation class of the type of region built in the test
     */
    public void testBuildRegionWithNoProperties(Class coherenceRegionSubclass)
    {
        //start the CoherenceRegionFactory in order to instantiate the CacheFactory used in buildEntityRegion()
        //the CacheFactory is instantiated in start() because it depends on the properties passed in there
        testStartWithNoProperties();

        String regionName = "testBuild" + coherenceRegionSubclass.getSimpleName();
        Properties properties = new Properties();
        CacheDataDescription cacheDataDescription = new CacheDataDescriptionImpl(true, false, null);
        Region region = null;
        switch (coherenceRegionSubclass.getSimpleName())
        {
            case "CoherenceEntityRegion":
                region = getCoherenceRegionFactory().buildEntityRegion(regionName, properties, cacheDataDescription);
                break;
            case "CoherenceNaturalIdRegion":
                region = getCoherenceRegionFactory().buildNaturalIdRegion(regionName, properties, cacheDataDescription);
                break;
            case "CoherenceCollectionRegion":
                region = getCoherenceRegionFactory().buildCollectionRegion(regionName, properties, cacheDataDescription);
                break;
            case "CoherenceQueryResultsRegion":
                region = getCoherenceRegionFactory().buildQueryResultsRegion(regionName, properties);
                break;
            case "CoherenceTimestampsRegion":
                region = getCoherenceRegionFactory().buildTimestampsRegion(regionName, properties);
                break;
        }

        assertTrue("Expect an instance of the correct CoherenceRegion subclass", coherenceRegionSubclass.isAssignableFrom(region.getClass()));
    }


}
