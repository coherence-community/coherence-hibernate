/*
 * File: NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy.java
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

import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy is a CoherenceRegionAccessStrategy
 * implementing Hibernate's nonstrict-read-write cache concurrency strategy for a natural ID region.
 *
 * @author Randy Stafford
 * @author Gunnar Hillert
 */
public class NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceNaturalIdRegion>
implements NaturalIdRegionAccessStrategy
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceNaturalIdRegion the CoherenceNaturalIdRegion for this NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy
     * @param sessionFactoryOptions the Hibernate SessionFactoryOptions object
     */
    public NaturalIdNonstrictReadWriteCoherenceRegionAccessStrategy(CoherenceNaturalIdRegion coherenceNaturalIdRegion, SessionFactoryOptions sessionFactoryOptions)
    {
        super(coherenceNaturalIdRegion, sessionFactoryOptions);
    }


    // ---- interface org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalIdRegion getRegion()
    {
        debugf("%s.getRegion()", this);
        return getCoherenceRegion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        //But in nonstrict-read-write cache concurrency strategies, don't put newly inserted natural IDs, to force
        //subsequent putFromLoad.
        debugf("%s.afterInsert(%s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should update the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should update it in afterUpdate instead.
        debugf("%s.update(%s, %s, %s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should invalidate or update the cache entry here and release the lock,
        //as appropriate for the kind of strategy (nonstrict-read-write vs. read-write).
        //In the nonstrict-read-write strategy we remove the cache entry to force subsequent putFromLoad.
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, lock);
        remove(session, key);
        unlockItem(session, key, lock);
        return false;
    }

    @Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session)
    {
    	return DefaultCacheKeysFactory.staticCreateNaturalIdKey(naturalIdValues, persister, session);
	}

    @Override
    public Object[] getNaturalIdValues(Object cacheKey)
    {
        return DefaultCacheKeysFactory.staticGetNaturalIdValues( cacheKey );
    }

}
