/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access.processor;

import java.io.Serializable;

import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * A AbstractReadWriteCoherenceEntityDataAccess.AfterUpdateProcessor is an EntryProcessor
 * responsible for updating a value in a second-level cache and returning a boolean indicating
 * whether it did so, consistent with the expected behavior of a read-write cache access strategy's
 * afterUpdate() method.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @author Randy Stafford
 */
public class AfterUpdateProcessor extends AbstractProcessor implements Serializable {

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = 2890338818667968735L;

    /**
     * A cache value to potentially replace the present one.
     */
    private CoherenceRegionValue replacementValue;

    /**
     * A SoftLock presumably acquired by a previous lockItem call on the entry being processed.
     */
    private SoftLock softLock;

    /**
     * The potential time at which all locks on the entry being processed were released.
     */
    private long timeOfSoftLockRelease;

    /**
     * Complete constructor.
     * @param replacementValue a cache value to potentially replace the present one
     * @param softLock a SoftLock presumably acquired by a previous lockItem call on the entry being processed
     * @param timeOfSoftLockRelease the potential time at which all locks on the entry being processed were released
     */
    public AfterUpdateProcessor(CoherenceRegionValue replacementValue, SoftLock softLock, long timeOfSoftLockRelease) {
        this.replacementValue = replacementValue;
        this.softLock = softLock;
        this.timeOfSoftLockRelease = timeOfSoftLockRelease;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry) {
        if (entry.isPresent()) {
            final CoherenceRegionValue cacheValue = (CoherenceRegionValue) entry.getValue();
            cacheValue.releaseSoftLock(this.softLock, this.timeOfSoftLockRelease);
            if (cacheValue.isSoftLocked()) {
                //The cache value being processed was soft-locked concurrently by multiple Hibernate transactions.
                //Under this condition we will not replace the cache value with the updated one.
                //But we need to save the mutation to the present value's state (i.e. the release of a soft lock).
                entry.setValue(cacheValue);
                return false;
            }
            else {
                //The cache value was soft-locked by only one Hibernate transaction.
                //Under this condition we can replace it with the updated one.
                entry.setValue(this.replacementValue);
                return true;
            }
        }
        else {
            //Some Hibernate transaction is trying to update a cache value that is not present.
            //Normally we would expect it to be present, I assume, as the result of a previous putFromLoad or afterInsert call.
            //Perhaps it got evicted in the meantime, either by application code or by cache configuration.
            //In any case, we will not modify cache contents under this condition.
            //Perhaps the value whose presence was expected will but put back into cache by a future putFromLoad call.
            return false;
        }
    }
}
