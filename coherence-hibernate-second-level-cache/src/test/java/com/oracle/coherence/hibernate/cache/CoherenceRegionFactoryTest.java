package com.oracle.coherence.hibernate.cache;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceEntityRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceQueryResultsRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceTimestampsRegion;
import com.tangosol.net.CacheFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.TransactionalDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;
import org.junit.After;
import org.junit.Before;
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
{


    // ---- Fields

    /**
     * The CoherenceRegionFactory in the test fixture.
     */
    private CoherenceRegionFactory coherenceRegionFactory;


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUp()
    {
        coherenceRegionFactory = new CoherenceRegionFactory();
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDown()
    {
        coherenceRegionFactory = null;
    }


    // ---- Test cases

    /**
     * Tests CoherenceRegionFactory.start() with no properties supplied.
     */
    @Test
    public void testStartWithNoProperties()
    {
        //Settings is a final class in the Hibernate codebase, so it's hard to create a Test Double.
        //And it takes a lot of context, i.e. a non-empty ServiceRegistry, to create one via SettingsFactory.buildSettings()
        //So for this test's purposes we'll pass null
        Settings settings = null;
        Properties properties = new Properties();
        coherenceRegionFactory.start(settings, properties);
        assertEquals("Expect cluster of one after start", 1, CacheFactory.getCluster().getMemberSet().size());
        assertNotNull("Expect non-null cache factory", coherenceRegionFactory.getConfigurableCacheFactory());
    }

    /**
     * Tests CoherenceRegionFactory.stop().
     */
    @Test
    public void testStop()
    {
        testStartWithNoProperties();
        coherenceRegionFactory.stop();
        assertNull("Expect null cache factory after stop", coherenceRegionFactory.getConfigurableCacheFactory());
    }

    /**
     * Tests CoherenceRegionFactory.isMinimalPutsEnabledByDefault().
     */
    @Test
    public void testMinimalPutsEnabledByDefault()
    {
        assertTrue("Expect minimal puts enabled by default", coherenceRegionFactory.isMinimalPutsEnabledByDefault());
    }

    /**
     * Tests CoherenceRegionFactory.getDefaultAccessType().
     */
    @Test
    public void testDefaultAccessType()
    {
        assertEquals("Expect default access type READ_WRITE", AccessType.READ_WRITE, coherenceRegionFactory.getDefaultAccessType());
    }

    /**
     * Tests CoherenceRegionFactory.nextTimestamp().
     */
    @Test
    public void testNextTimestamp()
    {
        long currentTime = coherenceRegionFactory.nextTimestamp();
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
                region = coherenceRegionFactory.buildEntityRegion(regionName, properties, cacheDataDescription);
                break;
            case "CoherenceNaturalIdRegion":
                region = coherenceRegionFactory.buildNaturalIdRegion(regionName, properties, cacheDataDescription);
                break;
            case "CoherenceCollectionRegion":
                region = coherenceRegionFactory.buildCollectionRegion(regionName, properties, cacheDataDescription);
                break;
            case "CoherenceQueryResultsRegion":
                region = coherenceRegionFactory.buildQueryResultsRegion(regionName, properties);
                break;
            case "CoherenceTimestampsRegion":
                region = coherenceRegionFactory.buildTimestampsRegion(regionName, properties);
                break;
        }

        assertTrue("Expect an instance of the correct CoherenceRegion subclass", coherenceRegionSubclass.isAssignableFrom(region.getClass()));
    }


}
