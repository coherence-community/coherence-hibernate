/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53;

import com.oracle.coherence.hibernate.cache.v53.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.v53.access.CoherenceStorageAccess;
import com.tangosol.net.CacheFactory;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.TimestampsRegionTemplate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * A CoherenceRegionFactoryTest is a test of CoherenceRegionFactory behavior.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
@RunWith(JUnit4.class)
public class CoherenceRegionFactoryTest
extends AbstractCoherenceRegionFactoryTest
{
    @AfterClass
    public static void after() {
        CacheFactory.shutdown();
    }

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
     * Tests CoherenceRegionFactory.buildQueryResultsRegion() with no properties.
     */
//    @Test
//    public void testBuildQueryResultsRegionWithNoProperties()
//    {
//        //start the CoherenceRegionFactory in order to instantiate the CacheFactory used in buildEntityRegion()
//        //the CacheFactory is instantiated in start() because it depends on the properties passed in there
//        testStartWithNoProperties();
//
//        Region region = region = getCoherenceRegionFactory().buildDomainDataRegion(
//                        new DomainDataRegionConfigImpl.Builder("foo").build(), null);
//
//        //assertTrue("Expect an instance of the correct CoherenceRegion subclass", coherenceRegionSubclass.isAssignableFrom(region.getClass()));
//    }

    /**
     * Tests CoherenceRegionFactory.buildTimestampsRegion() with no properties.
     */
    @Test
    public void testBuildTimestampsRegionWithNoProperties()
    {
        testStartWithNoProperties();
        String regionName = "testBuild_CoherenceRegion_timestamps";
        final TimestampsRegionTemplate region = (TimestampsRegionTemplate) super.coherenceRegionFactory.buildTimestampsRegion(regionName, getSessionFactoryImplmentor());
        assertTrue("Expect an instance of the correct CoherenceRegion subclass", region.getStorageAccess() instanceof CoherenceStorageAccess);

    }

    private SessionFactoryImplementor getSessionFactoryImplmentor() {
        BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

        final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
                .applySetting(AvailableSettings.CACHE_REGION_FACTORY, CoherenceRegionFactory.class.getName())
                .applySetting("hibernate.connection.url", "jdbc:hsqldb:mem:test")
                .build();

        final SessionFactoryImplementor sessionFactory;

        try {
            sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
                    //.addAnnotatedClass( TheEntity.class )
                    .buildMetadata()
                    .getSessionFactoryBuilder()
                    .build();
            return sessionFactory;
        }
        catch ( Exception e ) {
            StandardServiceRegistryBuilder.destroy( ssr );
            throw e;
        }
    }
}
