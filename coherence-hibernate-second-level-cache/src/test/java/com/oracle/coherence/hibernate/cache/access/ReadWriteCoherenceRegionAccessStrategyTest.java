package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A ReadWriteCoherenceRegionAccessStrategyTest is a test of ReadWriteCoherenceRegionAccessStrategy behavior.
 * For convenience an instance of a subclass (EntityReadWriteCoherenceRegionAccessStrategy) is used to test
 * the abstracted behavior.
 *
 * @author Randy Stafford
 */
public class ReadWriteCoherenceRegionAccessStrategyTest
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
        return newEntityReadWriteCoherenceRegionAccessStrategy();
    }


    // ---- Test cases

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.get() when the sought entry is absent.
     */
    @Test
    public void testGetEntryAbsent()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();
        Object key = "testGetEntryAbsent";
        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        Object valueGot = getCoherenceRegionAccessStrategy().get(key, txTimestamp);
        assertNull("Expect null getting from empty cache", valueGot);
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.get() when the sought entry is present and not soft-locked.
     */
    @Test
    public void testGetEntryPresentNotLocked()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testGetEntryPresentNotLocked";
        Object valuePut = "testGetEntryPresentNotLocked";
        putFromLoad(key, valuePut, false);

        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        Object valueGot = getCoherenceRegionAccessStrategy().get(key, txTimestamp);
        assertNotNull("Expect non-null value", valueGot);
        assertEquals("Expect got same value put", valueGot, valuePut);
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.get() when the sought entry is present and soft-locked.
     */
    @Test
    public void testGetEntryPresentAndLocked()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testGetEntryPresentAndLocked";
        Object valuePut = "testGetEntryPresentAndLocked";
        Object version = null;
        putFromLoad(key, valuePut, false);
        accessStrategy.lockItem(key, version);

        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        Object valueGot = getCoherenceRegionAccessStrategy().get(key, txTimestamp);
        assertNull("Expect null getting locked value", valueGot);
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.putFromLoad() when the entry being put is absent.
     */
    @Test
    public void testPutFromLoadEntryAbsent()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testPutFromLoadEntryAbsent";
        Object valuePut = "testPutFromLoadEntryAbsent";
        boolean objectWasCached = putFromLoad(key, valuePut, false);
        assertTrue("Expect successful putFromLoad when entry absent", objectWasCached);

        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        assertEquals("Expect got same object put", valuePut, accessStrategy.get(key, txTimestamp));
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.putFromLoad() when the entry being put is already present
     * and "minimal puts" is in effect.
     */
    @Test
    public void testPutFromLoadEntryPresentMinimalPuts()
    {
        Object key = "testPutFromLoadEntryPresentMinimalPuts";
        Object valuePut = "testPutFromLoadEntryPresentMinimalPuts";
        putFromLoad(key, valuePut, false);

        boolean objectWasCached = putFromLoad(key, valuePut, true);
        assertFalse("Expect no putFromLoad when entry present and minimal puts", objectWasCached);
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.putFromLoad() when the entry being put is already present and
     * "minimal puts" is not in effect and the entry was never locked.
     */
    @Test
    public void testPutFromLoadEntryPresentNotMinimalPutsNeverLocked()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testPutFromLoadEntryPresentNotMinimalPutsNeverLocked";
        Object valuePut = "testPutFromLoadEntryPresentNotMinimalPutsNeverLocked";
        putFromLoad(key, valuePut, false);

        boolean objectWasCached = putFromLoad(key, valuePut, false);
        assertTrue("Expect successful putFromLoad when entry present and not minimal puts", objectWasCached);

        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        assertEquals("Expect got same object put", valuePut, accessStrategy.get(key, txTimestamp));
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.putFromLoad() when the entry being put is already present and
     * "minimal puts" is not in effect and the entry was locked but the locks were released before the putFromLoad.
     */
    @Test
    public void testPutFromLoadEntryPresentNotMinimalPutsLocksReleased()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testPutFromLoadEntryPresentNotMinimalPutsLocksReleased";
        Object valuePut = "testPutFromLoadEntryPresentNotMinimalPutsLocksReleased";
        putFromLoad(key, valuePut, false);

        Object version = null;
        CoherenceRegion.Value.SoftLock softLock = (CoherenceRegion.Value.SoftLock) accessStrategy.lockItem(key, version);
        accessStrategy.unlockItem(key, softLock);

        boolean objectWasCached = putFromLoad(key, valuePut, false);
        assertTrue("Expect successful putFromLoad when entry present and not minimal puts and locks released", objectWasCached);

        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        assertEquals("Expect got same object put", valuePut, accessStrategy.get(key, txTimestamp));
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.putFromLoad() when the entry being put is already present and
     * "minimal puts" is in effect and the entry was locked but the locks have expired.
     */
    @Test
    public void testPutFromLoadEntryPresentNotMinimalPutsLocksNotExpired()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testPutFromLoadEntryPresentNotMinimalPutsLocksNotExpired";
        Object valuePut = "testPutFromLoadEntryPresentNotMinimalPutsLocksNotExpired";
        putFromLoad(key, valuePut, false);

        Object version = null;
        CoherenceRegion.Value.SoftLock softLock = (CoherenceRegion.Value.SoftLock) accessStrategy.lockItem(key, version);
        long expirationTime = softLock.getExpirationTime();

        boolean objectWasCached = putFromLoad(key, valuePut, false, expirationTime - 1000L);
        assertFalse("Expect no putFromLoad when entry present and locked", objectWasCached);
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.putFromLoad() when the entry being put is already present and
     * "minimal puts" is in effect and the entry was locked but the locks have expired.
     */
    @Test
    public void testPutFromLoadEntryPresentNotMinimalPutsLocksExpired()
    {
        EntityRegionAccessStrategy accessStrategy = getEntityRegionAccessStrategy();

        Object key = "testPutFromLoadEntryPresentNotMinimalPutsLocksExpired";
        Object valuePut = "testPutFromLoadEntryPresentNotMinimalPutsLocksExpired";
        putFromLoad(key, valuePut, false);

        Object version = null;
        CoherenceRegion.Value.SoftLock softLock = (CoherenceRegion.Value.SoftLock) accessStrategy.lockItem(key, version);
        long expirationTime = softLock.getExpirationTime();

        boolean objectWasCached = putFromLoad(key, valuePut, false, expirationTime + 1000L);
        assertTrue("Expect successful putFromLoad when entry present, not minimal puts, locks expired", objectWasCached);

        long txTimestamp = accessStrategy.getRegion().nextTimestamp();
        assertEquals("Expect got same object put", valuePut, accessStrategy.get(key, txTimestamp));
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.lockItem().
     */
    @Test
    public void testLockItem()
    {
        EntityReadWriteCoherenceRegionAccessStrategy strategy = (EntityReadWriteCoherenceRegionAccessStrategy) getEntityRegionAccessStrategy();

        Object key = "testLockItem";
        Object version = null;
        strategy.lockItem(key, version);

        assertTrue("Expect entry to be present after being locked", strategy.getRegion().contains(key));
        assertTrue("Expect value to be locked", strategy.getCoherenceRegion().getValue(key).isSoftLocked());
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.unlockItem().
     */
    @Test
    public void testUnlockItem()
    {
        EntityReadWriteCoherenceRegionAccessStrategy strategy = (EntityReadWriteCoherenceRegionAccessStrategy) getEntityRegionAccessStrategy();

        Object key = "testUnlockItem";
        Object version = null;
        SoftLock softLock = strategy.lockItem(key, version);

        strategy.unlockItem(key, softLock);
        assertTrue("Expect entry to be present", strategy.getRegion().contains(key));
        assertFalse("Expect value to be not locked", strategy.getCoherenceRegion().getValue(key).isSoftLocked());
    }

    /**
     * Tests ReadWriteCoherenceRegionAccessStrategy.unlockItem() when concurrent SoftLocks exist.
     */
    @Test
    public void testUnlockItemConcurrent()
    {
        EntityReadWriteCoherenceRegionAccessStrategy strategy = (EntityReadWriteCoherenceRegionAccessStrategy) getEntityRegionAccessStrategy();

        Object key = "testUnlockItem";
        Object version = null;
        SoftLock softLock = strategy.lockItem(key, version);
        strategy.lockItem(key, version);

        strategy.unlockItem(key, softLock);
        assertTrue("Expect entry to be present", strategy.getRegion().contains(key));
        assertTrue("Expect value to still be locked", strategy.getCoherenceRegion().getValue(key).isSoftLocked());
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
        return putFromLoad(key, value, minimalPutsInEffect, getCoherenceRegion().nextTimestamp());
    }

    /**
     * Puts the argument value at the argument key from a (simulated) load from database.
     *
     * @param key the key at which to put the value
     * @param value the value to put
     * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect
     * @param txTimestamp the timestamp of the putFromLoad transaction
     *
     * @return a boolean indicating whether the value was put
     */
    private boolean putFromLoad(Object key, Object value, boolean minimalPutsInEffect, long txTimestamp)
    {
        //note we don't test the four-argument variant of putFromLoad() because it depends on the Hibernate Settings object
        //which is hard to instantiate or mock.  But the four-argument variant just funnels to the five-argument one anyway
        Object version = null;
        return getCoherenceRegionAccessStrategy().putFromLoad(key, value, txTimestamp, version, minimalPutsInEffect);
    }


}
