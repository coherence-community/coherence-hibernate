package com.oracle.coherence.hibernate.cache.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.SoftLock;

/**
 * @author mubin
 * @since 10/24/16.
 */
public interface RegionAccessStrategy {

    Object get(Object var1, long var2) throws CacheException;

    boolean putFromLoad(Object var1, Object var2, long var3, Object var5) throws CacheException;

    boolean putFromLoad(Object var1, Object var2, long var3, Object var5, boolean var6) throws CacheException;

    SoftLock lockItem(Object var1, Object var2) throws CacheException;

    SoftLock lockRegion() throws CacheException;

    void unlockItem(Object var1, SoftLock var2) throws CacheException;

    void unlockRegion(SoftLock var1) throws CacheException;

    void remove(Object var1) throws CacheException;

    void removeAll() throws CacheException;

    void evict(Object var1) throws CacheException;

    void evictAll() throws CacheException;
}
