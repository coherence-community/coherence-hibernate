/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A CollectionNonstrictReadWriteCoherenceRegionAccessStrategy is CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a collection region.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CollectionNonstrictReadWriteCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceCollectionRegion>
implements CollectionRegionAccessStrategy
{


    // ---- Constuctors

    /**
     * Complete constructor.
     *
     * @param coherenceCollectionRegion the CoherenceCollectionRegion for this CollectionNonstrictReadWriteCoherenceRegionAccessStrategy
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public CollectionNonstrictReadWriteCoherenceRegionAccessStrategy(CoherenceCollectionRegion coherenceCollectionRegion, SessionFactoryOptions sessionFactoryOptions)
    {
        super(coherenceCollectionRegion, sessionFactoryOptions);
    }


    // ---- interface org.hibernate.cache.spi.access.CollectionRegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectionRegion getRegion()
    {
        debugf("%s.getRegion()", this);
        return getCoherenceRegion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier)
    {
        return DefaultCacheKeysFactory.staticCreateCollectionKey( id, persister, factory, tenantIdentifier );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCacheKeyId(Object cacheKey)
    {
        return DefaultCacheKeysFactory.staticGetCollectionId(cacheKey);
    }

}
