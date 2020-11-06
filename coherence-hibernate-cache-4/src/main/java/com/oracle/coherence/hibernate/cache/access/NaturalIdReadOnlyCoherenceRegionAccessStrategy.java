/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceNaturalIdRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * A NaturalIdReadOnlyCoherenceRegionAccessStrategy is a CoherenceRegionAccessStrategy
 * implementing Hibernate's read-only cache concurrency strategy for a natural ID region.
 *
 * @author Randy Stafford
 */
public class NaturalIdReadOnlyCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceNaturalIdRegion>
implements NaturalIdRegionAccessStrategy
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceNaturalIdRegion the CoherenceNaturalIdRegion for this NaturalIdReadOnlyCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public NaturalIdReadOnlyCoherenceRegionAccessStrategy(CoherenceNaturalIdRegion coherenceNaturalIdRegion, Settings settings)
    {
        super(coherenceNaturalIdRegion, settings);
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
    public boolean insert(Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        debugf("%s.afterInsert(%s, %s)", getClass().getName(), key, value);
        getCoherenceRegion().putValue(key, newCacheValue(value, null));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(Object key, Object value) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.update(%s, %s)", this, key, value);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, lock);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }


}
