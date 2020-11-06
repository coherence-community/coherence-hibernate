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
 * A NaturalIdReadWriteCoherenceRegionAccessStrategy is a Coherence-based read-write region access strategy
 * for Hibernate natural ID regions.
 *
 * @author Randy Stafford
 */
public class NaturalIdReadWriteCoherenceRegionAccessStrategy
extends ReadWriteCoherenceRegionAccessStrategy<CoherenceNaturalIdRegion>
implements NaturalIdRegionAccessStrategy
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceNaturalIdRegion the CoherenceNaturalIdRegion for this NaturalIdReadWriteCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public NaturalIdReadWriteCoherenceRegionAccessStrategy(CoherenceNaturalIdRegion coherenceNaturalIdRegion, Settings settings)
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
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s, %s)", this, key, value);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(Object key, Object value) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        //In implementation we only insert the entry if there was no entry already present at the argument key
        debugf("%s.afterInsert(%s, %s)", this, key, value);
        return afterInsert(key, newCacheValue(value, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(Object key, Object value) throws CacheException
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
    public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/NaturalIdRegionAccessStrategy.html
        //Hibernate will make the call sequence lockItem() -> remove() -> update() -> afterUpdate() when updating a natural ID.
        //"Asynchrononous" (i.e. non-transactional) strategies should invalidate or update the cache entry here and release the lock,
        //as appropriate for the kind of strategy (nonstrict-read-write vs. read-write).
        //In the read-write strategy we only update the cache value if it is present and was not multiply locked.
        debugf("%s.afterUpdate(%s, %s, %s)", this, key, value, lock);
        return afterUpdate(key, newCacheValue(value, null), lock);
    }


}
