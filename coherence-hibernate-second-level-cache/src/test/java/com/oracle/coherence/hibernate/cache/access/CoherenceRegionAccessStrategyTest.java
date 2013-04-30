package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A CoherenceRegionAccessStrategyTest is a test of CoherenceRegionAccessStrategy behavior.  For convenience an instance
 * of a subclass (EntityReadOnlyCoherenceRegionAccessStrategy) is the fixture used for testing the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceRegionAccessStrategyTest
extends AbstractCoherenceRegionAccessStrategyTest
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

    /**
     * Return a new CoherenceRegion of the appropriate subtype.
     *
     * @return a CoherenceRegion of the appropriate subtype
     */
    protected CoherenceRegionAccessStrategy newCoherenceRegionAccessStrategy()
    {
        return newEntityReadOnlyCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests CoherenceRegionAccessStrategy.toString().
     */
    @Test
    public void testToString()
    {
        //this is mainly to ensure no NPEs, infinite loops, etc. in toString()
        String toString = getCoherenceRegionAccessStrategy().toString();
        assertNotNull("Expect non-null toString()", toString);
        assertTrue("Expect non-empty toString()", toString.length() > 0);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.putFromLoad() and get() with "minimal puts" not in effect.
     */
    @Test
    public void testPutFromLoadMinimalPutsNotInEffect()
    {
        Object key = "testPutFromLoadMinimalPutsNotInEffect";
        Object value = "testPutFromLoadMinimalPutsNotInEffect";

        boolean objectWasPut = putFromLoad(key, value, false);
        assertTrue("Expect object to be put", objectWasPut);

        Object objectGot = getCoherenceRegionAccessStrategy().get(key, getCoherenceRegion().nextTimestamp());
        assertEquals("Expect to get same object put", value, objectGot);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.putFromLoad() and get() with "minimal puts" in effect
     * and the region not containing the key.
     */
    @Test
    public void testPutFromLoadMinimalPutsInEffectKeyAbsent()
    {
        Object key = "testPutFromLoadMinimalPutsInEffectKeyAbsent";
        Object value = "testPutFromLoadMinimalPutsInEffectKeyAbsent";

        boolean objectWasPut = putFromLoad(key, value, true);
        assertTrue("Expect object to be put", objectWasPut);

        Object objectGot = getCoherenceRegionAccessStrategy().get(key, getCoherenceRegion().nextTimestamp());
        assertEquals("Expect to get same object put", value, objectGot);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.putFromLoad() and get() with "minimal puts" in effect
     * and the region containing the key.
     */
    @Test
    public void testPutFromLoadMinimalPutsInEffectKeyPresent()
    {
        Object key = "testPutFromLoadMinimalPutsInEffectKeyPresent";
        Object value = "testPutFromLoadMinimalPutsInEffectKeyPresent";
        putFromLoad(key, value, false);

        boolean objectWasPut = putFromLoad(key, value, true);
        assertFalse("Expect object not to be put", objectWasPut);

        Object objectGot = getCoherenceRegionAccessStrategy().get(key, getCoherenceRegion().nextTimestamp());
        assertEquals("Expect to get same object put", value, objectGot);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.lockItem().
     */
    public void testLockItem()
    {
        //TODO: lockItem is a no-op. How to assert that nothing happened?
    }

    /**
     * Tests CoherenceRegionAccessStrategy.lockRegion().
     */
    public void testLockRegion()
    {
        //TODO: how to test?
    }

    /**
     * Tests CoherenceRegionAccessStrategy.unlockItem().
     */
    public void testUnlockItem()
    {
        //TODO: unlockItem is a no-op. How to assert that nothing happened?
    }

    /**
     * Tests CoherenceRegionAccessStrategy.unlockRegion().
     */
    public void testUnlockRegion()
    {
        //TODO: how to test?
    }

    /**
     * Tests CoherenceRegionAccessStrategy.remove().
     */
    @Test
    public void testRemove()
    {
        CoherenceRegionAccessStrategy strategy = getCoherenceRegionAccessStrategy();

        Object key = "testRemove";
        Object value = "testRemove";

        boolean containsKey = strategy.getCoherenceRegion().contains(key);
        assertFalse("Empty region shouldn't contain key", containsKey);

        getCoherenceRegionAccessStrategy().remove(key);
        assertTrue("No Excepton should be thrown from removing an absent key", true);

        putFromLoad(key, value, false);
        containsKey = strategy.getCoherenceRegion().contains(key);
        assertTrue("Expect region contain key after put", containsKey);

        getCoherenceRegionAccessStrategy().remove(key);
        containsKey = strategy.getCoherenceRegion().contains(key);
        assertFalse("Expect region doesn't contain key after remove", containsKey);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.removeAll().
     */
    @Test
    public void testRemoveAll()
    {
        CoherenceRegionAccessStrategy strategy = getCoherenceRegionAccessStrategy();

        Object key = "testRemoveAll";
        Object value = "testRemoveAll";

        long cacheSize = strategy.getCoherenceRegion().getElementCountInMemory();
        assertEquals("Empty region should have size 0", 0, cacheSize);

        getCoherenceRegionAccessStrategy().removeAll();
        assertTrue("No Exception should be thrown from removeAll", true);

        putFromLoad(key, value, false);
        cacheSize = strategy.getCoherenceRegion().getElementCountInMemory();
        assertEquals("Expect cache size 1 after put", 1, cacheSize);

        getCoherenceRegionAccessStrategy().removeAll();
        cacheSize = strategy.getCoherenceRegion().getElementCountInMemory();
        assertEquals("Empty region should have size 0", 0, cacheSize);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.evict().
     */
    @Test
    public void testEvict()
    {
        CoherenceRegionAccessStrategy strategy = getCoherenceRegionAccessStrategy();

        Object key = "testEvict";
        Object value = "testEvict";

        boolean containsKey = strategy.getCoherenceRegion().contains(key);
        assertFalse("Empty region shouldn't contain key", containsKey);

        getCoherenceRegionAccessStrategy().evict(key);
        assertTrue("No Excepton should be thrown from evicting an absent key", true);

        putFromLoad(key, value, false);
        containsKey = strategy.getCoherenceRegion().contains(key);
        assertTrue("Expect region contain key after put", containsKey);

        getCoherenceRegionAccessStrategy().evict(key);
        containsKey = strategy.getCoherenceRegion().contains(key);
        assertFalse("Expect region doesn't contain key after evict", containsKey);
    }

    /**
     * Tests CoherenceRegionAccessStrategy.evictAll().
     */
    @Test
    public void testEvictAll()
    {
        CoherenceRegionAccessStrategy strategy = getCoherenceRegionAccessStrategy();

        Object key = "testEvictAll";
        Object value = "testEvictAll";

        long cacheSize = strategy.getCoherenceRegion().getElementCountInMemory();
        assertEquals("Empty region should have size 0", 0, cacheSize);

        getCoherenceRegionAccessStrategy().evictAll();
        assertTrue("No Exception should be thrown from evictAll", true);

        putFromLoad(key, value, false);
        cacheSize = strategy.getCoherenceRegion().getElementCountInMemory();
        assertEquals("Expect cache size 1 after put", 1, cacheSize);

        getCoherenceRegionAccessStrategy().evictAll();
        cacheSize = strategy.getCoherenceRegion().getElementCountInMemory();
        assertEquals("Empty region should have size 0", 0, cacheSize);
    }


    // ---- Internal

    /**
     * Puts the argument value at the argument key from a (simulated) load from database.
     *
     * @param key the key at which to put the value
     * @param value the value to put
     * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect
     *
     * @return a boolean indicating whether the value was put
     */
    private boolean putFromLoad(Object key, Object value, boolean minimalPutsInEffect)
    {
        //note we don't test the four-argument variant of putFromLoad() because it depends on the Hibernate Settings object
        //which is hard to instantiate or mock.  But the four-argument variant just funnels to the five-argument one anyway
        long txTimestamp = getCoherenceRegion().nextTimestamp();
        Object version = null;
        return getCoherenceRegionAccessStrategy().putFromLoad(key, value, txTimestamp, version, minimalPutsInEffect);
    }


}
