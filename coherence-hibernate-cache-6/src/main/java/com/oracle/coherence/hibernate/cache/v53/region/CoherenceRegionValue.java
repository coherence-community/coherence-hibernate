/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.region;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A CoherenceRegion.CoherenceRegionValue is an object representing a value in Hibernate's second-level cache.
 * It holds the "actual" cache value, as well as a "version" object and a timestamp (i.e. a long)
 * used by Hibernate.
 *
 * It further keeps track of its state with respect to soft-locked-ness, and the number of
 * soft locks currently in effect on it.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceRegionValue
implements Serializable
{

    // ---- Constants

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = 3748411110362855303L;


    // ---- Fields

    /**
     * A List of SoftLocks added to this cache value.
     */
    private List<CoherenceRegionValue.SoftLock> softLocks = new ArrayList<CoherenceRegionValue.SoftLock>();

    /**
     * The time at which all soft locks in effect on this cache value will have expired.
     */
    private long timeOfSoftLockExpiration = 0L;

    /**
     * The time at which the last soft lock in effect on this cache value was released.
     */
    private long timeOfSoftLockRelease = 0L;

    /**
     * The "timestamp" of the actual cache value.
     */
    private long timestamp;

    /**
    * The "actual" value in the cache.
    */
    private Object value;

    /**
    * The "version" of the actual cache value.
    */
    private Object version;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param value the actual value in this cache value
     * @param version the version of the actual value in this cache value
     * @param timestamp the timestamp of the actual value in this cache value
     */
    public CoherenceRegionValue(Object value, Object version, long timestamp)
    {
        this.value = value;
        this.version = version;
        this.timestamp = timestamp;
    }


    // ---- Accessors

    /**
     * Returns the "actual" value in this cache value.
     *
     * @return the Object that is the "actual" value in this cache value
     */
    public Object getValue()
    {
        return value;
    }


    /**
     * Returns the "version" of the "actual" value in this cache value.
     *
     * @return the Object that is the "version" of the "actual" value in this cache value
     */
    public Object getVersion()
    {
        return version;
    }


    /**
     * Returns the "timestamp" of the "actual" value in this cache value.
     *
     * @return the long "timestamp" of the "actual" value in this cache value
     */
    public long getTimestamp()
    {
        return timestamp;
    }


    // ---- interface java.lang.Object


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object someObject)
    {
        if (this == someObject) return true;
        if (someObject == null || getClass() != someObject.getClass()) return false;

        CoherenceRegionValue value1 = (CoherenceRegionValue) someObject;

        if (timestamp != value1.timestamp) return false;
        if (!value.equals(value1.value)) return false;
        if (version != null ? !version.equals(value1.version) : value1.version != null) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + value.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder(getClass().getName());
        stringBuilder.append("(value=").append(value);
        stringBuilder.append(", version=").append(version);
        stringBuilder.append(", timestamp=").append(timestamp);
        stringBuilder.append(", timeOfSoftLockExpiration=").append(timeOfSoftLockExpiration);
        stringBuilder.append(", timeOfSoftLockRelease=").append(timeOfSoftLockRelease);
        stringBuilder.append(", softLocks=(");
        for (CoherenceRegionValue.SoftLock softLock : softLocks) stringBuilder.append(softLock).append(", ");
        if (softLocks.size() > 0) stringBuilder.setLength(stringBuilder.length() - 2);
        stringBuilder.append("))");
        return stringBuilder.toString();
    }


    // ---- API

    /**
     * Adds a SoftLock to this cache value.
     *
     * @param softLock the SoftLock to add
     */
    public void addSoftLock(CoherenceRegionValue.SoftLock softLock)
    {
        softLocks.add(softLock);
        //it stands to reason that the most recently-added SoftLock will have the farthest expiration time
        timeOfSoftLockExpiration = softLock.getExpirationTime();
    }

    /**
     * Returns a boolean indicating whether this cache value is replaceable from database load.
     *
     * @param txTimestamp From Hibernate javadoc, "a timestamp prior to the transaction start time" [where "the transaction" loaded the potential replacement value from database]
     * @param replacementVersion the version of the would-be replacement
     * @param versionComparator a Comparator for comparing entity versions
     *
     * @return a boolean indicating whether this cache value is replaceable from database load
     */
    public boolean isReplaceableFromLoad(long txTimestamp, Object replacementVersion, Comparator<Object> versionComparator)
    {
        return isSoftLocked() ?
                wereSoftLocksExpiredBefore(txTimestamp) :
                (version == null) ?
                        wereSoftLocksReleasedBefore(txTimestamp) :
                        versionComparator.compare(version, replacementVersion) < 0;
    }

    /**
     * Returns a boolean indicating whether this cache value is not currently soft-locked.
     *
     * @return a boolean indicating whether this cache value is not currently soft-locked
     */
    public boolean isNotSoftLocked()
    {
        return !isSoftLocked();
    }

    /**
     * Returns a boolean indicating whether this cache value is currently soft-locked.
     *
     * @return a boolean indicating whether this cache value is currently soft-locked
     */
    public boolean isSoftLocked()
    {
        return softLocks.size() > 0;
    }

    /**
     * Attempts to release the argument SoftLock on this cache value.
     * Has no effect if soft locks are not released in the same order in which they were acquired.
     *
     * @param softLock the SoftLock whose release to attempt
     * @param timeOfRelease the time at which the SoftLock was released
     */
    public void releaseSoftLock(org.hibernate.cache.spi.access.SoftLock softLock, long timeOfRelease)
    {
        softLocks.remove(softLock);
        if (isNotSoftLocked()) timeOfSoftLockRelease = timeOfRelease;
    }


    // ---- Internal

    /**
     * Returns a boolean indicating whether all soft locks on this cache value were expired before the argument time.
     *
     * @param someTime the time before which it is asked whether all soft locks were expired
     *
     * @return a boolean indicating whether all soft locks on this cache value were expired before the argument time
     */
    private boolean wereSoftLocksExpiredBefore(long someTime)
    {
        return timeOfSoftLockExpiration < someTime;
    }

    /**
     * Returns a boolean indicating whether all soft locks on this cache value were released before the argument time.
     *
     * @param someTime the time before which it is asked whether all soft locks were released
     *
     * @return a boolean indicating whether all soft locks on this cache value were released before the argument time
     */
    private boolean wereSoftLocksReleasedBefore(long someTime)
    {
        return timeOfSoftLockRelease < someTime;
    }


    // ---- Nested Classes

    /**
     * A CoherenceRegion.CoherenceRegionValue.SoftLock is an object representing a "soft lock" on an entry in second-level cache.
     *
     * @author Randy Stafford
     */
    public static class SoftLock
    implements Serializable, org.hibernate.cache.spi.access.SoftLock
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = -1171771458206273933L;


        // ---- Fields

        /**
         * A unique identifier for the component that acquired this SoftLock.  A SoftLock may only be released
         * by the component that acquired it.  In practice this component appears to be a RegionAccessStrategy,
         * which appears to have the same lifecycle as a Hibernate SessionFactory.
         */
        private UUID acquirerId;

        /**
         * The time at which this SoftLock expires.
         */
        private long expirationTime;

        /**
         * The sequence number of this SoftLock with respect to its acquirer.  The same acquirer may acquire
         * multiple SoftLocks over time or "concurrently", even on the same cache key, so a sequence number *and*
         * acquirer ID are needed to equate two SoftLocks.
         */
        private long sequenceNumber;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param acquirerId a unique identifier of the component that acquired this SoftLock
         * @param sequenceNumber the sequenceNumber of this SoftLock with respect to its acquirer
         * @param expirationTime the time at which this SoftLock expires
         */
        public SoftLock(UUID acquirerId, long sequenceNumber, long expirationTime)
        {
            this.acquirerId = acquirerId;
            this.expirationTime = expirationTime;
            this.sequenceNumber = sequenceNumber;
        }


        // ---- Accessors

        /**
         * Returns this SoftLock's expiration time.
         *
         * @return the long that is this SoftLock's expiration time
         */
        public long getExpirationTime()
        {
            return expirationTime;
        }


        // ---- interface java.lang.Object


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object someObject)
        {
            if (this == someObject) return true;
            if (someObject == null || getClass() != someObject.getClass()) return false;

            CoherenceRegionValue.SoftLock softLock = (CoherenceRegionValue.SoftLock) someObject;

            if (sequenceNumber != softLock.sequenceNumber) return false;
            if (!acquirerId.equals(softLock.acquirerId)) return false;

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            int result = acquirerId.hashCode();
            result = 31 * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            StringBuilder stringBuilder = new StringBuilder(getClass().getName());
            stringBuilder.append("(acquirerId=").append(acquirerId);
            stringBuilder.append(", sequenceNumber=").append(sequenceNumber);
            stringBuilder.append(", expirationTime=").append(expirationTime);
            return stringBuilder.toString();
        }


    }


}
