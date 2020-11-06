/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache;

import org.hibernate.cfg.Settings;
import org.junit.After;
import org.junit.Before;

import java.util.Properties;

/**
 * AbstractCoherenceRegionFactoryTest is an abstract superclass for tests with a CoherenceRegionFactory in the fixture.
 * It abstracts state for holding an instance of CoherenceRegionFactory and the parameters used
 * to create that instance, and behavior for setting up and tearing down that instance.
 *
 * @author Randy Stafford
 */
public abstract class AbstractCoherenceRegionFactoryTest
{

    // ---- Fields

    /**
     * The CoherenceRegionFactory in the fixture.
     */
    private CoherenceRegionFactory coherenceRegionFactory;

    /**
     * The Properties used to start the CoherenceRegionFactory.
     */
    private Properties properties;


    // ---- Accessing

    /**
     * Returns the CoherenceRegionFactory in the fixture.
     *
     * @return the CoherenceRegionFactory in the fixture
     */
    protected CoherenceRegionFactory getCoherenceRegionFactory()
    {
        if (coherenceRegionFactory == null) coherenceRegionFactory = new CoherenceRegionFactory();
        return coherenceRegionFactory;
    }

    /**
     * Returns the Properties used to start the CoherenceRegionFactory.
     *
     * @return the Properties used to start the CoherenceRegionFactory
     */
    protected Properties getProperties()
    {
        if (properties == null) properties = new Properties();
        return properties;
    }

    /**
     * Returns the Hibernate Settings used to start the CoherenceRegionFactory.
     *
     * @return the Settings used to start the CoherenceRegionFactory
     */
    protected Settings getSettings()
    {
        //Settings is difficult to instantiate, depending on a lot of Hibernate configuration infrastructure.
        //It's also a final class and therefore difficult to mock.  Use null as long as we can get away with it.
        return null;
    }


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUpAbstractCoherenceRegionFactoryTest()
    {
        //use a started CoherenceRegionFactory in the test, as a convenience
        //to ensure the cluster is joined and the cache factory is configured etc.
        getCoherenceRegionFactory().start(getSettings(), getProperties());
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDownAbstractCoherenceRegionFactoryTest()
    {
        if (coherenceRegionFactory == null) return;
        coherenceRegionFactory.stop();
        coherenceRegionFactory = null;
    }


}
