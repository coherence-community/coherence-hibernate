/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import java.util.Comparator;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.AbstractDomainDataRegion;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * An CoherenceReadWriteEntityAccess is a Coherence-based read-write region access strategy
 * for Hibernate entity regions.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceReadWriteEntityAccess
extends AbstractReadWriteCoherenceEntityDataAccess
implements EntityDataAccess
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceEntityRegion the CoherenceEntityRegion for this CoherenceReadWriteEntityAccess
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public CoherenceReadWriteEntityAccess(DomainDataRegion domainDataRegion,
            DomainDataStorageAccess domainDataStorageAccess, Comparator<?> versionComparator)
    {
        super(domainDataRegion, domainDataStorageAccess, versionComparator);
    }


    // ---- interface org.hibernate.cache.spi.access.EntityRegionAccessStrategy
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public DomainDataRegion getRegion()
//    {
//        debugf("%s.getRegion()", this);
//        return getCoherenceRegion();
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/EntityRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s, %s)", this, key, value, version);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/EntityRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        //In implementation we only insert the entry if there was no entry already present at the argument key
        debugf("%s.afterInsert(%s, %s, %s)", this, key, value, version);
        return super.afterInsert(key, newCacheValue(value, version));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/EntityRegionAccessStrategy.html,
        //Hibernate will make the call sequence lockItem() -> update() -> afterUpdate() when updating an entity.
        //"Synchronous" (i.e. transactional) access strategies should update the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should update it in afterUpdate instead.
        debugf("%s.update(%s, %s, %s, %s)", this, key, value, currentVersion, previousVersion);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/EntityRegionAccessStrategy.html,
        //Hibernate will make the call sequence lockItem() -> update() -> afterUpdate() when updating an entity.
        //"Asynchrononous" (i.e. non-transactional) strategies should invalidate or update the cache entry here and release the lock,
        //as appropriate for the kind of strategy (nonstrict-read-write vs. read-write).
        //In the read-write strategy we only update the cache value if it is present and was not multiply locked.
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, currentVersion, previousVersion, lock);
        return afterUpdate(key, newCacheValue(value, currentVersion), lock);
    }

    @Override
    public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor sessionFactoryImplementor, String tenantIdentifier)
    {
        return ((AbstractDomainDataRegion) this.getRegion()).getEffectiveKeysFactory().createEntityKey( id, persister, sessionFactoryImplementor, tenantIdentifier );
    }

    @Override
    public Object getCacheKeyId(Object cacheKey)
    {
        return this.getCacheKeysFactory().getEntityId(cacheKey);
    }


    @Override
    public AccessType getAccessType() {
        return AccessType.READ_WRITE;
    }


    @Override
    public boolean contains(Object key) {
        // TODO Auto-generated method stub
        return false;
    }

}
