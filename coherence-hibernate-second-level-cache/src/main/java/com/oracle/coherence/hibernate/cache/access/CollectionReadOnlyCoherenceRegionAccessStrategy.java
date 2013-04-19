package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

/**
 * A CollectionReadOnlyCoherenceRegionAccessStrategy is CoherenceRegionAccessStrategy
 * implementing Hibernate's read-only cache concurrency strategy for collection regions.
 *
 * @author Randy Stafford
 */
public class CollectionReadOnlyCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceCollectionRegion>
implements CollectionRegionAccessStrategy
{


    // ---- Constuctors

    /**
     * Complete constructor.
     *
     * @param coherenceCollectionRegion the CoherenceCollectionRegion for this CollectionReadOnlyCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public CollectionReadOnlyCoherenceRegionAccessStrategy(CoherenceCollectionRegion coherenceCollectionRegion, Settings settings)
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
