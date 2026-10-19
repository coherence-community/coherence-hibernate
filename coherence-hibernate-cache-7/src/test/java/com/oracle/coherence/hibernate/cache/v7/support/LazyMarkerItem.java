/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7.support;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Enhanced at runtime by the marker regression, so its basic attribute is genuinely lazy.
 */
@Entity(name = "MarkerLazyItem")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, includeLazy = false)
public class LazyMarkerItem {

    @Id
    public Long id = 1L;

    @Basic(fetch = FetchType.LAZY)
    public String details = "lazy details";

    public String getDetails() {
        return this.details;
    }
}
