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

package com.oracle.coherence.hibernate.cachestore;

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
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.ConditionalRemove;
import org.hibernate.Session;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * A CoherenceHibernateCacheStoreFunctionalTest is a functional test of the Hibernate-based implementation
 * of the Coherence CacheStore SPI.  The test strategy is to drive the NamedCache API operations that result
 * in CacheStore calls, i.e. get, put, and remove, and assert expected post-conditions including in cache.
 *
 * For efficiency the underlying database and Coherence cache server processes remain running throughout
 * the test suite, instead of being started and stopped around each test method.  But for test method independence
 * the cache contents are reset to an initial (cold, empty) state after each test method executes.  Therefore the
 * expected cache contents post-conditions from a cold initial state can be asserted in each test method regardless
 * of the order of test method execution.  The ClassLoader of this class remains a cluster member throughout
 * the execution of the test suite.
 *
 * @author Randy Stafford
 */
@RunWith(JUnit4.class)
public class CoherenceHibernateCacheStoreFunctionalTest
{


    // ---- Constants

    /**
     * The path of the Coherence cache configuration file used in the test fixture.
     */
    private static String CACHE_CONFIG_FILE_PATH = "test-hibernate-cache-store-config.xml";

    /**
     * The username with which to log in to the database in the test fixture.
     */
    private static String DATABASE_USERNAME = "SA";

    /**
     * The password with which to log in to the database in the test fixture.
     */
    private static String DATABASE_PASSWORD = "";

    /**
     * The JDBC URL to the database in the test fixture.
     */
    private static String JDBC_URL = "jdbc:hsqldb:hsql://localhost";

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
     * A connection to the database in the test fixture.
     * Used by assertions, teardown logic, etc.
     */
    private static Connection connection;

    /**
     * The hsqldb process in the test fixture.
     */
    private static SimpleJavaApplication hsqldbProcess;

    /**
     * A sequence for Person identifiers.
     */
    private static long nextPersonIdentifier = 0L;


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
        initializeSchema();
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
        closeConnection();
        hsqldbProcess.destroy();
    }

    /**
     * Resets all second-level cache contents to initial (cold, empty) state.
     */
    @After
    public void resetCacheContents()
    {
        getPersonCache().clear();
    }

    /**
     * Resets all database contents to initial (cold, empty) state.
     *
     * @throws SQLException if there was some problem interacting with the database
     */
    @After
    public void resetDatabaseContents()
    throws SQLException
    {
        truncatePersonTable();
    }


    // ---- Test cases

    /**
     * Tests HibernateCacheStore.store() via NamedCache.put().
     *
     * @throws SQLException if there was some problem interacting with the database while asserting expected results
     */
    @Test
    public void testHibernateCacheStore_store()
    throws SQLException
    {
        Person person = newPerson();
        getPersonCache().put(person.getId(), person);

        assertEquals("Expect person cache size of one", 1, getPersonCache().size());
        assertEquals("Expect PERSON table count of one", 1, getPersonTableCount());
    }

    /**
     * Tests HibernateCacheStore.storeAll() via NamedCache.putAll().
     *
     * @throws SQLException if there was some problem interacting with the database while asserting expected results
     */
    @Test
    public void testHibernateCacheStore_storeAll()
    throws SQLException
    {
        Person person1 = newPerson();
        Person person2 = newPerson();
        HashMap<Long,Person> hashMap = new HashMap<>();
        hashMap.put(person1.getId(), person1);
        hashMap.put(person2.getId(), person2);
        getPersonCache().putAll(hashMap);

        assertEquals("Expect person cache size of two", 2, getPersonCache().size());
        assertEquals("Expect PERSON table count of two", 2, getPersonTableCount());
    }

    /**
     * Tests HibernateCacheLoader.load() via NamedCache.get().
     */
    @Test
    public void testHibernateCacheStore_load()
    {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Person savedPerson = newPerson();
        session.save(savedPerson);
        session.getTransaction().commit();

        Person gottenPerson = (Person) getPersonCache().get(savedPerson.getId());

        assertNotNull("Expect non-null gotten person", gottenPerson);
        assertEquals("Expect gotten person equal to saved person", gottenPerson, savedPerson);
        assertEquals("Expect person cache size of one", 1, getPersonCache().size());
    }

    /**
     * Tests HibernateCacheStore.storeAll() via NamedCache.putAll().
     */
    @Test
    public void testHibernateCacheStore_loadAll()
    {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Person savedPerson1 = newPerson();
        Person savedPerson2 = newPerson();
        session.save(savedPerson1);
        session.save(savedPerson2);
        session.getTransaction().commit();

        Map gottenPersons = getPersonCache().getAll(Arrays.asList(new Long[] {savedPerson1.getId(), savedPerson2.getId()}));
        assertEquals("Expect two gotten persons", 2, gottenPersons.size());

        Person gottenPerson1 = (Person)gottenPersons.get(savedPerson1.getId());
        Person gottenPerson2 = (Person)gottenPersons.get(savedPerson2.getId());

        assertNotNull("Expect non-null gotten person 1", gottenPerson1);
        assertNotNull("Expect non-null gotten person 2", gottenPerson2);
        assertEquals("Expect gotten person 1 equal to saved person 1", gottenPerson1, savedPerson1);
        assertEquals("Expect gotten person 2 equal to saved person 2", gottenPerson2, savedPerson2);
        assertEquals("Expect person cache size of two", 2, getPersonCache().size());
    }

    /**
     * Tests HibernateCacheStore.erase() via NamedCache.remove().
     *
     * @throws SQLException if there was some problem interacting with the database while asserting expected results
     */
    @Test
    public void testHibernateCacheStore_erase()
    throws SQLException
    {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Person savedPerson = newPerson();
        session.save(savedPerson);
        session.getTransaction().commit();

        getPersonCache().get(savedPerson.getId());
        getPersonCache().remove(savedPerson.getId());

        assertEquals("Expect person cache size of zero", 0, getPersonCache().size());
        assertEquals("Expect PERSON table count of zero", 0, getPersonTableCount());
    }

    /**
     * Tests HibernateCacheStore.eraseAll()
     *
     * @throws SQLException if there was some problem interacting with the database while asserting expected results
     */
    @Test
    public void testHibernateCacheStore_eraseAll()
    throws SQLException
    {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Person savedPerson1 = newPerson();
        Person savedPerson2 = newPerson();
        session.save(savedPerson1);
        session.save(savedPerson2);
        session.getTransaction().commit();

        List keyList = Arrays.asList(new Long[] {savedPerson1.getId(), savedPerson2.getId()});
        getPersonCache().getAll(keyList);
        getPersonCache().invokeAll(keyList, new ConditionalRemove(new AlwaysFilter()));

        assertEquals("Expect person cache size of zero", 0, getPersonCache().size());
        assertEquals("Expect PERSON table count of zero", 0, getPersonTableCount());
    }


    // ---- Internal

    /**
     * Returns a new Person.
     *
     * @return a Person newly constructed
     */
    private Person newPerson()
    {
        Person person = new Person();
        person.setId(getNextPersonId());
        person.setFirstname("John");
        person.setLastname("Smith");
        person.setAge(person.getId().intValue());
        return person;
    }

    /**
     * Returns the next identifier in sequence for a new Person.
     *
     * @return a long identifier for a Person
     */
    private long getNextPersonId()
    {
        return nextPersonIdentifier++;
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
    private static void startDatabase()
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

    /**
     * Initializes the database schema, indirectly via Hibernate.
     */
    private static void initializeSchema()
    {
        HibernateUtil.getSessionFactory();
    }

    /**
     * Closes the database connection.
     */
    private static void closeConnection()
    {
        if (connection == null) return;
        try
        {
            connection.close();
        }
        catch (SQLException exception)
        {
            exception.printStackTrace();
            //best efforts
        }
    }

    /**
     * Ensures the connection field is initialized.
     *
     * @throws SQLException if there was some problem interacting with the database
     */
    private void ensureConnection()
    throws SQLException
    {
        if (connection == null)
        {
            Properties properties = new Properties();
            properties.put("user", DATABASE_USERNAME);
            properties.put("password", DATABASE_PASSWORD);
            connection = DriverManager.getConnection(JDBC_URL, properties);
        }
    }

    /**
     * Truncates the PERSON table in the database.
     *
     * @throws SQLException if there was some problem interacting with the database
     */
    private void truncatePersonTable()
    throws SQLException
    {
        ensureConnection();
        Statement statement = null;
        try
        {
            statement = connection.createStatement();
            statement.executeUpdate("truncate table PERSON");
        }
        finally
        {
            if (statement != null)
                try
                {
                    statement.close();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                    //best efforts
                }
        }
    }

    /**
     * Returns the count of records in the PERSON table.
     *
     * @return the int count of records in the PERSON table
     *
     * @throws SQLException if there was some problem interacting with the database
     */
    private int getPersonTableCount()
    throws SQLException
    {
        ensureConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select count(*) from PERSON");
            resultSet.next();
            return resultSet.getInt(1);
        }
        finally
        {
            if (resultSet != null)
                try
                {
                    resultSet.close();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                    //best efforts
                }
            if (statement != null)
                try
                {
                    statement.close();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                    //best efforts
                }
        }
    }

}
