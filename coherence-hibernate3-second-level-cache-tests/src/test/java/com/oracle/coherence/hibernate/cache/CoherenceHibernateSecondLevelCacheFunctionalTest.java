/*
 * File: CoherenceHibernateSecondLevelCacheFunctionalTest.java
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
import org.hibernate.Session;
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
import java.util.Set;

import static org.junit.Assert.*;

/**
 * A CoherenceHibernateSecondLevelCacheFunctionalTest is a functional test of the Coherence-based implementation
 * of the Hibernate second-level cache SPI.  The test strategy is to drive the EventManager class from the Hibernate
 * tutorial and assert expected post-conditions in the Coherence second-level cache given the EventManager operation
 * invoked.  For efficiency the underlying database and Coherence cache server processes remain running throughout
 * the test suite, instead of being started and stopped around each test method.  But for test method independence
 * the cache contents are reset to an initial (cold, empty) state after each test method executes.  Therefore the
 * expected cache contents post-conditions from a cold initial state can be asserted in each test method regardless
 * of the order of test method execution.  The ClassLoader of this class remains a cluster member throughout
 * the execution of the test suite.
 *
 * Note there is not currently functional test coverage of Natural ID caches.  Despite modifying the Event class in the
 * Hibernate tutorial example application to declare natural id fields (via hbm.xml), saving and listing Events
 * in this test suite does not result in the creation of any Natural ID caches.  From stepping through in a debugger,
 * the test fails on line 356 of the Hibernate SessionFactoryImpl constructor, which guards the creation of a NaturalId
 * cache, because the region name is null.  From googling this appears to be a buggy area of Hibernate.  Annotations
 * don't work but hbm.xml does, and there is no place in annotations or XML configuration to specify a region name.
 *
 * @author Randy Stafford
 */
@RunWith(JUnit4.class)
public class CoherenceHibernateSecondLevelCacheFunctionalTest
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
    private static Cluster cluster;

    /**
     * The hsqldb process in the test fixture.
     */
    private static SimpleJavaApplication hsqldbProcess;


    // ---- Setup / teardown

    /**
     * Set up resources in the test fixture that survive test method execution.
     * In other words start the database and Coherence processes used by all tests.
     * These processes are not restarted between test cases for efficiency.
     * Test method independence is maintained by restoring initial cache state in normal teardown.
     *
     * @throws InterruptedException if the calling thread is interrupted while sleeping for fixture startup
     * @throws IOException if files could not be opened for redirecting process output
     */
    @BeforeClass
    public static void setupSuite()
    throws InterruptedException, IOException
    {
        startDatabase();
        startCacheServer();
        Thread.currentThread().sleep(5 * 1000);
        joinCluster();
    }

    /**
     * Tear down resources in the test fixture that survive test method execution.
     * In other words start the database and Coherence processes used by all tests.
     * These processes are not restarted between test cases for efficiency.
     * Test method independence is maintained by restoring initial cache state in normal tearDown.
     */
    @AfterClass
    public static void tearDownSuite()
    {
        leaveCluster();
        cluster.destroy();
        hsqldbProcess.destroy();
    }

    /**
     * Resets all second-level cache contents to initial (cold, empty) state.
     */
    @After
    public void resetCacheContents()
    {
        getEventCache().clear();
        getEventParticipantsCache().clear();
        getPersonCache().clear();
        getPersonEmailAddressesCache().clear();
        getPersonEventsCache().clear();
        getQueryCache().clear();
        getTimestampCache().clear();
    }


    // ---- Test cases

    /**
     * Tests EventManager.createAndStoreEvent() post-conditions in cache.
     */
    @Test
    public void testEventManager_createAndStoreEvent()
    {
        createAndStoreEvent();

        //The Hibernate tutorial example app doesn't cause cache insertion of a newly saved Event.
        //From stepping through transaction commit in a debugger, it doesn't appear that an EntityInsertAction
        //is ever registered, so the doAfterTransactionCompletion() method (which inserts into cache) is never called
        assertEquals("Expect no cache insertion creating new Event", 0, getEventCache().size());

        //EventManager.createAndStoreEvent() doesn't execute any queries
        assertEquals("Expect no query cache insertion creating new Event", 0, getQueryCache().size());

        //Hibernate puts two entries in the timestamps cache on new Event save - one each for the EVENTS and PERSON_EVENTS tables
        assertEquals("Expect two timestamps cache entries on creating new Event", 2, getTimestampCache().size());
    }

    /**
     * Tests EventManager.listEvents() post-conditions in cache.
     */
    @Test
    public void testEventManager_listEvents()
    {
        createAndStoreEvent();
        new EventManager().listEvents();

        assertTrue("Expect cache population listing Events", getEventCache().size() > 0);

        //EventManager.listEvents() executes a cacheable query, causing one entry in the query cache
        assertEquals("Expect one query cache insertion listing Events", 1, getQueryCache().size());

        //Hibernate puts two entries in the timestamps cache on listing Events - one each for the EVENTS and PERSON_EVENTS tables
        assertEquals("Expect two timestamps cache entries on listing Events", 2, getTimestampCache().size());
    }

    /**
     * Tests EventManager.createAndStorePerson() post-conditions in cache.
     */
    @Test
    public void testEventManager_createAndStorePerson()
    {
        createAndStorePerson();

        //The Hibernate tutorial example app doesn't cause cache insertion of a newly saved Person.
        //From stepping through transaction commit in a debugger, it doesn't appear that an EntityInsertAction
        //is ever registered, so the doAfterTransactionCompletion() method (which inserts into cache) is never called
        assertEquals("Expect no cache insertion creating new Person", 0, getPersonCache().size());

        //EventManager.createAndStorePerson() doesn't execute any queries
        assertEquals("Expect no query cache insertion creating new Person", 0, getQueryCache().size());

        //Hibernate puts three entries in the timestamps cache on new Person save -
        //one each for the PERSON, PERSON_EMAIL_ADDR, and PERSON_EVENTS tables
        assertEquals("Expect three timestamps cache entries on creating new Person", 3, getTimestampCache().size());
    }

    /**
     * Tests EventManager.listEvents() post-conditions in cache.
     */
    @Test
    public void testEventManager_listPersons()
    {
        createAndStorePerson();
        new EventManager().listPersons();

        assertTrue("Expect cache population listing Persons", getPersonCache().size() > 0);

        //EventManager.listPersons() executes a cacheable query, causing one entry in the query cache
        assertEquals("Expect one query cache insertion listing Persons", 1, getQueryCache().size());

        //Hibernate puts three entries in the timestamps cache on listing Persons -
        //one each for the PERSON, PERSON_EMAIL_ADDR, and PERSON_EVENTS tables
        assertEquals("Expect three timestamps cache entries on listing Persons", 3, getTimestampCache().size());
    }

    /**
     * Tests EventManager.addPersonToEvent() post-conditions in cache.
     */
    @Test
    public void testEventManager_addPersonToEvent()
    {
        Long eventId = createAndStoreEvent();
        Long personId = createAndStorePerson();
        new EventManager().addPersonToEvent(personId, eventId);

        //the Session.load() call for the Event in EventManager.addPersonToEvent() loads a Hibernate proxy, which causes no cache insertion
        assertEquals("Expect no cached Event afterward", 0, getEventCache().size());
        assertEquals("Expect no cached Event participants afterward", 0, getEventParticipantsCache().size());
        assertEquals("Expect a cached Person afterward", 1, getPersonCache().size());
        assertEquals("Expect no cached Person events afterward", 0, getPersonEventsCache().size());
        assertEquals("Expect one query cache insertion afterward", 1, getQueryCache().size());
    }

    /**
     * Tests EventManager.addEmailToPerson() post-conditions in cache.
     */
    @Test
    public void testEventManager_addEmailToPerson()
    {
        Long personId = createAndStorePerson();
        new EventManager().addEmailToPerson(personId, "randy.stafford@oracle.com");
        assertEquals("Expect a cached Person afterward", 1, getPersonCache().size());
        assertEquals("Expect no cached Person emailAddresses afterward", 0, getPersonEmailAddressesCache().size());
    }

    /**
     * Tests the cache consequences of navigating the Person/Events relationship in both directions.
     */
    @Test
    public void testNavigatingPersonEvents()
    {
        Long eventId = createAndStoreEvent();
        Long personId = createAndStorePerson();
        new EventManager().addPersonToEvent(personId, eventId);

        //the Session.load() call for the Event in EventManager.addPersonToEvent() loads a Hibernate proxy, which causes no cache insertion
        assertEquals("Expect no cached Event afterward", 0, getEventCache().size());
        assertEquals("Expect no cached Event participants afterward", 0, getEventParticipantsCache().size());
        assertEquals("Expect a cached Person afterward", 1, getPersonCache().size());
        assertEquals("Expect no cached Person events afterward", 0, getPersonEventsCache().size());
        assertEquals("Expect one query cache insertion afterward", 1, getQueryCache().size());

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person person = (Person) session.load(Person.class, personId);
        Set events = person.getEvents();
        assertEquals("Expect no cached Person events afterward", 0, getPersonEventsCache().size()); //surprisingly
        assertEquals("Expect person.getEvents().size() == 1", 1, events.size());

        Iterator iterator = events.iterator();
        while (iterator.hasNext())
        {
            Event event = (Event) iterator.next();
            Set participants = event.getParticipants();
            assertTrue("Expect event participants to contain the person", participants.contains(person));
        }
        assertEquals("Expect one cached Person events afterward", 1, getPersonEventsCache().size());
        assertEquals("Expect one cached Event participants afterward", 1, getEventParticipantsCache().size());

        session.getTransaction().commit();
    }

    /**
     * Tests the cache consequences of navigating the Person/emailAddresses relationship.
     */
    @Test
    public void testNavigatingPersonEmailAddresses()
    {
        Long personId = createAndStorePerson();
        new EventManager().addEmailToPerson(personId, "randy.stafford@oracle.com");
        assertEquals("Expect a cached Person afterward", 1, getPersonCache().size());
        assertEquals("Expect no cached Person emailAddresses afterward", 0, getPersonEmailAddressesCache().size());

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person person = (Person) session.load(Person.class, personId);
        Set emailAddresses = person.getEmailAddresses();
        assertEquals("Expect person.getEmailAddresses().size() == 1", 1, emailAddresses.size());
        assertEquals("Expect one cached Person emailAddresses afterward", 1, getPersonEmailAddressesCache().size());

        session.getTransaction().commit();
    }

    /**
     * Tests that cache is hit on second read.
     */
    @Test
    public void testCacheHitOnSecondRead()
    throws MalformedObjectNameException
    {
        ClusterMember dcs = cluster.iterator().next();
        ObjectName objectName = new ObjectName("Coherence:type=Cache,service=TestHibernateSecondLevelCache,name=org.hibernate.tutorial.domain.Person,nodeId=1,tier=back");

        Long personId = createAndStorePerson();
        assertEquals("Expect no cache insertion creating new Person", 0, getPersonCache().size());

        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        long totalPutsBefore = dcs.getMBeanAttribute(objectName, "TotalPuts", Long.class);
        long totalGetsBefore = dcs.getMBeanAttribute(objectName, "TotalGets", Long.class);
        long cacheHitsBefore = dcs.getMBeanAttribute(objectName, "CacheHits", Long.class);

        Person person = (Person) session.get(Person.class, personId);

        long totalPutsAfter = dcs.getMBeanAttribute(objectName, "TotalPuts", Long.class);
        long totalGetsAfter = dcs.getMBeanAttribute(objectName, "TotalGets", Long.class);
        long cacheHitsAfter = dcs.getMBeanAttribute(objectName, "CacheHits", Long.class);
        assertEquals("Expect one put", 1, totalPutsAfter - totalPutsBefore);
        assertEquals("Expect one get", 1, totalGetsAfter - totalGetsBefore);
        assertEquals("Expect zero hits", 0, cacheHitsAfter - cacheHitsBefore);

        session.getTransaction().commit();
        session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        totalPutsBefore = totalPutsAfter;
        totalGetsBefore = totalGetsAfter;
        cacheHitsBefore = cacheHitsAfter;

        person = (Person) session.get(Person.class, personId);

        totalPutsAfter = dcs.getMBeanAttribute(objectName, "TotalPuts", Long.class);
        totalGetsAfter = dcs.getMBeanAttribute(objectName, "TotalGets", Long.class);
        cacheHitsAfter = dcs.getMBeanAttribute(objectName, "CacheHits", Long.class);
        assertEquals("Expect no more puts", totalPutsAfter, totalPutsBefore);
        assertEquals("Expect one more get", 1, totalGetsAfter - totalGetsBefore);
        assertEquals("Expect a cache hit", 1, cacheHitsAfter - cacheHitsBefore);

        session.getTransaction().commit();
    }



    // ---- Internal

    /**
     * Invokes EventManager.createAndStoreEvent().
     *
     * @return the Long id of the created Event
     */
    private Long createAndStoreEvent()
    {
        return new EventManager().createAndStoreEvent(getClass().getSimpleName(), new Date());
    }

    /**
     * Invokes EventManager.createAndStorePerson().
     *
     * @return the Long id of the created Person
     */
    private Long createAndStorePerson()
    {
        return new EventManager().createAndStorePerson("Randy", "Stafford", 50);
    }

    /**
     * Returns the cache for Events.
     *
     * @return the NamedCache for Events
     */
    private NamedCache getEventCache()
    {
        return CacheFactory.getCache(Event.class.getName());
    }

    /**
     * Returns the cache for Event participants.
     *
     * @return the NamedCache for Event participants
     */
    private NamedCache getEventParticipantsCache()
    {
        return CacheFactory.getCache(Event.class.getName() + ".participants");
    }

    /**
     * Returns the cache for Persons.
     *
     * @return the NamedCache for Persons
     */
    private NamedCache getPersonCache()
    {
        return CacheFactory.getCache(Person.class.getName());
    }

    /**
     * Returns the cache for Person emailAddresses.
     *
     * @return the NamedCache for Person emailAddresses
     */
    private NamedCache getPersonEmailAddressesCache()
    {
        return CacheFactory.getCache(Person.class.getName() + ".emailAddresses");
    }

    /**
     * Returns the cache for Person events.
     *
     * @return the NamedCache for Person events
     */
    private NamedCache getPersonEventsCache()
    {
        return CacheFactory.getCache(Person.class.getName() + ".events");
    }

    /**
     * Returns the query cache.
     *
     * @return the NamedCache for query results
     */
    private NamedCache getQueryCache()
    {
        return CacheFactory.getCache("org.hibernate.cache.StandardQueryCache");
    }

    /**
     * Returns the timestamps cache.
     *
     * @return the NamedCache for timestamps
     */
    private NamedCache getTimestampCache()
    {
        return CacheFactory.getCache("org.hibernate.cache.UpdateTimestampsCache");
    }

    /**
     * Join the Coherence cluster in the test fixture.
     */
    private static void joinCluster()
    {
        System.setProperty(JavaApplication.JAVA_NET_PREFER_IPV4_STACK, "true");
        System.setProperty(ClusterMemberSchema.PROPERTY_WELL_KNOWN_ADDRESS, WKA_HOST);
        System.setProperty(ClusterMemberSchema.PROPERTY_WELL_KNOWN_ADDRESS_PORT, WKA_PORT);
        System.setProperty(ClusterMemberSchema.PROPERTY_CACHECONFIG, CACHE_CONFIG_FILE_PATH);
        System.setProperty(ClusterMemberSchema.PROPERTY_DISTRIBUTED_LOCALSTORAGE, "false");
        CacheFactory.ensureCluster();
    }

    /**
     * Leave the Coherence cluster in the test fixture.
     */
    private static void leaveCluster()
    {
        CacheFactory.shutdown();
    }

    /**
     * Starts the Coherence cache server process in the text fixture.
     *
     * @throws IOException if cache server process files could not be opened
     */
    private static void startCacheServer()
    throws IOException
    {
        ClusterMemberSchema dcsSchema = new ClusterMemberSchema();
        dcsSchema.setWellKnownAddress(WKA_HOST);
        dcsSchema.setWellKnownAddressPort(Integer.valueOf(WKA_PORT));
        dcsSchema.setCacheConfigURI(CACHE_CONFIG_FILE_PATH);
        dcsSchema.setJMXManagementMode(ClusterMemberSchema.JMXManagementMode.ALL);
        dcsSchema.setErrorStreamRedirected(true);
        FileWriterApplicationConsole dcsConsole = new FileWriterApplicationConsole("target/dcs.log");
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
    private static void startDatabase()
    throws IOException
    {
        NativeJavaApplicationBuilder<SimpleJavaApplication, SimpleJavaApplicationSchema> hsqldbBuilder;
        hsqldbBuilder = new NativeJavaApplicationBuilder<>();
        SimpleJavaApplicationSchema hsqldbSchema = new SimpleJavaApplicationSchema("org.hsqldb.Server");
        hsqldbSchema.addArgument("-database.0");
        hsqldbSchema.addArgument("file:target/data/tutorial");
        hsqldbSchema.setErrorStreamRedirected(true);
        FileWriterApplicationConsole hsqldbConsole = new FileWriterApplicationConsole("target/hsqldb.log");
        hsqldbProcess = hsqldbBuilder.realize(hsqldbSchema, "hsqldb", hsqldbConsole);
    }

}
