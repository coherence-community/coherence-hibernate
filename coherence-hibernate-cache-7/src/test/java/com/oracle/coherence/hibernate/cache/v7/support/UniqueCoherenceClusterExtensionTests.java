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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueCoherenceClusterExtensionTests {

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

}
