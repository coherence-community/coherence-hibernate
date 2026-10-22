/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7.support;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Gives each test method (or opted-in class) a distinct cluster identity and a discovery port
 * checked for both TCP and UDP availability.
 */
public class UniqueCoherenceClusterExtension implements BeforeAllCallback, BeforeEachCallback {

    /**
     * Selects one identity before class setup for fixtures that share a running cluster.
     * Such fixtures must shut down Coherence after all tests, rather than between methods.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ClassScoped {
    }

    private static final String CLUSTER_NAME_PROPERTY = "coherence.cluster";
    private static final String CLUSTER_PORT_PROPERTY = "coherence.clusterport";

    private static final int MINIMUM_CLUSTER_PORT = 3000;
    private static final int CLUSTER_PORT_RANGE = 6000;
    private static final int MAXIMUM_PORT_ATTEMPTS = 64;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (isClassScoped(context)) {
            configureCluster(context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (!isClassScoped(context)) {
            configureCluster(context);
        }
    }

    private static boolean isClassScoped(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(ClassScoped.class);
    }

    private static void configureCluster(ExtensionContext context) {
        final long processId = ProcessHandle.current().pid();
        final String testId = Integer.toUnsignedString(context.getUniqueId().hashCode());
        final String clusterName = String.format("coherence-hibernate-%d-%s", processId, testId);
        final int preferredPort = MINIMUM_CLUSTER_PORT + Math.floorMod(
                31 * Long.hashCode(processId) + context.getUniqueId().hashCode(), CLUSTER_PORT_RANGE);
        final int clusterPort = findAvailableClusterPort(preferredPort, MAXIMUM_PORT_ATTEMPTS);

        System.setProperty(CLUSTER_NAME_PROPERTY, clusterName);
        System.setProperty(CLUSTER_PORT_PROPERTY, Integer.toString(clusterPort));
        context.publishReportEntry("coherence.discovery",
                "cluster=" + clusterName + ", preferred-port=" + preferredPort + ", selected-port=" + clusterPort);
    }

    static int findAvailableClusterPort(int preferredPort, int maximumAttempts) {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < maximumAttempts; attempt++) {
            final int candidate = MINIMUM_CLUSTER_PORT
                    + Math.floorMod(preferredPort - MINIMUM_CLUSTER_PORT + attempt, CLUSTER_PORT_RANGE);
            // Coherence discovery needs both protocols. Keep the TCP probe bound while checking UDP,
            // and use wildcard, exclusive bindings to detect loopback and other local listeners.
            // These probes are released before startup; another process can still acquire the port afterward.
            try (ServerSocket tcp = new ServerSocket(); DatagramSocket udp = new DatagramSocket(null)) {
                tcp.setReuseAddress(false);
                udp.setReuseAddress(false);
                tcp.bind(new InetSocketAddress(candidate));
                udp.bind(new InetSocketAddress(candidate));
                return candidate;
            }
            catch (IOException ex) {
                lastFailure = ex;
            }
        }
        throw new IllegalStateException("No discovery port available for both TCP and UDP after "
                + maximumAttempts + " attempts starting at " + preferredPort, lastFailure);
    }

}
