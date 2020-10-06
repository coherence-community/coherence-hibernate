package com.oracle.coherence.hibernate.cache.region;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.processor.ExtractorProcessor;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * A CoherenceRegionTest is test of CoherenceRegion behavior.  For convenience an instance of a subclass
 * (CoherenceEntityRegion) is the fixture used for testing the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceRegionTest
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
     * Tests CoherenceRegion.toString().
     */
    @Test
    public void testToString()
    {
        //this is mainly to ensure no NPEs, infinite loops, etc. in toString()
        String toString = getCoherenceEntityRegion().toString();
        assertNotNull("Expect non-null toString()", toString);
        assertTrue("Expect non-empty toString()", toString.length() > 0);
    }

    /**
     * Tests CoherenceRegion.putValue() and CoherenceRegion.getValue().
     */
    @Test
    public void testPutValueGetValue()
    {
        Object key = "testPutValueGetValue";

        CoherenceRegion.Value valuePut = putValue(key);
        Object objectPut = valuePut.getValue();

        CoherenceRegion.Value valueGot = getCoherenceEntityRegion().getValue(key);
        Object objectGot = valueGot.getValue();

        assertEquals("Expect got same value as put", valuePut, valueGot);
        assertEquals("Expect got same object as put", objectPut, objectGot);
    }

    /**
     * Tests CoherenceRegion.evict().
     */
    @Test
    public void testEvict()
    {
        CoherenceEntityRegion coherenceEntityRegion = getCoherenceEntityRegion();

        Object key = "testEvict";
        putValue(key);

        long elementCountPreEviction = coherenceEntityRegion.getElementCountInMemory();
        getCoherenceEntityRegion().evict(key);
        long elementCountPostEviction = coherenceEntityRegion.getElementCountInMemory();

        assertEquals("Expect one less element post-eviction", elementCountPreEviction-1, elementCountPostEviction);
        assertTrue("Expect region not to contain evicted key", !getCoherenceEntityRegion().contains(key));
    }

    /**
     * Tests CoherenceRegion.evictAll().
     */
    @Test
    public void testEvictAll()
    {
        CoherenceEntityRegion coherenceEntityRegion = getCoherenceEntityRegion();

        Object key = "testEvictAll";
        putValue(key);

        coherenceEntityRegion.evictAll();
        assertEquals("Expect empty region after evictAll", 0, coherenceEntityRegion.getElementCountInMemory());
    }

    /**
     * Tests CoherenceRegion.lockCache().
     */
    //@Test
    public void testLockCache()
    {
        //TODO: how to test this?
    }

    /**
     * Tests CoherenceRegion.unlockCache().
     */
    //@Test
    public void testUnlockCache()
    {
        //TODO: how to test this?
    }

    /**
     * Tests CoherenceRegion.invoke().
     */
    @Test
    public void testInvoke()
    {
        Object key = "testInvoke";
        CoherenceRegion.Value valuePut = putValue(key);
        Object invocationReturnValue = getCoherenceEntityRegion().invoke(key, new ExtractorProcessor(IdentityExtractor.INSTANCE));
        assertEquals("Expect ExtractorProcessor invocation to return same value put", valuePut, invocationReturnValue);
    }

    /**
     * Tests CoherenceRegion.getName().  Indirectly tests the instantiation and binding of a NamedCache
     * in the CoherenceRegion.
     */
    @Test
    public void testGetName()
    {
        assertEquals("Expect correct region name", getRegionName(), getCoherenceEntityRegion().getName());
    }

    /**
     * Tests CoherenceRegion.destroy().
     */
    //@Test
    public void testDestroy()
    {
        //TODO: how to test this?
    }

    /**
     * Tests CoherenceRegion.contains().
     */
    @Test
    public void testContains()
    {
        Object key = "testContains";
        putValue(key);
        assertTrue("Expect region contains value put", getCoherenceEntityRegion().contains(key));
    }

    /**
     * Tests CoherenceRegion.getSizeInMemory().
     */
    @Test
    public void testGetSizeInMemory()
    {
        assertEquals("Expect size in memory -1", -1, getCoherenceEntityRegion().getSizeInMemory());
    }

    /**
     * Tests CoherenceRegion.getElementCountInMemory().
     */
    @Test
    public void testGetElementCountInMemory()
    {
        CoherenceEntityRegion coherenceEntityRegion = getCoherenceEntityRegion();
        coherenceEntityRegion.evictAll();
        assertEquals("Expect element count in memory 0", 0, coherenceEntityRegion.getElementCountInMemory());
        putValue("testGetElementCountInMemory");
        assertEquals("Expect element count in memory 1", 1, coherenceEntityRegion.getElementCountInMemory());
    }

    /**
     * Tests CoherenceRegion.getElementCountOnDisk().
     */
    @Test
    public void testGetElementCountOnDisk()
    {
        assertEquals("Expect element count on disk -1", -1, getCoherenceEntityRegion().getElementCountOnDisk());
    }

    /**
     * Tests CoherenceRegion.toMap().
     */
    @Test
    public void testToMap()
    {
        assertTrue("Expect a Map", getCoherenceEntityRegion().toMap() instanceof Map);
    }

    /**
     * Tests CoherenceRegion.nextTimestamp().
     */
    @Test
    public void testNextTimestamp()
    {
        long currentTime = getCoherenceEntityRegion().nextTimestamp();
        assertTrue("Expect positive current time value", currentTime > 0);
    }

    /**
     * Tests CoherenceRegion.getTimeout().
     */
    @Test
    public void testTimeout()
    {
        assertEquals("Expect default lock lease duration", CoherenceRegion.DEFAULT_LOCK_LEASE_DURATION, getCoherenceEntityRegion().getTimeout());
    }


    // ---- Internal

    /**
     * Puts a test value into the CoherenceEntityRegion at the argument key.
     *
     * @param key the key at which to put a test value
     * @return the Value that was put
     */
    private CoherenceRegion.Value putValue(Object key)
    {
        CoherenceEntityRegion coherenceEntityRegion = getCoherenceEntityRegion();
        Object objectPut = "testObject";
        Object versionPut = 1;
        CoherenceRegion.Value valuePut = new CoherenceRegion.Value(objectPut, versionPut, coherenceEntityRegion.nextTimestamp());
        coherenceEntityRegion.putValue(key, valuePut);
        return valuePut;
    }



}
