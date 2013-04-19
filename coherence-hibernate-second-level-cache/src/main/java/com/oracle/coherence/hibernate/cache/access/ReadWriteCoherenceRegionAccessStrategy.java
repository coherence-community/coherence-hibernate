package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import com.oracle.coherence.hibernate.cache.region.CoherenceTransactionalDataRegion;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A ReadWriteCoherenceRegionAccessStrategy is CoherenceRegionAccessStrategy
 * implementing the "read-write" cache concurrency strategy as defined by Hibernate.
 *
 * @author Randy Stafford
 */
public class ReadWriteCoherenceRegionAccessStrategy<T extends CoherenceTransactionalDataRegion>
extends CoherenceRegionAccessStrategy<T>
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceRegion the CoherenceRegion for this ReadWriteCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public ReadWriteCoherenceRegionAccessStrategy(T coherenceRegion, Settings settings)
    {
        super(coherenceRegion, settings);
    }


    // ---- interface org.hibernate.cache.spi.access.RegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key, long txTimestamp) throws CacheException
    {
        debugf("%s.get(%s, %s)", this, key, txTimestamp);
        return getCoherenceRegion().invoke(key, new GetProcessor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.hibernate.cache.spi.access.SoftLock lockItem(Object key, Object version) throws CacheException
    {
        debugf("%s.lockItem(%s, %s, %s)", this, key, version);
        CoherenceRegion.Value valueIfAbsent = newCacheValue(null, version);
        CoherenceRegion.Value.SoftLock newSoftLock = newSoftLock();
        SoftLockItemProcessor processor = new SoftLockItemProcessor(valueIfAbsent, newSoftLock);
        getCoherenceRegion().invoke(key, processor);
        return newSoftLock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
    throws CacheException
    {
        debugf("%s.putFromLoad(%s, %s, %s, %s, %s)", this, key, value, txTimestamp, version, minimalPutOverride);
        CoherenceRegion.Value newCacheValue = newCacheValue(value, version);
        Comparator versionComparator = getCoherenceRegion().getCacheDataDescription().getVersionComparator();
        PutFromLoadProcessor processor = new PutFromLoadProcessor(minimalPutOverride, txTimestamp, newCacheValue, versionComparator);
        return (Boolean) getCoherenceRegion().invoke(key, processor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlockItem(Object key, org.hibernate.cache.spi.access.SoftLock lock) throws CacheException
    {
        debugf("%s.unlockItem(%s, %s, %s)", this, key, lock);
        SoftUnlockItemProcessor processor = new SoftUnlockItemProcessor(lock, getCoherenceRegion().nextTimestamp());
        getCoherenceRegion().invoke(key, processor);
    }


    // ---- Subclass interface

    /**
     * Coherence-based implementation of behavior common to:
     * 1. org.hibernate.cache.spi.access.EntityRegionAccessStrategy.afterInsert(Object key, Object value, Object version) and
     * 2. org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy.afterInsert(Object key, Object value).
     *
     * The only difference in implementation is that the cache value in a NaturalIdRegion will have a null version object.
     *
     * @param key the key at which to insert a value
     * @param value the value to insert
     *
     * @return a boolean indicating whether cache contents were modified
     */
    protected boolean afterInsert(Object key, CoherenceRegion.Value value)
    {
        AfterInsertProcessor afterInsertProcessor = new AfterInsertProcessor(value);
        return (Boolean) getCoherenceRegion().invoke(key, afterInsertProcessor);
    }

    /**
     * Coherence-based implementation of behavior common to:
     * 1. org.hibernate.cache.spi.access.EntityRegionAccessStrategy.afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) and
     * 2. org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy.afterUpdate(Object key, Object value, SoftLock lock).
     *
     * The only difference in implementation is that the cache value in a NaturalIdRegion will have a null version object.
     *
     * @param key the key at which to insert a value
     * @param value the value to insert
     * @param softLock the softLock acquired in an earlier lockItem call with the argument key
     *
     * @return a boolean indicating whether cache contents were modified
     */
    protected boolean afterUpdate(Object key, CoherenceRegion.Value value, SoftLock softLock)
    {
        long timeOfSoftLockRelease = getCoherenceRegion().nextTimestamp();
        AfterUpdateProcessor afterUpdateProcessor = new AfterUpdateProcessor(value, softLock, timeOfSoftLockRelease);
        return (Boolean) getCoherenceRegion().invoke(key, afterUpdateProcessor);
    }


    // ---- Internal: factory

    /**
     * Returns a new SoftLock.
     *
     * @return a SoftLock newly constructed
     */
    private CoherenceRegion.Value.SoftLock newSoftLock()
    {
        long lockExpirationTime = getCoherenceRegion().newSoftLockExpirationTime();
        return new CoherenceRegion.Value.SoftLock(getUuid(), nextSoftLockSequenceNumber(), lockExpirationTime);
    }


    // ---- Nested Classes

    /**
     * A ReadWriteCoherenceRegionAccessStrategy.AfterInsertProcessor is an EntryProcessor
     * responsible for inserting a value into cache if none is present, and returning a boolean
     * indicating whether it did so, consistent with the expected behavior of a read-write cache
     * access strategy's afterInsert() method.
     *
     * We move this behavior into the grid for efficient concurrency control.
     *
     * @author Randy Stafford
     */
    private static class AfterInsertProcessor
    extends AbstractProcessor
    implements Serializable
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = 2326579233150319530L;


        // ---- Fields

        /**
         * The cache value for use by this AfterInsertProcessor.
         */
        private CoherenceRegion.Value cacheValue;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param cacheValue the cache value for use by this AfterInsertProcessor
         */
        public AfterInsertProcessor(CoherenceRegion.Value cacheValue)
        {
            this.cacheValue = cacheValue;
        }


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object process(InvocableMap.Entry entry)
        {
            if (entry.isPresent())
            {
                return false;
            }
            else
            {
                entry.setValue(cacheValue);
                return true;
            }
        }


    }


    /**
     * A ReadWriteCoherenceRegionAccessStrategy.AfterUpdateProcessor is an EntryProcessor
     * responsible for updating a value in a second-level cache and returning a boolean indicating
     * whether it did so, consistent with the expected behavior of a read-write cache access strategy's
     * afterUpdate() method.
     *
     * We move this behavior into the grid for efficient concurrency control.
     *
     * @author Randy Stafford
     */
    private static class AfterUpdateProcessor
    extends AbstractProcessor
    implements Serializable
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = 2890338818667968735L;


        // ---- Fields

        /**
         * A cache value to potentially replace the present one.
         */
        private CoherenceRegion.Value replacementValue;

        /**
         * A SoftLock presumably acquired by a previous lockItem call on the entry being processed
         */
        private SoftLock softLock;

        /**
         * The potential time at which all locks on the entry being processed were released.
         */
        private long timeOfSoftLockRelease;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param replacementValue a cache value to potentially replace the present one
         * @param softLock a SoftLock presumably acquired by a previous lockItem call on the entry being processed
         * @param timeOfSoftLockRelease the potential time at which all locks on the entry being processed were released
         */
        public AfterUpdateProcessor(CoherenceRegion.Value replacementValue, SoftLock softLock, long timeOfSoftLockRelease)
        {
            this.replacementValue = replacementValue;
            this.softLock = softLock;
            this.timeOfSoftLockRelease = timeOfSoftLockRelease;
        }


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object process(InvocableMap.Entry entry)
        {
            if (entry.isPresent())
            {
                CoherenceRegion.Value cacheValue = (CoherenceRegion.Value) entry.getValue();
                cacheValue.releaseSoftLock(softLock, timeOfSoftLockRelease);
                if (cacheValue.isSoftLocked())
                {
                    //The cache value being processed was soft-locked concurrently by multiple Hibernate transactions.
                    //Under this condition we will not replace the cache value with the updated one.
                    //But we need to save the mutation to the present value's state (i.e. the release of a soft lock).
                    entry.setValue(cacheValue);
                    return false;
                }
                else
                {
                    //The cache value was soft-locked by only one Hibernate transaction.
                    //Under this condition we can replace it with the updated one.
                    entry.setValue(replacementValue);
                    return true;
                }
            }
            else
            {
                //Some Hibernate transaction is trying to update a cache value that is not present.
                //Normally we would expect it to be present, I assume, as the result of a previous putFromLoad or afterInsert call.
                //Perhaps it got evicted in the meantime, either by application code or by cache configuration.
                //In any case, we will not modify cache contents under this condition.
                //Perhaps the value whose presence was expected will but put back into cache by a future putFromLoad call.
                return false;
            }
        }


    }


    /**
     * A ReadWriteCoherenceRegionAccessStrategy.GetProcessor is an EntryProcessor
     * for getting an entity in second-level cache.  It returns null if the cache value
     * is soft-locked, thereby forcing Hibernate to read from the database.
     *
     * @author Randy Stafford
     */
    private static class GetProcessor
    extends AbstractProcessor
    implements Serializable
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = 2359701955887239611L;


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */

        @Override
        public Object process(InvocableMap.Entry entry)
        {
            if (!entry.isPresent()) return null;
            CoherenceRegion.Value cacheValue = (CoherenceRegion.Value) entry.getValue();
            if (cacheValue.isSoftLocked()) return null;
            return cacheValue.getValue();
        }


    }


    /**
     * A ReadWriteCoherenceRegionAccessStrategy.PutFromLoadProcessor is an EntryProcessor
     * responsible for putting a value in a second-level cache that was just loaded from database,
     * and returning a boolean indicating whether it did so, consistent with the expected behavior
     * of a cache access strategy's putFromLoad() method.
     *
     * We move this behavior into the grid for efficient concurrency control.
     *
     * @author Randy Stafford
     */
    private static class PutFromLoadProcessor
    extends AbstractProcessor
    implements Serializable
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = -3993308461928039511L;


        // ---- Fields

        /**
         * A flag indicating whether "minimal puts" is in effect for Hibernate.
         */
        private boolean minimalPutsInEffect;

        /**
         * The replacement cache value in this PutFromLoadProcessor.
         */
        private CoherenceRegion.Value replacementValue;

        /**
         * From Hibernate javadoc, "a timestamp prior to the transaction start time"
         * [where "the transaction" loaded the potential replacement value from database].
         */
        private long txTimestamp;

        /**
         * A comparator for comparing actual value versions.
         */
        private Comparator versionComparator;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect for Hibernate
         * @param txTimestamp From Hibernate javadoc, "a timestamp prior to the transaction start time" [where "the transaction" loaded the potential replacement value from database]
         * @param replacementValue the replacement cache value in this PutFromLoadProcessor
         * @param versionComparator a Comparator for comparing actual value versions
         */
        private PutFromLoadProcessor(boolean minimalPutsInEffect, long txTimestamp, CoherenceRegion.Value replacementValue, Comparator versionComparator)
        {
            this.minimalPutsInEffect = minimalPutsInEffect;
            this.txTimestamp = txTimestamp;
            this.replacementValue = replacementValue;
            this.versionComparator = versionComparator;
        }


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object process(InvocableMap.Entry entry)
        {
            boolean isReplaceable = true;
            if (entry.isPresent())
            {
                if (minimalPutsInEffect) return false;
                CoherenceRegion.Value presentValue = (CoherenceRegion.Value) entry.getValue();
                isReplaceable = presentValue.isReplaceableFromLoad(txTimestamp, replacementValue.getVersion(), versionComparator);
            }
            if (isReplaceable) entry.setValue(replacementValue);
            return isReplaceable;
        }


    }


    /**
     * A ReadWriteCoherenceRegionAccessStrategy.SoftLockItemProcessor is an EntryProcessor
     * responsible for "soft locking" a cache entry and returning an instance of
     * org.hibernate.cache.spi.access.SoftLock.
     *
     * We move this behavior into the grid for efficient concurrency control.
     *
     * @author Randy Stafford
     */
    private static class SoftLockItemProcessor
    extends AbstractProcessor
    implements Serializable
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = 5452465432039772596L;


        // ---- Fields

        /**
         * The SoftLock to be added to the cache value.
         */
        private CoherenceRegion.Value.SoftLock softLock;

        /**
         * The cache value to soft lock in case there is no cache value already present.
         */
        private CoherenceRegion.Value valueIfAbsent;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param valueIfAbsent the cache value to soft lock in case there is no cache value already present
         * @param softLock the SoftLock to be added to the cache value
         */
        private SoftLockItemProcessor(CoherenceRegion.Value valueIfAbsent, CoherenceRegion.Value.SoftLock softLock)
        {
            this.valueIfAbsent = valueIfAbsent;
            this.softLock = softLock;
        }


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object process(InvocableMap.Entry entry)
        {
            CoherenceRegion.Value cacheValue = entry.isPresent() ?
                    (CoherenceRegion.Value) entry.getValue() :
                    valueIfAbsent;
            cacheValue.addSoftLock(softLock);
            entry.setValue(cacheValue);
            return null;
        }


    }


    /**
     * A ReadWriteCoherenceRegionAccessStrategy.SoftUnlockItemProcessor is an EntryProcessor
     * responsible for releasing a previously-acquired "soft lock" on a cache entry.
     *
     * We move this behavior into the grid for efficient concurrency control.
     *
     * @author Randy Stafford
     */
    private static class SoftUnlockItemProcessor
    extends AbstractProcessor
    implements Serializable
    {


        // ---- Constants

        /**
         * An identifier of this class's version for serialization purposes.
         */
        private static final long serialVersionUID = 8996659062190093054L;


        // ---- Fields

        /**
         * The SoftLock which is being released.
         */
        private SoftLock softLock;

        /**
         * The time at which the SoftLock was released.
         */
        private long timeOfRelease;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param softLock the SoftLock which is being released
         * @param timeOfRelease the time at which the SoftLock was released
         */
        private SoftUnlockItemProcessor(SoftLock softLock, long timeOfRelease)
        {
            this.softLock = softLock;
            this.timeOfRelease = timeOfRelease;
        }


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object process(InvocableMap.Entry entry)
        {
            if (entry.isPresent())
            {
                CoherenceRegion.Value cacheValue = (CoherenceRegion.Value) entry;
                cacheValue.releaseSoftLock(softLock, timeOfRelease);
            }
            return null;
        }


    }


}
