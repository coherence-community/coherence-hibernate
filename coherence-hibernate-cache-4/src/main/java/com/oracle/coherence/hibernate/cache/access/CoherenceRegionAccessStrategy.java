/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegion;
import com.oracle.coherence.hibernate.cache.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.region.CoherenceTransactionalDataRegion;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A CoherenceRegionAccessStrategy is an object implementing a strategy for accessing a cache region.
 * Strategies vary with respect to transaction isolation enforcement.
 *
 * @author Randy Stafford
 */
public abstract class CoherenceRegionAccessStrategy<T extends CoherenceTransactionalDataRegion>
implements RegionAccessStrategy
{


    // ---- Constants

    /**
     * The log message indicating lack of support for the transactional cache concurrency strategy.
     */
    public static final String TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE =
            "The transactional cache concurrency strategy is not supported.";

    /**
     * A message indicating write operations are not supported in the read-only cache concurrency strategy.
     */
    protected static final String WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE =
            "Write operations are not supported in the read-only cache concurrency strategy.";


    // ---- Fields

    /**
     * The CoherenceRegion for which this is an access strategy.
     */
    private T coherenceRegion;

    /**
     * The Hibernate settings object; may contain user-supplied "minimal puts" settings.
     */
    private Settings settings;

    /**
     * A sequence number for soft locks acquired by this CoherenceRegionAccessStrategy.
     * Monotonically increasing characteristic enforced by usage.
     */
    private AtomicLong softLockSequenceNumber = new AtomicLong(0L);

    /**
     * A unique identifier of this CoherenceRegionAccessStrategy.
     */
    private UUID uuid = UUID.randomUUID();


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceRegion the CoherenceRegion for which this is a CoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public CoherenceRegionAccessStrategy(T coherenceRegion, Settings settings)
    {
        debugf("%s(%s, %s)", getClass().getName(), coherenceRegion, settings);
        this.coherenceRegion = coherenceRegion;
        this.settings = settings;
    }


    // ---- Accessors

    /**
     * Returns the CoherenceRegion for which this is a CoherenceRegionAccessStrategy.
     *
     * @return the CoherenceRegion for which this is a CoherenceRegionAccessStrategy
     */
    protected T getCoherenceRegion()
    {
        return coherenceRegion;
    }

    /**
     * Returns the UUID of this CoherenceRegionAccessStrategy.
     *
     * @return the UUID of this CoherenceRegionAccessStrategy
     */
    protected UUID getUuid()
    {
        return uuid;
    }

    /**
     * Returns the next sequence number for a SoftLock acquired by this CoherenceRegionAccessStrategy.
     *
     * @return the long that is the next sequence number for a SoftLock acquired by this CoherenceRegionAccessStrategy
     */
    protected long nextSoftLockSequenceNumber()
    {
        return softLockSequenceNumber.incrementAndGet();
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
        stringBuilder.append("coherenceRegion=").append(getCoherenceRegion());
        stringBuilder.append(", uuid=").append(uuid);
        stringBuilder.append(", softLockSequenceNumber=").append(softLockSequenceNumber);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }


    // ---- interface org.hibernate.cache.spi.access.RegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key, long txTimestamp) throws CacheException
    {
        debugf("%s.getValue(%s, %s)", this, key, txTimestamp);
        CoherenceRegion.Value cacheValue = getCoherenceRegion().getValue(key);
        return (cacheValue == null)? null : cacheValue.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version)
    throws CacheException
    {
        debugf("%s.putFromLoad(%s, %s, %s, %s)", this, key, value, txTimestamp, version);
        return putFromLoad(key, value, txTimestamp, version, settings.isMinimalPutsEnabled());
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
        PutFromLoadProcessor processor = new PutFromLoadProcessor(minimalPutOverride, newCacheValue);
        return (Boolean) getCoherenceRegion().invoke(key, processor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SoftLock lockItem(Object key, Object version) throws CacheException
    {
        debugf("%s.lockItem(%s, %s)", this, key, version);
        //for the majority of access strategies lockItem is a no-op
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SoftLock lockRegion() throws CacheException
    {
        debugf("%s.lockRegion()", this);
        getCoherenceRegion().lockCache();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException
    {
        debugf("%s.lockItem(%s, %s)", this, key, lock);
        //for the majority of access strategies unlockItem is a no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlockRegion(SoftLock lock) throws CacheException
    {
        debugf("%s.unlockRegion(%s)", this, lock);
        getCoherenceRegion().unlockCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Object key) throws CacheException
    {
        debugf("%s.remove(%s)", this, key);
        evict(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws CacheException
    {
        debugf("%s.removeAll()", this);
        evictAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evict(Object key) throws CacheException
    {
        debugf("%s.evict(%s)", this, key);
        getCoherenceRegion().evict(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evictAll() throws CacheException
    {
        debugf("%s.evictAll()", this);
        getCoherenceRegion().evictAll();
    }


    // ---- Internal: factory

    /**
     * Returns a new cache value with the argument value and version.
     *
     * @param value the value for the new cache value
     * @param version the version for the new cache value
     *
     * @return a CoherenceRegion.Value with the argument value
     */
    protected CoherenceRegion.Value newCacheValue(Object value, Object version)
    {
        return new CoherenceRegion.Value(value, version, getCoherenceRegion().nextTimestamp());
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
     * A CoherenceRegionAccessStrategy.PutFromLoadProcessor is an EntryProcessor
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
        private static final long serialVersionUID = -4088045964348261168L;


        // ---- Fields

        /**
         * A flag indicating whether "minimal puts" is in effect for Hibernate.
         */
        private boolean minimalPutsInEffect;

        /**
         * The replacement cache value in this PutFromLoadProcessor.
         */
        private CoherenceRegion.Value replacementValue;


        // ---- Constructors

        /**
         * Complete constructor.
         *
         * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect for Hibernate
         * @param replacementValue the replacement cache value in this PutFromLoadProcessor
         */
        private PutFromLoadProcessor(boolean minimalPutsInEffect, CoherenceRegion.Value replacementValue)
        {
            this.minimalPutsInEffect = minimalPutsInEffect;
            this.replacementValue = replacementValue;
        }


        // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

        /**
         * {@inheritDoc}
         */
        @Override
        public Object process(InvocableMap.Entry entry)
        {
            if (minimalPutsInEffect && entry.isPresent())
            {
                return false;
            }
            else
            {
                entry.setValue(replacementValue);
                return true;
            }
        }


    }


}
