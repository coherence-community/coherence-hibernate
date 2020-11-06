/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.access;

import com.oracle.coherence.hibernate.cache.region.CoherenceEntityRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * An EntityReadOnlyCoherenceRegionAccessStrategy is an CoherenceRegionAccessStrategy
 * implementing Hibernate's read-only cache concurrency strategy for entity regions.
 *
 * @author Randy Stafford
 */
public class EntityReadOnlyCoherenceRegionAccessStrategy
extends CoherenceRegionAccessStrategy<CoherenceEntityRegion>
implements EntityRegionAccessStrategy
{


    // ---- Constructors

    /**
     * Complete constructor.
     *
     * @param coherenceEntityRegion the CoherenceEntityRegion for this EntityReadOnlyCoherenceRegionAccessStrategy
     * @param settings the Hibernate settings object
     */
    public EntityReadOnlyCoherenceRegionAccessStrategy(CoherenceEntityRegion coherenceEntityRegion, Settings settings)
    {
        super(coherenceEntityRegion, settings);
    }


    // ---- interface org.hibernate.cache.spi.access.EntityRegionAccessStrategy

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityRegion getRegion()
    {
        return getCoherenceRegion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(Object key, Object value, Object version) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/EntityRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Synchronous" (i.e. transactional) access strategies should insert the cache entry here, but
        //"asynchrononous" (i.e. non-transactional) strategies should insert it in afterInsert instead.
        debugf("%s.insert(%s, %s, %s)", this, key, value, version);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException
    {
        //per http://docs.jboss.org/hibernate/orm/4.1/javadocs/org/hibernate/cache/spi/access/EntityRegionAccessStrategy.html,
        //Hibernate will make the call sequence insert() -> afterInsert() when inserting an entity.
        //"Asynchrononous" (i.e. non-transactional) strategies should insert the cache entry here.
        debugf("%s.afterInsert(%s, %s, %s)", getClass().getName(), key, value, version);
        getCoherenceRegion().putValue(key, newCacheValue(value, version));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.update(%s, %s, %s, %s)", this, key, value, currentVersion, previousVersion);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) throws CacheException
    {
        //read-only cache entries should not be updated
        debugf("%s.afterUpdate(%s, %s, %s, %s, %s)", this, key, value, currentVersion, previousVersion, lock);
        throw new UnsupportedOperationException(WRITE_OPERATIONS_NOT_SUPPORTED_MESSAGE);
    }


}
