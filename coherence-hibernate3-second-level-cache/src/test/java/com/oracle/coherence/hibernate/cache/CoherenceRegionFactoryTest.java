/*
 * File: CoherenceRegionFactoryTest.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.hibernate.cache;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceEntityRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceQueryResultsRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceTimestampsRegion;
import com.tangosol.net.CacheFactory;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.Region;
import org.hibernate.cache.access.AccessType;
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
