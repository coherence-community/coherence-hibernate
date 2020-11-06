/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.tangosol.net.NamedCache;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.engine.spi.SessionImplementor;

import java.util.Properties;

/**
 * A CoherenceGeneralDataRegion is a CoherenceRegion holding "general" (i.e., non-transactional) data.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public abstract class CoherenceGeneralDataRegion
extends CoherenceRegion
implements GeneralDataRegion
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceGeneralDataRegion
     * @param properties configuration properties for this CoherenceGeneralDataRegion
     */
    public CoherenceGeneralDataRegion(NamedCache namedCache, Properties properties)
    {
        super(namedCache, properties);
    }


    // ---- Interface org.hibernate.cache.spi.GeneralDataRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(SessionImplementor session, Object key)
    {
        Value cacheValue = super.getValue(key);
        return (cacheValue == null) ? null : cacheValue.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(SessionImplementor session, Object key, Object value)
    {
        super.putValue(key, new Value(value, null, nextTimestamp()));
    }


}
