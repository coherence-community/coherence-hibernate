/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7.support;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniqueCoherenceClusterExtensionTests {

    @Test
    void classScopedIdentityIsReadyBeforeSetupAndUnchangedBetweenMethods() {
        final String previousName = System.getProperty("coherence.cluster");
        final String previousPort = System.getProperty("coherence.clusterport");
        try {
            final var extension = new UniqueCoherenceClusterExtension();
            extension.beforeAll(context(SharedClusterFixture.class, "shared-class"));
            final String name = System.getProperty("coherence.cluster");
            final String port = System.getProperty("coherence.clusterport");
            assertTrue(name.endsWith(Integer.toUnsignedString("shared-class".hashCode())));
            assertTrue(Integer.parseInt(port) >= 3000);
            extension.beforeEach(context(SharedClusterFixture.class, "first-method"));
            assertEquals(name, System.getProperty("coherence.cluster"));
            assertEquals(port, System.getProperty("coherence.clusterport"));
            extension.beforeEach(context(SharedClusterFixture.class, "second-method"));
            assertEquals(name, System.getProperty("coherence.cluster"));
            assertEquals(port, System.getProperty("coherence.clusterport"));
        }
        finally {
            restoreProperty("coherence.cluster", previousName);
            restoreProperty("coherence.clusterport", previousPort);
        }
    }

    @Test
    void defaultScopeStillSelectsAnIdentityForEachMethod() {
        final String previousName = System.getProperty("coherence.cluster");
        final String previousPort = System.getProperty("coherence.clusterport");
        try {
            final var extension = new UniqueCoherenceClusterExtension();
            extension.beforeAll(context(getClass(), "ordinary-class"));
            assertEquals(previousName, System.getProperty("coherence.cluster"));
            assertEquals(previousPort, System.getProperty("coherence.clusterport"));
            extension.beforeEach(context(getClass(), "first-method"));
            final String firstName = System.getProperty("coherence.cluster");
            extension.beforeEach(context(getClass(), "second-method"));
            assertNotEquals(firstName, System.getProperty("coherence.cluster"));
        }
        finally {
            restoreProperty("coherence.cluster", previousName);
            restoreProperty("coherence.clusterport", previousPort);
        }
    }

    private static ExtensionContext context(Class<?> testClass, String id) {
        final ExtensionContext context = mock(ExtensionContext.class);
        doReturn(testClass).when(context).getRequiredTestClass();
        when(context.getUniqueId()).thenReturn(id);
        return context;
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        }
        else {
            System.setProperty(name, value);
        }
    }

    @Test
    void selectedPortAcceptsBothProtocols() throws Exception {
        final int port = UniqueCoherenceClusterExtension.findAvailableClusterPort(3000, 64);
        try (ServerSocket tcp = new ServerSocket(); DatagramSocket udp = new DatagramSocket(null)) {
            tcp.bind(new InetSocketAddress(port));
            udp.bind(new InetSocketAddress(port));
            assertEquals(port, tcp.getLocalPort());
            assertEquals(port, udp.getLocalPort());
        }
    }

    @Test
    void skipsTcpOnlyListenerIncludingLoopbackSocketActivation() throws Exception {
        final int occupied = UniqueCoherenceClusterExtension.findAvailableClusterPort(4000, 64);
        try (ServerSocket tcp = new ServerSocket()) {
            tcp.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), occupied));
            final int selected = UniqueCoherenceClusterExtension.findAvailableClusterPort(occupied, 64);
            assertNotEquals(occupied, selected);
            try (DatagramSocket udp = new DatagramSocket(occupied)) {
                assertEquals(occupied, udp.getLocalPort(), "The original conflict is TCP only");
            }
        }
    }

    @Test
    void skipsUdpOnlyListenerAndReleasesItsTcpProbe() throws Exception {
        final int occupied = UniqueCoherenceClusterExtension.findAvailableClusterPort(5000, 64);
        try (DatagramSocket udp = new DatagramSocket(null)) {
            udp.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), occupied));
            final int selected = UniqueCoherenceClusterExtension.findAvailableClusterPort(occupied, 64);
            assertNotEquals(occupied, selected);
            try (ServerSocket tcp = new ServerSocket(occupied)) {
                assertEquals(occupied, tcp.getLocalPort(), "Rejected UDP candidate must not leak its TCP probe");
            }
        }
    }

    @Test
    void exhaustedCandidatesFailBeforeStartingCoherence() throws Exception {
        final int occupied = UniqueCoherenceClusterExtension.findAvailableClusterPort(6000, 64);
        try (DatagramSocket udp = new DatagramSocket(occupied)) {
            final IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> UniqueCoherenceClusterExtension.findAvailableClusterPort(occupied, 1));
            assertTrue(failure.getMessage().contains("both TCP and UDP"));
            assertTrue(failure.getMessage().contains(Integer.toString(occupied)));
            try (ServerSocket tcp = new ServerSocket(occupied)) {
                assertEquals(occupied, tcp.getLocalPort());
            }
        }
    }

    @UniqueCoherenceClusterExtension.ClassScoped
    private static final class SharedClusterFixture {
    }

}
