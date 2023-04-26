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
 * A AbstractCoherenceEntityDataAccess.PutFromLoadProcessor is an EntryProcessor
 * responsible for putting a value in a second-level cache that was just loaded from database,
 * and returning a boolean indicating whether it did so, consistent with the expected behavior
 * of a cache access strategy's putFromLoad() method.
 *
 * We move this behavior into the grid for efficient concurrency control.
 *
 * @author Randy Stafford
 */
public class PutFromLoadProcessor
extends AbstractProcessor
implements Serializable
{


    // ---- Constants

    /**
     * An identifier of this class's version for serialization purposes.
     */
    private static final long serialVersionUID = -4088045964348261168L;


    // ---- Fields

    /**
     * A flag indicating whether "minimal puts" is in effect for Hibernate.
     */
    private boolean minimalPutsInEffect;

    /**
     * The replacement cache value in this ReadWritePutFromLoadProcessor.
     */
    private CoherenceRegionValue replacementValue;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param minimalPutsInEffect a flag indicating whether "minimal puts" is in effect for Hibernate
     * @param replacementValue the replacement cache value in this ReadWritePutFromLoadProcessor
     */
    public PutFromLoadProcessor(boolean minimalPutsInEffect, CoherenceRegionValue replacementValue)
    {
        this.minimalPutsInEffect = minimalPutsInEffect;
        this.replacementValue = replacementValue;
    }


    // ---- interface com.tangosol.util.InvocableMap.EntryProcessor

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(InvocableMap.Entry entry)
    {
        if (minimalPutsInEffect && entry.isPresent())
        {
            return false;
        }
        else
        {
            entry.setValue(replacementValue);
            return true;
        }
    }

}
