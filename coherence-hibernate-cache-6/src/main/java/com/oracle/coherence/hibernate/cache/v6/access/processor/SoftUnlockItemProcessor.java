/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access.processor;

import java.io.Serializable;

import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import org.hibernate.cache.spi.access.SoftLock;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

/**
 * A AbstractReadWriteCoherenceEntityDataAccess.SoftUnlockItemProcessor is an EntryProcessor
 * responsible for releasing a previously-acquired "soft lock" on a cache entry.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @author Randy Stafford
 */
public class SoftUnlockItemProcessor
extends AbstractProcessor
implements Serializable
{


    // ---- Constants

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = 8996659062190093054L;


    // ---- Fields

    /**
     * The SoftLock which is being released.
     */
    private SoftLock softLock;

    /**
     * The time at which the SoftLock was released.
     */
    private long timeOfRelease;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param softLock the SoftLock which is being released
     * @param timeOfRelease the time at which the SoftLock was released
     */
    public SoftUnlockItemProcessor(SoftLock softLock, long timeOfRelease)
    {
        this.softLock = softLock;
        this.timeOfRelease = timeOfRelease;
    }


    // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry)
    {
        if (entry.isPresent())
        {
            CoherenceRegionValue cacheValue = (CoherenceRegionValue) entry.getValue();
            cacheValue.releaseSoftLock(softLock, timeOfRelease);
            entry.setValue(cacheValue);
        }
        return null;
    }


}
