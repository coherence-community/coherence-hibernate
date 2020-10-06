package com.oracle.coherence.hibernate.cache.region;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * A CoherenceRegionValueTest is test of CoherenceRegion.Value behavior.
 *
 * @author Randy Stafford
 */
public class CoherenceRegionValueTest
{


    // ---- Fields

    /**
     * A sequence number for soft locks used in this test.
     * Monotonically increasing characteristic enforced by usage.
     */
    private AtomicLong softLockSequenceNumber = new AtomicLong(0L);

    /**
     * A unique identifier of this test.
     */
    private UUID uuid = UUID.randomUUID();

    /**
     * The CoherenceRegion.Value in the test fixture.
     */
    private CoherenceRegion.Value value;


    // ---- Fixture lifecycle

    /**
     * Set up the test fixture.
     */
    @Before
    public void setUp()
    {
        Object object = "testValue";
        Object version = null;
        long timestamp = System.currentTimeMillis();
        value = new CoherenceRegion.Value(object, version, timestamp);
    }

    /**
     * Tear down the test fixture.
     */
    @After
    public void tearDown()
    {
        value = null;
    }


    // ---- Test cases

    /**
     * Tests CoherenceRegion.Value.toString().
     */
    @Test
    public void testToString()
    {
        //this is mainly to ensure no NPEs, infinite loops, etc. in toString()
        String toString = value.toString();
        assertNotNull("Expect non-null toString()", toString);
        assertTrue("Expect non-empty toString()", toString.length() > 0);
    }

    /**
     * Tests CoherenceRegion.Value.addSoftLock().
     */
    @Test
    public void testAddSoftLock()
    {
        assertFalse("Expect new value not soft locked", value.isSoftLocked());
        assertTrue("Expect new value not soft locked", value.isNotSoftLocked());

        addSoftLock();

        assertFalse("Expect value soft locked", value.isNotSoftLocked());
        assertTrue("Expect value soft locked", value.isSoftLocked());
    }

    /**
     * Tests CoherenceRegion.Value.releaseSoftLock().
     */
    @Test
    public void testReleaseSoftLock()
    {
        CoherenceRegion.Value.SoftLock softLock = addSoftLock();

        assertFalse("Expect value soft locked", value.isNotSoftLocked());
        assertTrue("Expect value soft locked", value.isSoftLocked());

        value.releaseSoftLock(softLock, System.currentTimeMillis());

        assertFalse("Expect value not soft locked", value.isSoftLocked());
        assertTrue("Expect value not soft locked", value.isNotSoftLocked());

    }

    /**
     * Tests CoherenceRegion.Value.releaseSoftLock() when the value is concurrently soft-locked.
     */
    @Test
    public void testReleaseSoftLockConcurrent()
    {
        CoherenceRegion.Value.SoftLock softLock = addSoftLock();
        CoherenceRegion.Value.SoftLock softLock2 = addSoftLock();

        value.releaseSoftLock(softLock, System.currentTimeMillis());

        assertFalse("Expect value soft locked", value.isNotSoftLocked());
        assertTrue("Expect value soft locked", value.isSoftLocked());

        value.releaseSoftLock(softLock2, System.currentTimeMillis());

        assertFalse("Expect value not soft locked", value.isSoftLocked());
        assertTrue("Expect value not soft locked", value.isNotSoftLocked());

    }

    /**
     * Tests CoherenceRegion.Value.isReplaceableFromLoad() when the value has a null version and was never soft locked.
     */
    @Test
    public void testIsReplaceableFromLoadNullVersionNeverLocked()
    {
        long txTimestamp = System.currentTimeMillis();
        Object replacementVersion = null;
        Comparator versionComparator = null;
        boolean isReplaceableFromLoad = value.isReplaceableFromLoad(txTimestamp, replacementVersion, versionComparator);


        //We expect the value to be replaceable from load in this scenario because soft locks were released before the
        //passed-in txTimestamp (since timeOfSoftLockRelease initializes to 0L).
        assertTrue("Expect never-locked value replaceable from load", isReplaceableFromLoad);
    }

    /**
     * Tests CoherenceRegion.Value.isReplaceableFromLoad() when the value has a null version and was soft locked
     * but the locks were released.
     */
    @Test
    public void testIsReplaceableFromLoadNullVersionLocksReleased()
    {
        CoherenceRegion.Value.SoftLock softLock = addSoftLock();
        long timeOfLockRelease = System.currentTimeMillis();
        value.releaseSoftLock(softLock, timeOfLockRelease);

        long txTimestamp = timeOfLockRelease + 1000L;
        Object replacementVersion = null;
        Comparator versionComparator = null;
        boolean isReplaceableFromLoad = value.isReplaceableFromLoad(txTimestamp, replacementVersion, versionComparator);


        //We expect the value to be replaceable from load in this scenario because soft locks were released before txTimestamp
        assertTrue("Expect lock-released value replaceable from load", isReplaceableFromLoad);
    }

    /**
     * Tests CoherenceRegion.Value.isReplaceableFromLoad() when the value is soft locked and the soft locks aren't expired.
     */
    @Test
    public void testIsReplaceableFromLoadLocksNotExpired()
    {
        CoherenceRegion.Value.SoftLock softLock = addSoftLock();

        long txTimestamp = System.currentTimeMillis();
        Object replacementVersion = null;
        Comparator versionComparator = null;
        boolean isReplaceableFromLoad = value.isReplaceableFromLoad(txTimestamp, replacementVersion, versionComparator);

        //We expect the value to NOT be replaceable from load in this scenario because it is locked and the lock expires
        //AFTER the passed-in txTimestamp.
        assertTrue("Expect expiration time in the future", txTimestamp < softLock.getExpirationTime());
        assertFalse("Expect unlocked value NOT replaceable from load", isReplaceableFromLoad);
    }

    /**
     * Tests CoherenceRegion.Value.isReplaceableFromLoad() when the value is soft locked and the soft locks are expired.
     */
    @Test
    public void testIsReplaceableFromLoadLocksExpired()
    {
        CoherenceRegion.Value.SoftLock softLock = addSoftLock();

        long txTimestamp = softLock.getExpirationTime() + 1000L;
        Object replacementVersion = null;
        Comparator versionComparator = null;
        boolean isReplaceableFromLoad = value.isReplaceableFromLoad(txTimestamp, replacementVersion, versionComparator);

        //We expect the value to be replaceable from load in this scenario because it is locked but the lock expired
        //before the passed-in txTimestamp.
        assertTrue("Expect expired-locked value replaceable from load", isReplaceableFromLoad);
    }


    // ---- Internal

    /**
     * Adds a soft lock to the value in the test fixture.
     *
     * @return the CoherenceRegion.Value.SoftLock that was added.
     */
    private CoherenceRegion.Value.SoftLock addSoftLock()
    {
        long sequenceNumber = softLockSequenceNumber.incrementAndGet();
        long expirationTime = System.currentTimeMillis() + CoherenceRegion.DEFAULT_LOCK_LEASE_DURATION;
        CoherenceRegion.Value.SoftLock softLock = new CoherenceRegion.Value.SoftLock(uuid, sequenceNumber, expirationTime);
        value.addSoftLock(softLock);
        return softLock;
    }


}
