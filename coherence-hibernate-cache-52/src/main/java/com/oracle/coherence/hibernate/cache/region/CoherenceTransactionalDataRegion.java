/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.tangosol.net.NamedCache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.TransactionalDataRegion;

import java.util.Properties;

/**
 * A CoherenceTransactionalDataRegion is a CoherenceRegion which may cache transactional data.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public abstract class CoherenceTransactionalDataRegion
extends CoherenceRegion
implements TransactionalDataRegion
{


    // ---- Fields

    /**
     * The CacheDataDescription describing the data in this CoherenceTransactionalDataRegion.
     */
    private CacheDataDescription cacheDataDescription;

    /**
     * The Hibernate settings object; may contain user-supplied "minimal puts" setting.
     */
    private SessionFactoryOptions sessionFactoryOptions;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceTransactionalDataRegion
     * @param sessionFactoryOptions Hibernate SessionFactoryOptions
     * @param properties configuration properties for this CoherenceTransactionalDataRegion
     * @param cacheDataDescription a description of the data in this CoherenceTransactionalDataRegion
     */
    public CoherenceTransactionalDataRegion(NamedCache namedCache, SessionFactoryOptions sessionFactoryOptions, Properties properties, CacheDataDescription cacheDataDescription)
    {
        super(namedCache, properties);
        this.cacheDataDescription = cacheDataDescription;
        this.sessionFactoryOptions = sessionFactoryOptions;
    }


    // ---- Accessors

    /**
     * Returns the Hibernate {@link SessionFactoryOptions} object for this CoherenceTransactionalDataRegion.
     *
     * @return the SessionFactoryOptions object for this CoherenceTransactionalDataRegion
     */
    protected SessionFactoryOptions getSessionFactoryOptions()
    {
        return sessionFactoryOptions;
    }


    //---- interface org.hibernate.cache.spi.TransactionalDataRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionAware()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheDataDescription getCacheDataDescription()
    {
        return cacheDataDescription;
    }


}
