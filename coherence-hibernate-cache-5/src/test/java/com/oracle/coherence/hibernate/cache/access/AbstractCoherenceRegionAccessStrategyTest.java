/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.AbstractCoherenceRegionTest;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.junit.After;
import org.junit.Before;

/**
 * AbstractCoherenceRegionAccessStrategyTest is an abstract superclass for tests with a CoherenceRegionAccessStrategy
 * in the fixture.  It abstracts state for holding an instance of CoherenceRegion and behavior for setting up and
 * tearing down that instance.
 *
 * @author Randy Stafford
 */
public abstract class AbstractCoherenceRegionAccessStrategyTest
extends AbstractCoherenceRegionTest
{


    // ---- Fields

    /**
     * The CoherenceRegionAccessStrategy in the test fixture.
     */
    private CoherenceRegionAccessStrategy coherenceRegionAccessStrategy;


    // ---- Accessing

    /**
     * Returns the CoherenceRegionAccessStrategy in the fixture.
     *
     * @return the CoherenceRegionAccessStrategy in the fixture
     */
    protected CoherenceRegionAccessStrategy getCoherenceRegionAccessStrategy()
    {
        return coherenceRegionAccessStrategy;
    }

    /**
     * Returns the CollectionRegionAccessStrategy in the fixture.
     *
     * @return the CollectionRegionAccessStrategy in the fixture
     */
    protected CollectionRegionAccessStrategy getCollectionRegionAccessStrategy()
    {
        return (CollectionRegionAccessStrategy) coherenceRegionAccessStrategy;
    }

    /**
     * Returns the EntityRegionAccessStrategy in the fixture.
     *
     * @return the EntityRegionAccessStrategy in the fixture
     */
    protected EntityRegionAccessStrategy getEntityRegionAccessStrategy()
    {
        return (EntityRegionAccessStrategy) coherenceRegionAccessStrategy;
    }

    /**
     * Returns the NaturalIdRegionAccessStrategy in the fixture.
     *
     * @return the NaturalIdRegionAccessStrategy in the fixture
     */
    protected NaturalIdRegionAccessStrategy getNaturalIdRegionAccessStrategy()
    {
        return (NaturalIdRegionAccessStrategy) coherenceRegionAccessStrategy;
    }


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUpAbstractCoherenceRegionAccessStrategyTest()
    {
        coherenceRegionAccessStrategy = newCoherenceRegionAccessStrategy();
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDownAbstractCoherenceRegionAccessStrategyTest()
    {
        coherenceRegionAccessStrategy = null;
    }


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected abstract CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy();


    // ---- Factory

    /**
     * Returns a new CollectionReadOnlyCoherenceRegionAccessStrategy.
     *
     * @return an CollectionReadOnlyCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newCollectionReadOnlyCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceCollectionRegion().buildAccessStrategy(AccessType.READ_ONLY);
    }

    /**
     * Returns a new CollectionNonstrictReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an CollectionNonstrictReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newCollectionNonstrictReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceCollectionRegion().buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
    }

    /**
     * Returns a new CollectionReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an CollectionReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newCollectionReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceCollectionRegion().buildAccessStrategy(AccessType.READ_WRITE);
    }

    /**
     * Returns a new EntityReadOnlyCoherenceRegionAccessStrategy.
     *
     * @return an EntityReadOnlyCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newEntityReadOnlyCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceEntityRegion().buildAccessStrategy(AccessType.READ_ONLY);
    }

    /**
     * Returns a new EntityNonstrictReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an EntityNonstrictReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newEntityNonstrictReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceEntityRegion().buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
    }

    /**
     * Returns a new EntityReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an EntityReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newEntityReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceEntityRegion().buildAccessStrategy(AccessType.READ_WRITE);
    }

    /**
     * Returns a new NaturalIdReadOnlyCoherenceRegionAccessStrategy.
     *
     * @return an NaturalIdReadOnlyCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newNaturalIdReadOnlyCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceNaturalIdRegion().buildAccessStrategy(AccessType.READ_ONLY);
    }

    /**
     * Returns a new NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy.
     *
     * @return an NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy
     */
    protected CoherenceRegionAccessStrategy newNaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy()
    {
        return (CoherenceRegionAccessStrategy) getCoherenceNaturalIdRegion().buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
    }


}
