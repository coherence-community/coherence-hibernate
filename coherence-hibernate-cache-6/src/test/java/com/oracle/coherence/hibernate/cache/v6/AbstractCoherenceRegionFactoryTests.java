/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
     * The Hibernate SessionFactoryOptions used to start the CoherenceRegionFactory.
     */
    private SessionFactoryOptions sessionFactoryOptions;

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
        final SessionFactoryOptions sessionFactoryOptions = mock(SessionFactoryOptions.class);
        final StandardServiceRegistry serviceRegistry = mock(StandardServiceRegistry.class);
        final StrategySelector strategySelector = mock(StrategySelector.class);
        when(sessionFactoryOptions.getServiceRegistry()).thenReturn(serviceRegistry);
        when(serviceRegistry.getService(StrategySelector.class)).thenReturn(strategySelector);
        when(strategySelector.resolveDefaultableStrategy(Mockito.any(), Mockito.any(), Mockito.isA(DefaultCacheKeysFactory.class)))
                .thenReturn(new DefaultCacheKeysFactory());
        return sessionFactoryOptions;
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
