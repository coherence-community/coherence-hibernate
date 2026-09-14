/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.mock;

/**
 * AbstractCoherenceRegionFactoryTest is an abstract superclass for tests with a CoherenceRegionFactory in the fixture.
 * It abstracts state for holding an instance of CoherenceRegionFactory and the parameters used
 * to create that instance, and behavior for setting up and tearing down that instance.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public abstract class AbstractCoherenceRegionFactoryTests {

    /**
     * The CoherenceRegionFactory in the fixture.
     */
    public CoherenceRegionFactory coherenceRegionFactory;

    /**
     * The Properties used to start the CoherenceRegionFactory.
     */
    private Map<String, Object> properties;

    /**
     * Returns the CoherenceRegionFactory in the fixture.
     *
     * @return the CoherenceRegionFactory in the fixture
     */
    protected CoherenceRegionFactory getCoherenceRegionFactory() {
        if (this.coherenceRegionFactory == null) {
            this.coherenceRegionFactory = new CoherenceRegionFactory();
        }
        return this.coherenceRegionFactory;
    }

    /**
     * Returns the Properties used to start the CoherenceRegionFactory.
     *
     * @return the Properties used to start the CoherenceRegionFactory
     */
    protected Map<String, Object> getProperties() {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        return this.properties;
    }

    /**
     * Returns the Hibernate Settings used to start the CoherenceRegionFactory.
     *
     * @return the SessionFactoryOptions used to start the CoherenceRegionFactory
     */
    protected SessionFactoryOptions getSessionFactoryOptions() {
        return mock(SessionFactoryOptions.class);
    }


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @BeforeEach
    public void setUpAbstractCoherenceRegionFactoryTest() {
        //use a started CoherenceRegionFactory in the test, as a convenience
        //to ensure the cluster is joined and the cache factory is configured etc.
        getCoherenceRegionFactory().start(getSessionFactoryOptions(), getProperties());
    }

    /**
     * Tear down the test fixture.
     */
    @AfterEach
    public void tearDownAbstractCoherenceRegionFactoryTest() {
        if (this.coherenceRegionFactory == null) {
            return;
        }
        this.coherenceRegionFactory.stop();
        this.coherenceRegionFactory = null;
    }
}
