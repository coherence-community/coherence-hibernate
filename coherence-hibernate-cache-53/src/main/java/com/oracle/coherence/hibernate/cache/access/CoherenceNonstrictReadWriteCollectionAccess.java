/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import java.util.Comparator;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A CoherenceNonstrictReadWriteCollectionAccess is CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a collection region.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceNonstrictReadWriteCollectionAccess
extends AbstractCoherenceEntityDataAccess
implements CollectionDataAccess
{

    // ---- Constuctors

    /**
     * Complete constructor.
     *
     * @param domainDataRegion the domain data region
     * @param domainDataStorageAccess the domain data storage access
     * @param versionComparator the version comparator
     */
    public CoherenceNonstrictReadWriteCollectionAccess(DomainDataRegion domainDataRegion,
            DomainDataStorageAccess domainDataStorageAccess, Comparator<?> versionComparator)
    {
        super(domainDataRegion, domainDataStorageAccess, versionComparator);
    }


    // ---- interface org.hibernate.cache.spi.access.CollectionRegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor sessionFactoryImplementor, String tenantIdentifier)
    {
        return this.getCacheKeysFactory().createCollectionKey(id, persister, sessionFactoryImplementor, tenantIdentifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCacheKeyId(Object cacheKey)
    {
        return this.getCacheKeysFactory().getCollectionId(cacheKey);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.NONSTRICT_READ_WRITE;
    }
}
