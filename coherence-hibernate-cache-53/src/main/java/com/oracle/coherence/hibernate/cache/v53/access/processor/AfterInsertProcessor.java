/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access.processor;

import java.io.Serializable;

import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

/**
 * A AbstractReadWriteCoherenceEntityDataAccess.AfterInsertProcessor is an EntryProcessor
 * responsible for inserting a value into cache if none is present, and returning a boolean
 * indicating whether it did so, consistent with the expected behavior of a read-write cache
 * access strategy's afterInsert() method.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @param <K> the type of the Map entry key, see {@link AbstractProcessor}
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 *
 */
public class AfterInsertProcessor<K> extends AbstractProcessor<K, CoherenceRegionValue, Boolean> implements Serializable {

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = 2326579233150319530L;

    /**
     * The cache value for use by this AfterInsertProcessor.
     */
    private CoherenceRegionValue cacheValue;

    /**
     * Complete constructor.
     *
     * @param cacheValue the cache value for use by this AfterInsertProcessor
     */
    public AfterInsertProcessor(CoherenceRegionValue cacheValue) {
        this.cacheValue = cacheValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean process(InvocableMap.Entry entry) {
        if (entry.isPresent()) {
            return false;
        }
        else {
            entry.setValue(this.cacheValue);
            return true;
        }
    }
}
