package com.oracle.coherence.hibernate.cache.region;

import com.tangosol.net.NamedCache;
import org.hibernate.cache.spi.QueryResultsRegion;

import java.util.Properties;

/**
 * A CoherenceQueryResultsRegion is a CoherenceGeneralDataRegion intended to cache Hibernate query results.
 *
 * @author Randy Stafford
 */
public class CoherenceQueryResultsRegion
extends CoherenceGeneralDataRegion
implements QueryResultsRegion
{

    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceQueryResultsRegion
     * @param properties configuration properties for this CoherenceQueryResultsRegion
     */
    public CoherenceQueryResultsRegion(NamedCache namedCache, Properties properties)
    {
        super(namedCache, properties);
    }


}
