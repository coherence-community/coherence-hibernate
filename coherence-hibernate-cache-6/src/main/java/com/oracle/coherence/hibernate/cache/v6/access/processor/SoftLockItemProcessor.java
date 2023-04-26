/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.access.processor;

import java.io.Serializable;

import com.oracle.coherence.hibernate.cache.v6.region.CoherenceRegionValue;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

/**
 * A AbstractReadWriteCoherenceEntityDataAccess.SoftLockItemProcessor is an EntryProcessor
 * responsible for "soft locking" a cache entry and returning an instance of
 * org.hibernate.cache.spi.access.SoftLock.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @author Randy Stafford
 */
public class SoftLockItemProcessor
extends AbstractProcessor
implements Serializable
{


    // ---- Constants

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = 5452465432039772596L;


    // ---- Fields

    /**
     * The SoftLock to be added to the cache value.
     */
    private CoherenceRegionValue.SoftLock softLock;

    /**
     * The cache value to soft lock in case there is no cache value already present.
     */
    private CoherenceRegionValue valueIfAbsent;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param valueIfAbsent the cache value to soft lock in case there is no cache value already present
     * @param softLock the SoftLock to be added to the cache value
     */
    public SoftLockItemProcessor(CoherenceRegionValue valueIfAbsent, CoherenceRegionValue.SoftLock softLock)
    {
        this.valueIfAbsent = valueIfAbsent;
        this.softLock = softLock;
    }


    // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry)
    {
        CoherenceRegionValue cacheValue = entry.isPresent() ?
                (CoherenceRegionValue) entry.getValue() :
                valueIfAbsent;
        cacheValue.addSoftLock(softLock);
        entry.setValue(cacheValue);
        return null;
    }


}
