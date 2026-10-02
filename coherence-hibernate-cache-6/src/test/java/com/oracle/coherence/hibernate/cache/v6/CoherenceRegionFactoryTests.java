/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6;

import com.oracle.coherence.hibernate.cache.v6.access.CoherenceStorageAccess;
import com.tangosol.net.CacheFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.QueryResultsRegionTemplate;
import org.hibernate.cache.spi.support.TimestampsRegionTemplate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A CoherenceRegionFactoryTest is a test of CoherenceRegionFactory behavior.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoherenceRegionFactoryTests extends AbstractCoherenceRegionFactoryTests {

    /**
     * Tests CoherenceRegionFactory.start() with no properties supplied.
     */
    @Test
    @Order(1)
    public void testStartWithNoProperties() {
        setUpAbstractCoherenceRegionFactoryTest();
        assertEquals(1, CacheFactory.getCluster().getMemberSet().size(), "Expect cluster of one after start");
        assertNotNull(this.coherenceRegionFactory.getCoherenceSession(), "Expect non-null coherence session");
    }

    /**
     * Tests CoherenceRegionFactory.stop().
     */
    @Test
    @Order(2)
    public void testStop() {
        this.coherenceRegionFactory.stop();
        assertNull(getCoherenceRegionFactory().getCoherenceSession(), "Expect null cache coherence session after stop");
    }

    /**
     * Tests CoherenceRegionFactory.isMinimalPutsEnabledByDefault().
     */
    @Test
    @Order(3)
    public void testMinimalPutsEnabledByDefault() {
        assertTrue(getCoherenceRegionFactory().isMinimalPutsEnabledByDefault(), "Expect minimal puts enabled by default");
    }

    /**
     * Tests CoherenceRegionFactory.getDefaultAccessType().
     */
    @Test
    @Order(3)
    public void testDefaultAccessType() {
        assertEquals(AccessType.READ_WRITE, getCoherenceRegionFactory().getDefaultAccessType(), "Expect default access type READ_WRITE");
    }

    /**
     * Tests CoherenceRegionFactory.nextTimestamp().
     */
    @Test
    @Order(4)
    public void testNextTimestamp() {
        final long currentTime = this.coherenceRegionFactory.nextTimestamp();
        assertTrue(currentTime > 0, "Expect positive current time value");
        this.coherenceRegionFactory.stop();
    }

    /**
     * Tests CoherenceRegionFactory.buildQueryResultsRegion() with no properties.
     */
    @Test
    @Order(5)
    public void testBuildQueryResultsRegionWithNoProperties() {
        testStartWithNoProperties();
        final String regionName = "testBuild_CoherenceRegion_queryresults";

        final QueryResultsRegion queryResultsRegion = this.coherenceRegionFactory.buildQueryResultsRegion(
                regionName, getSessionFactoryImplementor(this.coherenceRegionFactory));
        final QueryResultsRegionTemplate region = (QueryResultsRegionTemplate) queryResultsRegion;

        assertTrue(region.getStorageAccess() instanceof CoherenceStorageAccess, "Expect an instance of the correct CoherenceRegion subclass");
        this.coherenceRegionFactory.stop();
    }

    /**
     * Tests CoherenceRegionFactory.buildTimestampsRegion() with no properties.
     */
    @Test
    @Order(6)
    public void testBuildTimestampsRegionWithNoProperties() {
        testStartWithNoProperties();
        final String regionName = "testBuild_CoherenceRegion_timestamps";
        final SessionFactoryImplementor sessionFactoryImplementor = getSessionFactoryImplementor(this.coherenceRegionFactory);

        final TimestampsRegion timestampsRegion = this.coherenceRegionFactory.buildTimestampsRegion(regionName, sessionFactoryImplementor);

        final TimestampsRegionTemplate region = (TimestampsRegionTemplate) timestampsRegion;
        assertTrue(region.getStorageAccess() instanceof CoherenceStorageAccess, "Expect an instance of the correct CoherenceRegion subclass");
        this.coherenceRegionFactory.stop();
    }

    private SessionFactoryImplementor getSessionFactoryImplementor(CoherenceRegionFactory coherenceRegionFactory) {
        final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

        final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder(bsr)
                .applySetting(AvailableSettings.CACHE_REGION_FACTORY, coherenceRegionFactory)
                .applySetting("hibernate.connection.url", "jdbc:hsqldb:mem:test")
                .build();

        final SessionFactoryImplementor sessionFactory;

        try {
            sessionFactory = (SessionFactoryImplementor) new MetadataSources(ssr)
                    //.addAnnotatedClass( TheEntity.class )
                    .buildMetadata()
                    .getSessionFactoryBuilder()
                    .build();
            return sessionFactory;
        }
        catch (Exception ex) {
            StandardServiceRegistryBuilder.destroy(ssr);
            throw ex;
        }
    }
}
