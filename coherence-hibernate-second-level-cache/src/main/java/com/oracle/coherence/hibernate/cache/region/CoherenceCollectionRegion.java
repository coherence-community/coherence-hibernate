package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadOnlyCoherenceRegionAccessStrategy;
import com.tangosol.net.NamedCache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * A CoherenceCollectionRegion is a CoherenceTransactionalDataRegion intended to cache Hibernate collections (i.e. relationships).
 *
 * @author Randy Stafford
 */
public class CoherenceCollectionRegion
extends CoherenceTransactionalDataRegion
implements CollectionRegion
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceCollectionRegion
     * @param settings the Hibernate settings object
     * @param properties configuration properties for this CoherenceCollectionRegion
     * @param metadata the description of the data in this CoherenceCollectionRegion
     */
    public CoherenceCollectionRegion(NamedCache namedCache, Settings settings, Properties properties, CacheDataDescription metadata)
    {
        super(namedCache, settings, properties, metadata);
    }


    // ---- interface org.hibernate.cache.spi.CollectionRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException
    {
        debugf("%s.buildAccessStrategy(%s)", this, accessType);
        switch (accessType)
        {
            case NONSTRICT_READ_WRITE :
                return new CollectionNonstrictReadWriteCoherenceRegionAccessStrategy(this, getSettings());
            case READ_ONLY :
                return new CollectionReadOnlyCoherenceRegionAccessStrategy(this, getSettings());
            case READ_WRITE :
                return new CollectionReadWriteCoherenceRegionAccessStrategy(this, getSettings());
            case TRANSACTIONAL :
                throw new CacheException(CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
            default :
                throw new CacheException("Unknown AccessType: " + accessType);
        }
    }


}
