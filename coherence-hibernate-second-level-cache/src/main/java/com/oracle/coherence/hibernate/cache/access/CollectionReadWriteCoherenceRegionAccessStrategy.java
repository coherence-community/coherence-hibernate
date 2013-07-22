/*
 * File: CollectionReadWriteCoherenceRegionAccessStrategy.java
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
