/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityReadOnlyCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.EntityReadWriteCoherenceRegionAccessStrategy;
import com.tangosol.net.NamedCache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;

import java.util.Properties;

/**
 * An CoherenceEntityRegion is a CoherenceTransactionalDataRegion intended to cache Hibernate entities.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class CoherenceEntityRegion
extends CoherenceTransactionalDataRegion
implements EntityRegion
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceEntityRegion
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions
     * @param properties configuration properties for this CoherenceEntityRegion
     * @param metadata the description of the data in this CoherenceEntityRegion
     */
    public CoherenceEntityRegion(NamedCache namedCache, SessionFactoryOptions sessionFactoryOptions, Properties properties, CacheDataDescription metadata)
    {
        super(namedCache, sessionFactoryOptions, properties, metadata);
    }


    // ---- interface org.hibernate.cache.spi.EntityRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException
    {
        debugf("%s.buildAccessStrategy(%s)", this, accessType);
        switch (accessType)
        {
            case NONSTRICT_READ_WRITE :
                return new EntityNonstrictReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_ONLY :
                return new EntityReadOnlyCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_WRITE :
                return new EntityReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case TRANSACTIONAL :
                throw new CacheException(CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
            default :
                throw new CacheException("Unknown AccessType: " + accessType);
        }
    }


}
