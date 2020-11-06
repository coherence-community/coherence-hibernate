/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdReadOnlyCoherenceRegionAccessStrategy;
import com.tangosol.net.NamedCache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

import java.util.Properties;

/**
 * A CoherenceNaturalIdRegion is a CoherenceTransactionalDataRegion intended to cache Hibernate natural IDs.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceNaturalIdRegion
extends CoherenceTransactionalDataRegion
implements NaturalIdRegion
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceNaturalIdRegion
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions
     * @param properties configuration properties for this CoherenceNaturalIdRegion
     * @param metadata the description of the data in this CoherenceNaturalIdRegion
     */
    public CoherenceNaturalIdRegion(NamedCache namedCache, SessionFactoryOptions sessionFactoryOptions, Properties properties, CacheDataDescription metadata)
    {
        super(namedCache, sessionFactoryOptions, properties, metadata);
    }


    // ---- interface org.hibernate.cache.spi.NaturalIdRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException
    {
        debugf("%s.buildAccessStrategy(%s)", this, accessType);
        switch (accessType)
        {
            case NONSTRICT_READ_WRITE :
                return new NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_ONLY :
                return new NaturalIdReadOnlyCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_WRITE :
                return new NaturalIdReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case TRANSACTIONAL :
                throw new CacheException(CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
            default :
                throw new CacheException("Unknown AccessType: " + accessType);
        }
    }


}
