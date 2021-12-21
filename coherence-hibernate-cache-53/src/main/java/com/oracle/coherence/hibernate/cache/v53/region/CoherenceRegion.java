/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.region;

import com.oracle.coherence.hibernate.cache.v53.CoherenceRegionFactory;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.Assert;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.ExtractorProcessor;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * CoherenceRegion is an abstract superclass for classes representing different kinds of "region" in the Hibernate
 * second-level cache.  It abstracts behavior (and state) common to all types of Hibernate second-level cache region.
 *
 * Note that there is a concept (and therefore terminology) mapping between the Hibernate world and the Coherence world.
 * Hibernate uses "cache" to mean the whole, and "region" to mean a part of the whole.  Coherence uses "data grid" to
 * mean the whole, and "NamedCache" to mean a part of the whole.  So a "region" to Hibernate is a NamedCache to
 * Coherence.  Therefore, CoherenceRegion is basically an Adapter, adapting the Region SPI to the NamedCache API by
 * encapsulating and delegating to a NamedCache.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceRegion
implements Region, ExtendedStatisticsSupport
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceRegion.class);

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

    private final RegionFactory regionFactory;

    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param regionFactory the region factory
     * @param namedCache the Coherence NamedCache
     * @param properties the properties
     */
    public CoherenceRegion(RegionFactory regionFactory, NamedCache namedCache, Map<String, Object> properties)
    {
        Assert.notNull(regionFactory, "regionFactory must not be null.");
        Assert.notNull(namedCache, "namedCache must not be null.");

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Constructing CoherenceRegion for NamedCache '{}'.", namedCache.getCacheName());
        }
        lockLeaseDuration = (int) getDurationProperty(
                properties,
                LOCK_LEASE_DURATION_PROPERTY_NAME,
                DEFAULT_LOCK_LEASE_DURATION,
                Integer.MAX_VALUE);
        this.namedCache = namedCache;
        this.regionFactory = regionFactory;
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

    @Override
    public RegionFactory getRegionFactory() {
        return regionFactory;
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


    // ---- API: CoherenceCachedDomainDataAccess support

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
     * @return the CoherenceRegionValue at the argument key in this CoherenceRegion
     */
    public Object getValue(Object key)
    {
        //don't use an EntryProcessor here, because that precludes near cache hits.
        //access strategies with more strict concurrency control requirements call invoke() not getValue().
        final Object value = getNamedCache().get(key);
        return value == null ? null : value;
    }

    /**
     * Put the argument value into this CoherenceRegion at the argument key.
     *
     * @param key the key at which to put the value
     * @param value the value to put
     */
    public void putValue(Object key, Object value)
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
        InvocableMapHelper.lockAll(getNamedCache(), getNamedCache().keySet(), 0);
    }

    /**
     * Unlocks the entire cache.
     */
    public void unlockCache()
    {
        // will only work as imagined with caches of replicated topology
        InvocableMapHelper.unlockAll(getNamedCache(), getNamedCache().keySet());
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
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("getName()");
        }
        return getNamedCache().getCacheName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws CacheException
    {
        if (!getNamedCache().isReleased()) {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("destroy()");
            }
            getNamedCache().release();
        }
    }

    /**
     * {@inheritDoc}
     */
    //@Override
    public boolean contains(Object key)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("contains({})", key);
        }
        return getNamedCache().invoke(key, new ExtractorProcessor(IdentityExtractor.INSTANCE)) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSizeInMemory()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("getSizeInMemory()");
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getElementCountInMemory()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("getElementCountInMemory()");
        }
        return getNamedCache().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getElementCountOnDisk()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("getElementCountOnDisk()");
        }
        return -1;
    }

    /**
     * This method is undocumented in Hibernate javadoc, but seems intended to return the "current" time.
     *
     * @return a millisecond clock value
     */
    //@Override
    public long nextTimestamp()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("nextTimestamp()");
        }
        return getRegionFactory().nextTimestamp();
    }

    /**
     * This method is undocumented in Hibernate javadoc.  Comments in the Coherence-based implementation of the
     * Hibernate 2.1 second-level cache SPI suggest the returned value is used as a lock lease duration.
     *
     * @return an int lock lease duration
     */
    public int getTimeout()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("getTimeout()");
        }
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
    protected long getDurationProperty(Map<String, Object> properties, String propertyName, long defaultValue, long maxValue)
    {
        Base.azzert(maxValue >= defaultValue);
        Base.azzert(defaultValue >= 0);

        String propertyValue = (String) properties.get(propertyName);
        long duration;
        try
        {
            duration = Base.parseTime(propertyValue);
        }
        catch (Exception e)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error("Error parsing duration property {}; provided value was " +
                         "{}; using default of {} milliseconds.", propertyName, propertyValue, defaultValue);
            }
            duration = defaultValue;
        }

        if (duration > maxValue)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Capping {} at {} milliseconds.", propertyName, maxValue);
            }
            duration = maxValue;
        }

        return duration;
    }

    @Override
    public void clear() {
        this.namedCache.clear();
    }

}
