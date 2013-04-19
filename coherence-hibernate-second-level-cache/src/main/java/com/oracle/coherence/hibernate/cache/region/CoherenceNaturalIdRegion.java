package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.NaturalIdReadOnlyCoherenceRegionAccessStrategy;
import com.tangosol.net.NamedCache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * A CoherenceNaturalIdRegion is a CoherenceTransactionalDataRegion intended to cache Hibernate natural IDs.
 *
 * @author Randy Stafford
 */
public class CoherenceNaturalIdRegion
extends CoherenceTransactionalDataRegion
implements NaturalIdRegion
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceNaturalIdRegion
     * @param settings the Hibernate settings object
     * @param properties configuration properties for this CoherenceNaturalIdRegion
     * @param metadata the description of the data in this CoherenceNaturalIdRegion
     */
    public CoherenceNaturalIdRegion(NamedCache namedCache, Settings settings, Properties properties, CacheDataDescription metadata)
    {
        super(namedCache, settings, properties, metadata);
    }


    // ---- interface org.hibernate.cache.spi.NaturalIdRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException
    {
        debugf("%s.buildAccessStrategy(%s)", this, accessType);
        switch (accessType)
        {
            case NONSTRICT_READ_WRITE :
                return new NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy(this, getSettings());
            case READ_ONLY :
                return new NaturalIdReadOnlyCoherenceRegionAccessStrategy(this, getSettings());
            case READ_WRITE :
                return new NaturalIdReadWriteCoherenceRegionAccessStrategy(this, getSettings());
            case TRANSACTIONAL :
                throw new CacheException(CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
            default :
                throw new CacheException("Unknown AccessType: " + accessType);
        }
    }


}
