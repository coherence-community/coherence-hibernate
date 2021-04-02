/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadOnlyCoherenceRegionAccessStrategy;
import com.tangosol.net.NamedCache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

import java.util.Properties;

/**
 * A CoherenceCollectionRegion is a CoherenceTransactionalDataRegion intended to cache Hibernate collections (i.e. relationships).
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceCollectionRegion
extends CoherenceTransactionalDataRegion
implements CollectionRegion
{

    private final CacheKeysFactory cacheKeysFactory;

    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceCollectionRegion
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions
     * @param properties configuration properties for this CoherenceCollectionRegion
     * @param metadata the description of the data in this CoherenceCollectionRegion
     * @param cacheKeysFactory the cacheKeysFactory to use
     */
    public CoherenceCollectionRegion(NamedCache namedCache, SessionFactoryOptions sessionFactoryOptions, Properties properties,
                                     CacheDataDescription metadata, CacheKeysFactory cacheKeysFactory)
    {
        super(namedCache, sessionFactoryOptions, properties, metadata);
        this.cacheKeysFactory = cacheKeysFactory;
    }

    public CacheKeysFactory getCacheKeysFactory()
    {
        return cacheKeysFactory;
    }

    // ---- interface org.hibernate.cache.spi.CollectionRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException
    {
        debugf("%s.buildAccessStrategy(%s)", this, accessType);
        switch (accessType)
        {
            case NONSTRICT_READ_WRITE :
                return new CollectionNonstrictReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_ONLY :
                return new CollectionReadOnlyCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_WRITE :
                return new CollectionReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case TRANSACTIONAL :
                throw new CacheException(CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
            default :
                throw new CacheException("Unknown AccessType: " + accessType);
        }
    }


}
