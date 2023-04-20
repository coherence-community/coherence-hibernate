/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CoherenceReadWriteNaturalIdAccess is a Coherence-based read-write region access strategy
 * for Hibernate natural ID regions.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceReadWriteNaturalIdAccess
extends AbstractReadWriteCoherenceEntityDataAccess
implements NaturalIdDataAccess
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceReadWriteNaturalIdAccess.class);

    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param domainDataRegion the domain data region
     * @param domainDataStorageAccess the domain data storage access
     */
    public CoherenceReadWriteNaturalIdAccess(DomainDataRegion domainDataRegion,
            DomainDataStorageAccess domainDataStorageAccess)
    {
        super(domainDataRegion, domainDataStorageAccess, null);
    }


//    // ---- interface org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public NaturalIdRegion getRegion()
//    {
//        debugf("%s.getRegion()", this);
//        return getCoherenceRegion();
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("insert - key: {}, value: {}",key, value);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        //In implementation we only insert the entry if there was no entry already present at the argument key
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("afterInsert({}, {})", key, value);
        }
        return afterInsert(key, newCacheValue(value, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should update the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should update it in afterUpdate instead.
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("update({}, {})",key, value);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should invalidate or update the cache entry here and release the lock,
        //as appropriate for the kind of strategy (nonstrict-read-write vs. read-write).
        //In the read-write strategy we only update the cache value if it is present and was not multiply locked.
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("afterUpdate({}, {}, {})", key, value, lock);
        }
        return afterUpdate(key, newCacheValue(value, null), lock);
    }

    @Override
    public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session)
    {
        return this.getCacheKeysFactory().createNaturalIdKey(naturalIdValues, persister, session);
    }

    @Override
    public Object[] getNaturalIdValues(Object cacheKey)
    {
        return this.getCacheKeysFactory().getNaturalIdValues(cacheKey);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.READ_WRITE;
    }


    @Override
    public boolean contains(Object key) {
        return false;
    }

}
