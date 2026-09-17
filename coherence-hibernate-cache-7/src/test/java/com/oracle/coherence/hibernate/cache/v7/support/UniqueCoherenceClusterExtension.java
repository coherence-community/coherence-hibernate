/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7.support;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Gives every test a distinct Coherence cluster identity and discovery port so a departing test cluster cannot
 * interfere with the next one.
 */
public class UniqueCoherenceClusterExtension implements BeforeEachCallback {

    private static final String CLUSTER_NAME_PROPERTY = "coherence.cluster";
    private static final String CLUSTER_PORT_PROPERTY = "coherence.clusterport";

    private static final int MINIMUM_CLUSTER_PORT = 3000;
    private static final int CLUSTER_PORT_RANGE = 6000;

    @Override
    public void beforeEach(ExtensionContext context) {
        final long processId = ProcessHandle.current().pid();
        final String testId = Integer.toUnsignedString(context.getUniqueId().hashCode());
        final String clusterName = String.format("coherence-hibernate-%d-%s", processId, testId);
        final int clusterPort = MINIMUM_CLUSTER_PORT + Math.floorMod(
                31 * Long.hashCode(processId) + context.getUniqueId().hashCode(), CLUSTER_PORT_RANGE);

        System.setProperty(CLUSTER_NAME_PROPERTY, clusterName);
        System.setProperty(CLUSTER_PORT_PROPERTY, Integer.toString(clusterPort));
    }

}
