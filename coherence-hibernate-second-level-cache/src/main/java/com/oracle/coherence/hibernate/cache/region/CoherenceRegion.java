package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.ExtractorProcessor;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.Region;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * CoherenceRegion is an abstract superclass for classes representing different kinds of "region" in the Hibernate
 * second-level cache.  It abstracts behavior (and state) common to all types of Hibernate second-level cache region.
 *
 * Note that there is a concept (and therefore terminology) mapping between the Hibernate world and the Coherence world.
 * Hibernate uses "cache" to mean the whole, and "region" to mean a part of the whole.  Coherence uses "data grid" to
 * mean the whole, and "NamedCache" to mean a part of the whole.  So a "region" to Hibernate is a NamedCache to
 * Coherence.  Therefore CoherenceRegion is basically an Adapter, adapting the Region SPI to the NamedCache API by
 * encapsulating and delegating to a NamedCache.
 *
 * @author Randy Stafford
 */
public abstract class CoherenceRegion
implements Region
{


    // ---- Constants

    /**
     * The prefix of the names of all properties specific to this SPI implementation.
     */
    private static final String PROPERTY_NAME_PREFIX = CoherenceRegionFactory.PROPERTY_NAME_PREFIX;

    /**
    * The name of the  property specifying the lock lease duration.
    */
    public static final String LOCK_LEASE_DURATION_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "lock_lease_duration";

    /**
    * The default lock lease duration in milliseconds.
    */
    public static final int DEFAULT_LOCK_LEASE_DURATION = 60 * 1000;


    // ---- Fields

    /**
    * The lock lease timeout in milliseconds.
    */
    private final int lockLeaseDuration;

    /**
     * The NamedCache implementing this CoherenceRegion.
     */
    private NamedCache namedCache;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceRegion
     * @param properties configuration properties for this CoherenceRegion
     */
    public CoherenceRegion(NamedCache namedCache, Properties properties)
    {
        debugf("%s(%s, properties)", getClass().getName(), namedCache);
        lockLeaseDuration = (int) getDurationProperty(
                properties,
                LOCK_LEASE_DURATION_PROPERTY_NAME,
                DEFAULT_LOCK_LEASE_DURATION,
                Integer.MAX_VALUE);
        this.namedCache = namedCache;
    }


    // ---- Accessors

    /**
     * Returns the NamedCache implementing this CoherenceRegion.
     *
     * @return the NamedCache implementing this CoherenceRegion
     */
    protected NamedCache getNamedCache()
    {
        return namedCache;
    }


    // ---- interface java.lang.Object

    /**
     * {@inheritDoc}}
     */
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder(getClass().getName());
        stringBuilder.append("(");
        stringBuilder.append(namedCache.getCacheName());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }


    // ---- API: CoherenceRegionAccessStrategy support

    /**
     * Computes and returns the expiration time for a new soft lock.
     *
     * @return a long representing the expiration time for a new soft lock
     */
    public long newSoftLockExpirationTime()
    {
        return nextTimestamp() + getTimeout();
    }

    /**
     * Returns the object at the argument key in this CoherenceRegion.
     *
     * @param key the key of the sought object
     *
     * @return the Value at the argument key in this CoherenceRegion
     */
    public Value getValue(Object key)
    {
        //don't use an EntryProcessor here, because that precludes near cache hits.
        //access strategies with more strict concurrency control requirements call invoke() not getValue().
        return (Value) getNamedCache().get(key);
    }

    /**
     * Put the argument value into this CoherenceRegion at the argument key.
     *
     * @param key the key at which to put the value
     * @param value the value to put
     */
    public void putValue(Object key, Value value)
    {
        getNamedCache().invoke(key, new ConditionalPut(AlwaysFilter.INSTANCE, value));
    }

    /**
     * Evicts from this CoherenceRegion the entry at the argument key.
     *
     * @param key the key of the entry to remove
     */
    public void evict(Object key)
    {
        getNamedCache().invoke(key, new ConditionalRemove(AlwaysFilter.INSTANCE));
    }

    /**
     * Evicts all entries from this CoherenceRegion.
     */
    public void evictAll()
    {
        getNamedCache().clear();
    }

    /**
     * Locks the entire cache.
     */
    public void lockCache()
    {
        // will only work as imagined with caches of replicated topology
        getNamedCache().lock(ConcurrentMap.LOCK_ALL);
    }

    /**
     * Unlocks the entire cache.
     */
    public void unlockCache()
    {
        // will only work as imagined with caches of replicated topology
        getNamedCache().unlock(ConcurrentMap.LOCK_ALL);
    }

    /**
     * Invoke the argument EntryProcessor on the argument key and return the result of the invocation.
     *
     * @param key the key on which to invoke the EntryProcessor
     * @param entryProcessor the EntryProcessor to invoke.
     *
     * @return the Object resulting from the EntryProcessor invocation
     */
    public Object invoke(Object key, InvocableMap.EntryProcessor entryProcessor)
    {
        return getNamedCache().invoke(key, entryProcessor);
    }


    // ---- interface org.hibernate.spi.cache.Region

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        debugf("%s.getName()", this);
        return getNamedCache().getCacheName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws CacheException
    {
        debugf("%s.destroy()", this);
        getNamedCache().release();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object key)
    {
        debugf("%s.contains(%s)", this, key);
        return getNamedCache().invoke(key, new ExtractorProcessor(IdentityExtractor.INSTANCE)) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSizeInMemory()
    {
        debugf("%s.getSizeInMemory()", this);
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getElementCountInMemory()
    {
        debugf("%s.getElementCountInMemory()", this);
        return getNamedCache().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getElementCountOnDisk()
    {
        debugf("%s.getElementCountOnDisk()", this);
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map toMap()
    {
        debugf("%s.toMap()", this);
        return getNamedCache();
    }

    /**
     * This method is undocumented in Hibernate javadoc, but seems intended to return the "current" time.
     *
     * @return a millisecond clock value
     */
    @Override
    public long nextTimestamp()
    {
        debugf("%s.nextTimestamp()", this);
        return getNamedCache().getCacheService().getCluster().getTimeMillis();
    }

    /**
     * This method is undocumented in Hibernate javadoc.  Comments in the Coherence-based implementation of the
     * Hibernate 2.1 second-level cache SPI suggest the returned value is used as a lock lease duration.
     *
     * @return an int lock lease duration
     */
    @Override
    public int getTimeout()
    {
        debugf("%s.getTimeout()", this);
        // Note that this must be the same time units as getTimestamp
        return lockLeaseDuration;
    }


    // ---- Internal

    /**
    * Get a duration value in milliseconds from the argument properties or defaults, capped at a maximum value.
    *
    * @param properties the property set containing the property
    * @param propertyName the name of the property
    * @param defaultValue the default value (in milliseconds)
    * @param maxValue the maximum value (saturating, in milliseconds)
    *
    * @return a long duration value in milliseconds
    */
    protected long getDurationProperty(Properties properties, String propertyName, long defaultValue, long maxValue)
    {
        Base.azzert(maxValue >= defaultValue);
        Base.azzert(defaultValue >= 0);

        String propertyValue = properties.getProperty(propertyName, Long.toString(defaultValue));
        long duration;
        try
        {
            duration = Base.parseTime(propertyValue);
        }
        catch (Exception e)
        {
            CacheFactory.log("Error parsing duration property " + propertyName + "; provided value was " +
                              propertyValue + "; using default of " + defaultValue + " milliseconds.", 2);
            duration = defaultValue;
        }

        if (duration > maxValue)
        {
            CacheFactory.log("Capping " + propertyName + " at " + maxValue + " milliseconds.");
            duration = maxValue;
        }

        return duration;
    }


    // ---- Internal: logging support

    /**
     * Log the argument message with formatted arguments at debug severity
     *
     * @param message the message to log
     * @param arguments the arguments to the format specifiers in message
     */
    protected void debugf(String message, Object ... arguments)
    {
        CoherenceRegionFactory.debugf(message, arguments);
    }


    // ---- Nested Classes


    /**
     * A CoherenceRegion.Value is an object representing a value in Hibernate's second-level cache.
     * It holds the "actual" cache value, as well as a "version" object and a timestamp (i.e. a long)
     * used by Hibernate.
     *
     * It further keeps track of its state with respect to soft-locked-ness, and the number of
     * soft locks currently in effect on it.
     *
     * @author Randy Stafford
     */
    public static class Value
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
        private List<SoftLock> softLocks = new ArrayList<SoftLock>();

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
        public Value(Object value, Object version, long timestamp)
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

            Value value1 = (Value) someObject;

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
            for (SoftLock softLock : softLocks) stringBuilder.append(softLock).append(", ");
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
        public void addSoftLock(SoftLock softLock)
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
        public boolean isReplaceableFromLoad(long txTimestamp, Object replacementVersion, Comparator versionComparator)
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
         * A CoherenceRegion.Value.SoftLock is an object representing a "soft lock" on an entry in second-level cache.
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

                SoftLock softLock = (SoftLock) someObject;

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


}
