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
 * A CoherenceNonstrictReadWriteCollectionAccess is CoherenceCollectionRegionAccessStrategy
 * implementing Hibernate's read-write cache concurrency strategy.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceReadWriteCollectionAccess
extends AbstractReadWriteCoherenceEntityDataAccess
implements CollectionDataAccess
{

    // ---- Constuctors

    /**
     * Complete constructor.
     *
     * @param coherenceCollectionRegion the CoherenceCollectionRegion for this CoherenceReadWriteCollectionAccess
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public CoherenceReadWriteCollectionAccess(DomainDataRegion domainDataRegion,
            DomainDataStorageAccess domainDataStorageAccess, Comparator<?> versionComparator)
    {
        super(domainDataRegion, domainDataStorageAccess, versionComparator);
    }


    // ---- interface org.hibernate.cache.spi.access.CollectionRegionAccessStrategy

    @Override
    public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor sessionFactoryImplementor, String tenantIdentifier)
    {
        return this.getCacheKeysFactory().createCollectionKey(id, persister, sessionFactoryImplementor, tenantIdentifier);
    }

    @Override
    public Object getCacheKeyId(Object cacheKey)
    {
        return this.getCacheKeysFactory().getCollectionId(cacheKey);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.READ_WRITE;
    }
}
