/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A CoherenceTransactionalDataRegionTest is test of CoherenceTransactionalDataRegion behavior.  For convenience an
 * instance of a subclass (CoherenceEntityRegion) is the fixture used for testing the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceTransactionalDataRegionTest
extends AbstractCoherenceRegionTest
{


    // ---- Subclass responsibility

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegion newCoherenceRegion()
    {
        return newCoherenceEntityRegion();
    }


    // ---- Test cases

    /**
     * Tests CoherenceTransactionalDataRegion.isTransactionAware().
     */
    @Test
    public void testIsTransactionAware()
    {
        assertFalse("Expect not transaction aware", getCoherenceEntityRegion().isTransactionAware());
    }

    /**
     * Tests CoherenceTransactionalDataRegion.getCacheDataDescription().
     */
    @Test
    public void testGetCacheDataDescription()
    {
        assertEquals("Expect same CacheDataDescription", getCacheDataDescription(), getCoherenceEntityRegion().getCacheDataDescription());
    }


}
