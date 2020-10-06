package com.oracle.coherence.hibernate.cache.region;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A CoherenceGeneralDataRegionTest is test of CoherenceGeneralDataRegion behavior.  For convenience an instance
 * of a subclass (CoherenceTimestampsRegion) is the fixture used for testing the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceGeneralDataRegionTest
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
        return newCoherenceTimestampsRegion();
    }


    // ---- Test cases

    /**
     * Tests CoherenceGeneralDataRegion.put() and CoherenceGeneralDataRegion.get().
     */
    @Test
    public void testPutGet()
    {
        CoherenceTimestampsRegion coherenceTimestampsRegion = getCoherenceTimestampsRegion();
        Object key = "testPutGet";
        Object objectPut = "testObject";
        coherenceTimestampsRegion.put(null, key, objectPut);
        Object objectGot = coherenceTimestampsRegion.get(null, key);
        assertEquals("Expect got same object as put", objectPut, objectGot);
    }


}
