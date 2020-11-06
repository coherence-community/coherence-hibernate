/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.AbstractCoherenceRegionFactoryTest;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.junit.After;
import org.junit.Before;

import java.util.Comparator;

/**
 * AbstractCoherenceRegionTest is an abstract superclass for tests with a CoherenceRegion in the fixture.
 * It abstracts state for holding an instance of CoherenceRegion and the parameters used
 * to create that instance, and behavior for setting up and tearing down that instance.
 *
 * @author Randy Stafford
 */
public abstract class AbstractCoherenceRegionTest
extends AbstractCoherenceRegionFactoryTest
{


    // ---- Fields

    /**
     * The CoherenceRegion in the fixture.
     */
    private CoherenceRegion coherenceRegion;

    /**
     * The CacheDataDescription used to build the CoherenceRegion.
     */
    private CacheDataDescription cacheDataDescription;


    // ---- Accessing

    /**
     * Returns the CacheDataDescription used to build the CoherenceRegion.
     *
     * @return the CacheDataDescription used to start the CoherenceRegion
     */
    protected CacheDataDescription getCacheDataDescription()
    {
        if (cacheDataDescription == null)
        {
            boolean mutable = true;
            boolean versioned = false;
            Comparator comparator = null;
            cacheDataDescription = new CacheDataDescriptionImpl(mutable, versioned, comparator, null);
        }
        return cacheDataDescription;
    }

    /**
     * Returns the CoherenceRegion in the fixture.
     *
     * @return the CoherenceRegion in the fixture
     */
    protected CoherenceRegion getCoherenceRegion()
    {
        return coherenceRegion;
    }

    /**
     * Returns the CoherenceCollectionRegion in the fixture.
     *
     * @return the CoherenceCollectionRegion in the fixture
     */
    protected CoherenceCollectionRegion getCoherenceCollectionRegion()
    {
        return (CoherenceCollectionRegion) getCoherenceRegion();
    }

    /**
     * Returns the CoherenceEntityRegion in the fixture.
     *
     * @return the CoherenceEntityRegion in the fixture
     */
    protected CoherenceEntityRegion getCoherenceEntityRegion()
    {
        return (CoherenceEntityRegion) getCoherenceRegion();
    }

    /**
     * Returns the CoherenceNaturalIdRegion in the fixture.
     *
     * @return the CoherenceNaturalIdRegion in the fixture
     */
    protected CoherenceNaturalIdRegion getCoherenceNaturalIdRegion()
    {
        return (CoherenceNaturalIdRegion) getCoherenceRegion();
    }

    /**
     * Returns the CoherenceTimestampsRegion in the fixture.
     *
     * @return the CoherenceTimestampsRegion in the fixture
     */
    protected CoherenceTimestampsRegion getCoherenceTimestampsRegion()
    {
        return (CoherenceTimestampsRegion) getCoherenceRegion();
    }

    /**
     * Returns the name of the CoherenceRegion in the fixture.
     *
     * @return the String name of the CoherenceRegion in the fixture
     */
    protected String getRegionName()
    {
        return getClass().getSimpleName();
    }


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUpAbstractCoherenceRegionTest()
    {
        coherenceRegion = newCoherenceRegion();
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDownAbstractCoherenceRegionTest()
    {
        if (coherenceRegion == null) return;
        coherenceRegion.evictAll();
        coherenceRegion.destroy();
        coherenceRegion = null;
    }


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected abstract CoherenceRegion newCoherenceRegion();


    // ---- Factory

    /**
     * Returns a new CoherenceCollectionRegion.
     *
     * @return a CoherenceCollectionRegion
     */
    protected CoherenceCollectionRegion newCoherenceCollectionRegion()
    {
        return (CoherenceCollectionRegion) getCoherenceRegionFactory().buildCollectionRegion(getRegionName(), getProperties(), getCacheDataDescription());
    }

    /**
     * Returns a new CoherenceEntityRegion.
     *
     * @return a CoherenceEntityRegion
     */
    protected CoherenceEntityRegion newCoherenceEntityRegion()
    {
        return (CoherenceEntityRegion) getCoherenceRegionFactory().buildEntityRegion(getRegionName(), getProperties(), getCacheDataDescription());
    }

    /**
     * Returns a new CoherenceNaturalIdRegion.
     *
     * @return a CoherenceNaturalIdRegion
     */
    protected CoherenceNaturalIdRegion newCoherenceNaturalIdRegion()
    {
        return (CoherenceNaturalIdRegion) getCoherenceRegionFactory().buildNaturalIdRegion(getRegionName(), getProperties(), getCacheDataDescription());
    }

    /**
     * Returns a new CoherenceTimestampsRegion.
     *
     * @return a CoherenceTimestampsRegion
     */
    protected CoherenceTimestampsRegion newCoherenceTimestampsRegion()
    {
        return (CoherenceTimestampsRegion) getCoherenceRegionFactory().buildTimestampsRegion(getRegionName(), getProperties());
    }


}
