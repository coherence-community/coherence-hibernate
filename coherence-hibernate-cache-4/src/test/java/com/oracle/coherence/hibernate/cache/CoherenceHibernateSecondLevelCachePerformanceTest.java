/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache;

import com.oracle.tools.runtime.coherence.Cluster;
import com.oracle.tools.runtime.coherence.ClusterBuilder;
import com.oracle.tools.runtime.coherence.ClusterMember;
import com.oracle.tools.runtime.coherence.ClusterMemberSchema;
import com.oracle.tools.runtime.console.FileWriterApplicationConsole;
import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.NativeJavaApplicationBuilder;
import com.oracle.tools.runtime.java.SimpleJavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplicationSchema;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tutorial.EventManager;
import org.hibernate.tutorial.domain.Event;
import org.hibernate.tutorial.domain.Person;
import org.hibernate.tutorial.util.HibernateUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * A CoherenceHibernateSecondLevelCachePerformanceTest is a performance test of the Coherence-based implementation
 * of the Hibernate second-level cache SPI.  The test strategy is to measure and compare the response time of various
 * Hibernate API calls under various configurations: no second-level caching, Coherence second-level caching,
 * and EhCache second-level caching.
 *
 * @author Randy Stafford
 */
//@RunWith(JUnit4.class)
public class CoherenceHibernateSecondLevelCachePerformanceTest
{


    // ---- Constants

    /**
     * The path of the Coherence cache configuration file used in the test fixture.
     */
    private static String CACHE_CONFIG_FILE_PATH = "test-hibernate-second-level-cache-config.xml";

    /**
     * The Coherence WKA host used in the test fixture.
     */
    private static String WKA_HOST = "localhost";

    /**
     * The Coherence WKA port used in the test fixture.
     */
    private static String WKA_PORT = "8088";


    // ---- Fields

    /**
     * The Coherence cluster in the test fixture.
     */
    private Cluster cluster;

    /**
     * The hsqldb process in the test fixture.
     */
    private SimpleJavaApplication hsqldbProcess;

    /**
     * The Hibernate SessionFactory in the test fixture.
     */
    private SessionFactory sessionFactory;


    // ---- Test cases

    /**
     * Measures the latency of Session.get() without second-level caching.
     */
    //@Test
    public void measureGetLatencyWithoutCaching()
    throws InterruptedException, IOException
    {
        //setup
        startDatabase();
        Thread.currentThread().sleep(5 * 1000);
        //setup

        Long personId = createAndStorePerson();
        long cumulativeLatency = 0;

        for (int i=1; i <= 1000; i++)
        {
            Session session = newSession("hibernate.cfg.nocache.xml");
            session.beginTransaction();
            long clockBefore = System.nanoTime();
            session.get(Person.class, personId);
            cumulativeLatency += System.nanoTime() - clockBefore;
            session.getTransaction().commit();
            session.close();
        }

        System.out.println("Average Session.get() latency without second-level caching: " + cumulativeLatency / 1000L + " nanoseconds");

        //teardown
        hsqldbProcess.destroy();
        //teardown
    }

    /**
     * Measures the latency of Session.get() with Coherence second-level caching.
     */
    //@Test
    public void measureGetLatencyWithCoherenceCaching()
    throws InterruptedException, IOException
    {
        //setup
        startDatabase();
        startCacheServer(CACHE_CONFIG_FILE_PATH);
        Thread.currentThread().sleep(5 * 1000);
        joinCluster(CACHE_CONFIG_FILE_PATH);
        //setup

        Long personId = createAndStorePerson();

        //Prime the cache
        Session session = newSession("hibernate.cfg.cohcache.xml");
        session.get(Person.class, personId);

        long cumulativeLatency = 0;

        for (int i=1; i <= 1000; i++)
        {
            session = newSession("hibernate.cfg.cohcache.xml");
            session.beginTransaction();
            long clockBefore = System.nanoTime();
            session.get(Person.class, personId);
            cumulativeLatency += System.nanoTime() - clockBefore;
            session.getTransaction().commit();
            session.close();
        }

        System.out.println("Average Session.get() latency with Coherence second-level caching: " + cumulativeLatency / 1000L + " nanoseconds");

        //teardown
        leaveCluster();
        cluster.destroy();
        hsqldbProcess.destroy();
        //teardown
    }

    /**
     * Measures the latency of Session.get() with Coherence near caching.
     */
    //@Test
    public void measureGetLatencyWithNearCaching()
            throws InterruptedException, IOException
    {
        //setup
        startDatabase();
        startCacheServer("near-hibernate-second-level-cache-config.xml");
        Thread.currentThread().sleep(5 * 1000);
        joinCluster("near-hibernate-second-level-cache-config.xml");
        //setup

        Long personId = createAndStorePerson();

        //Prime the cache
        Session session = newSession("hibernate.cfg.nearcache.xml");
        session.get(Person.class, personId);

        long cumulativeLatency = 0;

        for (int i=1; i <= 1000; i++)
        {
            session = newSession("hibernate.cfg.nearcache.xml");
            session.beginTransaction();
            long clockBefore = System.nanoTime();
            session.get(Person.class, personId);
            cumulativeLatency += System.nanoTime() - clockBefore;
            session.getTransaction().commit();
            session.close();
        }

        System.out.println("Average Session.get() latency with Coherence near caching: " + cumulativeLatency / 1000L + " nanoseconds");

        //teardown
        leaveCluster();
        cluster.destroy();
        hsqldbProcess.destroy();
        //teardown
    }


    /**
     * Measures the latency of a query without second-level caching.
     */
    //@Test
    public void measureQueryLatencyWithoutCaching()
            throws InterruptedException, IOException
    {
        //setup
        startDatabase();
        Thread.currentThread().sleep(5 * 1000);
        //setup

        for (int i=1; i<=100; i++) createAndStorePerson();

        long cumulativeLatency = 0;

        for (int i=1; i <= 1000; i++)
        {
            Session session = newSession("hibernate.cfg.nocache.xml");
            session.beginTransaction();
            long clockBefore = System.nanoTime();
            Query query = session.createQuery("from Person");
            query.list();
            cumulativeLatency += System.nanoTime() - clockBefore;
            session.getTransaction().commit();
            session.close();
        }

        System.out.println("Average Query.list() latency without query caching: " + cumulativeLatency / 1000L + " nanoseconds");

        //teardown
        hsqldbProcess.destroy();
        //teardown
    }

    /**
     * Measures the latency of a query with Coherence second-level caching.
     */
    //@Test
    public void measureQueryLatencyWithCoherenceCaching()
            throws InterruptedException, IOException
    {
        //setup
        startDatabase();
        startCacheServer(CACHE_CONFIG_FILE_PATH);
        Thread.currentThread().sleep(5 * 1000);
        joinCluster(CACHE_CONFIG_FILE_PATH);
        //setup

        for (int i=1; i<=100; i++) createAndStorePerson();

        long cumulativeLatency = 0;

        for (int i=1; i <= 1000; i++)
        {
            Session session = newSession("hibernate.cfg.xml");
            session.beginTransaction();
            long clockBefore = System.nanoTime();
            Query query = session.createQuery("from Person");
            query.setCacheable(true);
            query.list();
            cumulativeLatency += System.nanoTime() - clockBefore;
            session.getTransaction().commit();
            session.close();
        }

        System.out.println("Average Query.list() latency with query caching: " + cumulativeLatency / 1000L + " nanoseconds");

        //teardown
        leaveCluster();
        cluster.destroy();
        hsqldbProcess.destroy();
        //teardown
    }


    // ---- Internal

    /**
     * Invokes EventManager.createAndStorePerson().
     *
     * @return the Long id of the created Person
     */
    private Long createAndStorePerson()
    {
    	return null;
        //return new EventManager().createAndStorePerson("Randy", "Stafford", 50);
    }

    /**
     * Returns a new Hibernate Session.
     *
     * @param hibernateConfigFilePath the path to the Hibernate configuration file to use
     *
     * @return a Session
     */
    private Session newSession(String hibernateConfigFilePath)
    {
        if (sessionFactory == null)
            sessionFactory = new Configuration().configure(hibernateConfigFilePath).buildSessionFactory();
        return sessionFactory.openSession();

    }

    /**
     * Join the Coherence cluster in the test fixture.
     *
     * @param cacheConfigFilePath the path of the cache config file to use for this member
     */
    private void joinCluster(String cacheConfigFilePath)
    {
        System.setProperty(JavaApplication.JAVA_NET_PREFER_IPV4_STACK, "true");
        System.setProperty(ClusterMemberSchema.PROPERTY_WELL_KNOWN_ADDRESS, WKA_HOST);
        System.setProperty(ClusterMemberSchema.PROPERTY_WELL_KNOWN_ADDRESS_PORT, WKA_PORT);
        System.setProperty(ClusterMemberSchema.PROPERTY_CACHECONFIG, cacheConfigFilePath);
        System.setProperty(ClusterMemberSchema.PROPERTY_DISTRIBUTED_LOCALSTORAGE, "false");
        CacheFactory.ensureCluster();
    }

    /**
     * Leave the Coherence cluster in the test fixture.
     */
    private void leaveCluster()
    {
        CacheFactory.shutdown();
    }

    /**
     * Starts the Coherence cache server process in the text fixture.
     *
     * @param cacheConfigFilePath the path to the cache config file with which to start the cache server
     *
     * @throws IOException if cache server process files could not be opened
     */
    private void startCacheServer(String cacheConfigFilePath)
    throws IOException
    {
        ClusterMemberSchema dcsSchema = new ClusterMemberSchema();
        dcsSchema.setOption("-Xms1024m");
        dcsSchema.setOption("-Xmx1024m");
        dcsSchema.setWellKnownAddress(WKA_HOST);
        dcsSchema.setWellKnownAddressPort(Integer.valueOf(WKA_PORT));
        dcsSchema.setCacheConfigURI(cacheConfigFilePath);
        dcsSchema.setJMXManagementMode(ClusterMemberSchema.JMXManagementMode.ALL);
        dcsSchema.setErrorStreamRedirected(true);
        FileWriterApplicationConsole dcsConsole = new FileWriterApplicationConsole("dcs.log");
        NativeJavaApplicationBuilder<ClusterMember, ClusterMemberSchema> dcsBuilder;
        dcsBuilder = new NativeJavaApplicationBuilder<>();
        ClusterBuilder clusterBuilder = new ClusterBuilder();
        clusterBuilder.addBuilder(dcsBuilder, dcsSchema, "dcs", 1);
        cluster = clusterBuilder.realize(dcsConsole);
    }

    /**
     * Starts the database in the text fixture.
     *
     * @throws IOException if the database process files could not be opened
     */
    private void startDatabase()
    throws IOException
    {
        NativeJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema> hsqldbBuilder;
        hsqldbBuilder = new NativeJavaApplicationBuilder<>();
        SimpleJavaApplicationSchema hsqldbSchema = new SimpleJavaApplicationSchema("org.hsqldb.Server");
        hsqldbSchema.addArgument("-database.0");
        hsqldbSchema.addArgument("file:target/data/tutorial");
        hsqldbSchema.setErrorStreamRedirected(true);
        FileWriterApplicationConsole hsqldbConsole = new FileWriterApplicationConsole("hsqldb.log");
        hsqldbProcess = hsqldbBuilder.realize(hsqldbSchema, "hsqldb", hsqldbConsole);
    }

}
