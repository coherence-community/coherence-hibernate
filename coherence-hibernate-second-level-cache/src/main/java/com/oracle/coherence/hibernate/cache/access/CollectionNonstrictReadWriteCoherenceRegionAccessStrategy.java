package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

/**
 * A CollectionNonstrictReadWriteCoherenceRegionAccessStrategy is CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a collection region.
 *
 * @author Randy Stafford
 */
public class CollectionNonstrictReadWriteCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceCollectionRegion>
implements CollectionRegionAccessStrategy
{


    // ---- Constuctors

    /**
     * Complete constructor.
     *
     * @param coherenceCollectionRegion the CoherenceCollectionRegion for this CollectionNonstrictReadWriteCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public CollectionNonstrictReadWriteCoherenceRegionAccessStrategy(CoherenceCollectionRegion coherenceCollectionRegion, Settings settings)
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
        debugf("%s.getRegion()", this);
        return getCoherenceRegion();
    }


}
