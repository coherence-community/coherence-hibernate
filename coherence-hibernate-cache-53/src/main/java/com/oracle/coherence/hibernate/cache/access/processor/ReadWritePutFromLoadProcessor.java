/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access.processor;

import java.io.Serializable;
import java.util.Comparator;

import com.oracle.coherence.hibernate.cache.region.CoherenceRegionValue;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

/**
 * A AbstractReadWriteCoherenceEntityDataAccess.PutFromLoadProcessor is an EntryProcessor
 * responsible for putting a value in a second-level cache that was just loaded from database,
 * and returning a boolean indicating whether it did so, consistent with the expected behavior
 * of a cache access strategy's putFromLoad() method.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @author Randy Stafford
 */
public class ReadWritePutFromLoadProcessor
extends AbstractProcessor
implements Serializable
{


    // ---- Constants

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = -3993308461928039511L;


    // ---- Fields

    /**
     * A flag indicating whether "minimal puts" is in effect for Hibernate.
     */
    private boolean minimalPutsInEffect;

    /**
     * The replacement cache value in this ReadWritePutFromLoadProcessor.
     */
    private CoherenceRegionValue replacementValue;

    /**
     * From Hibernate javadoc, "a timestamp prior to the transaction start time"
     * [where "the transaction" loaded the potential replacement value from database].
     */
    private long txTimestamp;

    /**
     * A comparator for comparing actual value versions.
     */
    private Comparator versionComparator;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect for Hibernate
     * @param txTimestamp From Hibernate javadoc, "a timestamp prior to the transaction start time" [where "the transaction" loaded the potential replacement value from database]
     * @param replacementValue the replacement cache value in this ReadWritePutFromLoadProcessor
     * @param versionComparator a Comparator for comparing actual value versions
     */
    public ReadWritePutFromLoadProcessor(boolean minimalPutsInEffect, long txTimestamp, CoherenceRegionValue replacementValue, Comparator versionComparator)
    {
        this.minimalPutsInEffect = minimalPutsInEffect;
        this.txTimestamp = txTimestamp;
        this.replacementValue = replacementValue;
        this.versionComparator = versionComparator;
    }


    // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry)
    {
        boolean isReplaceable = true;
        if (entry.isPresent())
        {
            if (minimalPutsInEffect) return false;
            CoherenceRegionValue presentValue = (CoherenceRegionValue) entry.getValue();
            isReplaceable = presentValue.isReplaceableFromLoad(txTimestamp, replacementValue.getVersion(), versionComparator);
        }
        if (isReplaceable) entry.setValue(replacementValue);
        return isReplaceable;
    }


}
