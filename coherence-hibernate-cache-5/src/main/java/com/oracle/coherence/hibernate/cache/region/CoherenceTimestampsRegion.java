/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.region;

import com.tangosol.net.NamedCache;
import org.hibernate.cache.spi.TimestampsRegion;

import java.util.Properties;

/**
 * A CoherenceTimestampsRegion is a CoherenceGeneralDataRegion intended to cache Hibernate timestamps.
 *
 * @author Randy Stafford
 */
public class CoherenceTimestampsRegion
extends CoherenceGeneralDataRegion
implements TimestampsRegion
{

    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceTimestampsRegion
     * @param properties configuration properties for this CoherenceTimestampsRegion
     */
    public CoherenceTimestampsRegion(NamedCache namedCache, Properties properties)
    {
        super(namedCache, properties);
    }


}
