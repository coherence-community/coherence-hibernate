/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

/**
 * A CollectionNonstrictReadWriteCoherenceRegionAccessStrategy is CoherenceCollectionRegionAccessStrategy
 * implementing Hibernate's read-write cache concurrency strategy.
 *
 * @author Randy Stafford
 */
public class CollectionReadWriteCoherenceRegionAccessStrategy
extends ReadWriteCoherenceRegionAccessStrategy<CoherenceCollectionRegion>
implements CollectionRegionAccessStrategy
{


    // ---- Constuctors

    /**
     * Complete constructor.
     *
     * @param coherenceCollectionRegion the CoherenceCollectionRegion for this CollectionReadWriteCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public CollectionReadWriteCoherenceRegionAccessStrategy(CoherenceCollectionRegion coherenceCollectionRegion, Settings settings)
    {
        super(coherenceCollectionRegion, settings);
    }


    // ---- interface org.hibernate.cache.spi.access.CollectionRegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectionRegion getRegion()
    {
        return getCoherenceRegion();
    }

}
