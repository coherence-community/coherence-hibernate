/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates.
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
 * A AbstractReadWriteCoherenceEntityDataAccess.GetProcessor is an EntryProcessor
 * for getting an entity in second-level cache.  It returns null if the cache value
 * is soft-locked, thereby forcing Hibernate to read from the database.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class GetProcessor extends AbstractProcessor implements Serializable {

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = 2359701955887239611L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry) {
        if (!entry.isPresent()) {
            return null;
        }
        final CoherenceRegionValue cacheValue = (CoherenceRegionValue) entry.getValue();
        if (cacheValue.isSoftLocked()) {
            return null;
        }
        return cacheValue.getValue();
    }
}
