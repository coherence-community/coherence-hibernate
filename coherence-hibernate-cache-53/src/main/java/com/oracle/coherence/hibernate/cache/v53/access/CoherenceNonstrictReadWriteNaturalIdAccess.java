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

/**
 * A CoherenceNonstrictReadWriteNaturalIdAccess is a CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a natural ID region.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceNonstrictReadWriteNaturalIdAccess
extends AbstractCoherenceEntityDataAccess
implements NaturalIdDataAccess
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param domainDataRegion the domain data region
     * @param domainDataStorageAccess the domain data storage access
     */
    public CoherenceNonstrictReadWriteNaturalIdAccess(DomainDataRegion domainDataRegion,
            DomainDataStorageAccess domainDataStorageAccess)
    {
        super(domainDataRegion, domainDataStorageAccess, null);
    }


    // ---- interface org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy

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
        debugf("%s.insert(%s, %s)", this, key, value);
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
        //But in nonstrict-read-write cache concurrency strategies, don't put newly inserted natural IDs, to force
        //subsequent putFromLoad.
        debugf("%s.afterInsert(%s, %s)", this, key, value);
        return false;
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
        debugf("%s.update(%s, %s, %s, %s)", this, key, value);
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
        //In the nonstrict-read-write strategy we remove the cache entry to force subsequent putFromLoad.
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, lock);
        remove(session, key);
        unlockItem(session, key, lock);
        return false;
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
        return AccessType.READ_ONLY;
    }

}
