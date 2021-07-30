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
 * An CoherenceReadOnlyEntityAccess is an AbstractCoherenceEntityDataAccess
 * implementing Hibernate's read-only cache concurrency strategy for entity regions.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceReadOnlyEntityAccess
extends AbstractCoherenceEntityDataAccess
implements EntityDataAccess
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceEntityRegion the CoherenceEntityRegion for this CoherenceReadOnlyEntityAccess
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public CoherenceReadOnlyEntityAccess(DomainDataRegion domainDataRegion,
            DomainDataStorageAccess domainDataStorageAccess, Comparator<?> versionComparator)
    {
        super(domainDataRegion, domainDataStorageAccess, versionComparator);
    }


    // ---- interface org.hibernate.cache.spi.access.EntityRegionAccessStrategy

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
        debugf("%s.afterInsert(%s, %s, %s)", getClass().getName(), key, value, version);
        getCoherenceRegion().putValue(key, newCacheValue(value, version));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.update(%s, %s, %s, %s)", this, key, value, currentVersion, previousVersion);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.afterUpdate(%s, %s, %s, %s, %s)", this, key, value, currentVersion, previousVersion, lock);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
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
        return AccessType.READ_ONLY;
    }

    @Override
    public boolean contains(Object key) {
        // TODO Auto-generated method stub
        return false;
    }
}
