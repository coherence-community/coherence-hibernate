/*
 * File: CollectionNonstrictReadWriteCoherenceRegionAccessStrategy.java
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

package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceCollectionRegion;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A CollectionNonstrictReadWriteCoherenceRegionAccessStrategy is CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a collection region.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
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
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public CollectionNonstrictReadWriteCoherenceRegionAccessStrategy(CoherenceCollectionRegion coherenceCollectionRegion, SessionFactoryOptions sessionFactoryOptions)
    {
        super(coherenceCollectionRegion, sessionFactoryOptions);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier)
    {
        return DefaultCacheKeysFactory.staticCreateCollectionKey( id, persister, factory, tenantIdentifier );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCacheKeyId(Object cacheKey)
    {
        return DefaultCacheKeysFactory.staticGetCollectionId(cacheKey);
    }

}
