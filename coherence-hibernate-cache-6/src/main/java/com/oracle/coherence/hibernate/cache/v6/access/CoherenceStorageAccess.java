/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.access;

import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;

/**
 * Coherence Extension of {@link DomainDataStorageAccess}.
 * @author Gunnar Hillert
 * @author Gunnar Hillert
 */
public interface CoherenceStorageAccess extends DomainDataStorageAccess {

    void afterUpdate(Object key, Object newValue, Object newVersion);

    void unlockItem(Object key, SoftLock lock);
}
