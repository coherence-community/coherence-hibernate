/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.access;

import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Implementation of {@link CoherenceStorageAccess}. Will delegation operations to {@link CoherenceRegion}.
 *
 * @author Gunnar Hillert
 *
 */
public class CoherenceStorageAccessImpl implements CoherenceStorageAccess {

    private final CoherenceRegion delegate;

    public CoherenceStorageAccessImpl(CoherenceRegion delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public Object getFromCache(Object key, SharedSessionContractImplementor session) {
        return this.delegate.getValue(key);
    }

    @Override
    public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
        this.delegate.putValue(key, value);
    }

    @Override
    public boolean contains(Object key) {
        return this.delegate.contains(key);
    }

    @Override
    public void evictData() {
        this.delegate.evictAll();
    }

    @Override
    public void evictData(Object key) {
        this.delegate.evict(key);

    }

    @Override
    public void release() {
        this.delegate.destroy();

    }

    @Override
    public void afterUpdate(Object key, Object newValue, Object newVersion) {
        //this.delegate. //TODO
    }

    @Override
    public void unlockItem(Object key, SoftLock lock) {
        // TODO Auto-generated method stub //TODO
    }

    public CoherenceRegion getDelegate() {
        return this.delegate;
    }

}
