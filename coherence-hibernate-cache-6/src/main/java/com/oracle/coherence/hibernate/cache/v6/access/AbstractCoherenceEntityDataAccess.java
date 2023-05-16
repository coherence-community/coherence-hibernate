/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.access;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.oracle.coherence.hibernate.cache.v6.access.processor.PutFromLoadProcessor;
import com.oracle.coherence.hibernate.cache.v6.configuration.support.Assert;
import com.oracle.coherence.hibernate.cache.v6.region.CoherenceRegion;
import com.oracle.coherence.hibernate.cache.v6.region.CoherenceRegionValue;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.AbstractDomainDataRegion;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A AbstractCoherenceEntityDataAccess is an object implementing a strategy for accessing a cache region.
 * Strategies vary with respect to transaction isolation enforcement.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public abstract class AbstractCoherenceEntityDataAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCoherenceEntityDataAccess.class);

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

    private final DomainDataRegion domainDataRegion;
    private final DomainDataStorageAccess domainDataStorageAccess;

    private Comparator<?> versionComparator;

    /**
     * A sequence number for soft locks acquired by this AbstractCoherenceEntityDataAccess.
     * Monotonically increasing characteristic enforced by usage.
     */
    private AtomicLong softLockSequenceNumber = new AtomicLong(0L);

    /**
     * A unique identifier of this AbstractCoherenceEntityDataAccess.
     */
    private UUID uuid = UUID.randomUUID();

    /**
     * Complete constructor.
     * @param domainDataRegion must not be null
     * @param domainDataStorageAccess must not be null
     * @param versionComparator must not be null
     */
    public AbstractCoherenceEntityDataAccess(DomainDataRegion domainDataRegion, DomainDataStorageAccess domainDataStorageAccess, Comparator<?> versionComparator) {
        Assert.notNull(domainDataRegion, "domainDataRegion must not be null.");
        Assert.notNull(domainDataStorageAccess, "storageAccess must not be null.");
        // Assert.notNull(versionComparator, "versionComparator must not be null."); //TODO verify

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Constructing AbstractCoherenceEntityDataAccess.");
        }
        this.domainDataRegion = domainDataRegion;
        this.domainDataStorageAccess = domainDataStorageAccess;
        this.versionComparator = versionComparator;
    }

    /**
     * Returns the CoherenceRegion for which this is a AbstractCoherenceEntityDataAccess.
     * @return the CoherenceRegion for which this is a AbstractCoherenceEntityDataAccess
     */
    protected CoherenceRegion getCoherenceRegion() {
        return ((CoherenceStorageAccessImpl) this.domainDataStorageAccess).getDelegate();
    }

    public DomainDataRegion getRegion() {
        return this.domainDataRegion;
    }

    protected DomainDataStorageAccess getDomainDataStorageAccess() {
        return this.domainDataStorageAccess;
    }

    protected Comparator<?> getVersionComparator() {
        return this.versionComparator;
    }

    protected CacheKeysFactory getCacheKeysFactory() {
        return ((AbstractDomainDataRegion) this.getRegion()).getEffectiveKeysFactory();
    }

    /**
     * Returns the UUID of this AbstractCoherenceEntityDataAccess.
     * @return the UUID of this AbstractCoherenceEntityDataAccess
     */
    public UUID getUuid() {
        return this.uuid;
    }

    /**
     * Returns the next sequence number for a SoftLock acquired by this AbstractCoherenceEntityDataAccess.
     * @return the long that is the next sequence number for a SoftLock acquired by this AbstractCoherenceEntityDataAccess
     */
    public long nextSoftLockSequenceNumber() {
        return this.softLockSequenceNumber.incrementAndGet();
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder(getClass().getName());
        stringBuilder.append("(");
        stringBuilder.append("coherenceRegion=").append(getCoherenceRegion());
        stringBuilder.append(", uuid=").append(this.uuid);
        stringBuilder.append(", softLockSequenceNumber=").append(this.softLockSequenceNumber);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public Object get(SharedSessionContractImplementor session, Object key) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getValue({})", key);
        }
        final CoherenceRegionValue cacheValue = (CoherenceRegionValue) getCoherenceRegion().getValue(key);
        return (cacheValue != null) ? cacheValue.getValue() : null;
    }

    public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("putFromLoad({}, {}, {})", key, value, version);
        }
        return putFromLoad(session, key, value, version, this.getCoherenceRegion().getRegionFactory().isMinimalPutsEnabledByDefault()); //TODO
    }

    public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, Object version, boolean minimalPutOverride)
            throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("putFromLoad({}, {}, {}, {})", key, value, version, minimalPutOverride);
        }
        final CoherenceRegionValue newCacheValue = newCacheValue(value, version);
        final PutFromLoadProcessor processor = new PutFromLoadProcessor(minimalPutOverride, newCacheValue);
        return (Boolean) getCoherenceRegion().invoke(key, processor);
    }

    public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("lockItem({}, {})", key, version);
        }
        //for the majority of access strategies lockItem is a no-op
        return null;
    }

    public SoftLock lockRegion() throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("lockRegion()");
        }
        getCoherenceRegion().lockCache();
        return null;
    }

    public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("lockItem({}, {})", key, lock);
        }
        //for the majority of access strategies unlockItem is a no-op
    }

    public void unlockRegion(SoftLock lock) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("unlockRegion({})", lock);
        }
        getCoherenceRegion().unlockCache();
    }

    public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("remove({})", key);
        }
        evict(key);
    }

    public void removeAll(SharedSessionContractImplementor session) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("removeAll()");
        }
        evictAll();
    }

    public void evict(Object key) throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("evict({})", key);
        }
        getCoherenceRegion().evict(key);
    }

    public void evictAll() throws CacheException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("evictAll()");
        }
        getCoherenceRegion().evictAll();
    }

    /**
     * Returns a new cache value with the argument value and version.
     * @param value the value for the new cache value
     * @param version the version for the new cache value
     * @return a CoherenceRegion.Value with the argument value
     */
    public CoherenceRegionValue newCacheValue(Object value, Object version) {
        return new CoherenceRegionValue(value, version, getCoherenceRegion().nextTimestamp());
    }

    public boolean contains(Object key) {
        return this.domainDataStorageAccess.contains(key);
    }
}
