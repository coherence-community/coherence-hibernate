/*
 * File: CoherenceTransactionalDataRegion.java
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

import com.tangosol.net.NamedCache;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * A CoherenceTransactionalDataRegion is a CoherenceRegion which may cache transactional data.
 *
 * @author Randy Stafford
 */
public abstract class CoherenceTransactionalDataRegion
extends CoherenceRegion
implements TransactionalDataRegion
{


    // ---- Fields

    /**
     * The CacheDataDescription describing the data in this CoherenceTransactionalDataRegion.
     */
    private CacheDataDescription cacheDataDescription;

    /**
     * The Hibernate settings object; may contain user-supplied "minimal puts" setting.
     */
    private Settings settings;


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param namedCache the NamedCache implementing this CoherenceTransactionalDataRegion
     * @param settings the Hibernate settings object
     * @param properties configuration properties for this CoherenceTransactionalDataRegion
     * @param cacheDataDescription a description of the data in this CoherenceTransactionalDataRegion
     */
    public CoherenceTransactionalDataRegion(NamedCache namedCache, Settings settings, Properties properties, CacheDataDescription cacheDataDescription)
    {
        super(namedCache, properties);
        this.cacheDataDescription = cacheDataDescription;
        this.settings = settings;
    }


    // ---- Accessors

    /**
     * Returns the Hibernate settings object for this CoherenceTransactionalDataRegion.
     *
     * @return the Settings object for this CoherenceTransactionalDataRegion
     */
    protected Settings getSettings()
    {
        return settings;
    }


    //---- interface org.hibernate.cache.TransactionalDataRegion

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionAware()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheDataDescription getCacheDataDescription()
    {
        return cacheDataDescription;
    }


}
