/*
 * File: CoherenceCollectionRegion.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.hibernate.cache.region;

import com.oracle.coherence.hibernate.cache.access.CoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionNonstrictReadWriteCoherenceRegionAccessStrategy;
import com.oracle.coherence.hibernate.cache.access.CollectionReadOnlyCoherenceRegionAccessStrategy;
import com.tangosol.net.NamedCache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

import java.util.Properties;

/**
 * A CoherenceCollectionRegion is a CoherenceTransactionalDataRegion intended to cache Hibernate collections (i.e. relationships).
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
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
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions
     * @param properties configuration properties for this CoherenceCollectionRegion
     * @param metadata the description of the data in this CoherenceCollectionRegion
     */
    public CoherenceCollectionRegion(NamedCache namedCache, SessionFactoryOptions sessionFactoryOptions, Properties properties, CacheDataDescription metadata)
    {
        super(namedCache, sessionFactoryOptions, properties, metadata);
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
                return new CollectionNonstrictReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_ONLY :
                return new CollectionReadOnlyCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case READ_WRITE :
                return new CollectionReadWriteCoherenceRegionAccessStrategy(this, getSessionFactoryOptions());
            case TRANSACTIONAL :
                throw new CacheException(CoherenceRegionAccessStrategy.TRANSACTIONAL_STRATEGY_NOT_SUPPORTED_MESSAGE);
            default :
                throw new CacheException("Unknown AccessType: " + accessType);
        }
    }


}
