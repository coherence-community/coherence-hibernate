/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.oracle.coherence.hibernate.cache.v7.configuration.support.CoherenceHibernateProperties;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises a region factory in a separate client JVM against a real Extend proxy.
 */
public class CoherenceClientSessionTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    public void clientSessionUsesExtendWithoutJoiningCluster() throws Exception {
        final String previousProxyEnabled = System.getProperty("coherence.proxy.enabled");
        final String previousLocalStorage = System.getProperty("coherence.distributed.localstorage");
        System.setProperty("coherence.proxy.enabled", "true");
        System.setProperty("coherence.distributed.localstorage", "true");

        try (Coherence server = Coherence.clusterMember()) {
            server.start().get(60, TimeUnit.SECONDS);
            final Path clientLog = this.temporaryDirectory.resolve("client.log");
            final Process client = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Dcoherence.cluster=" + System.getProperty("coherence.cluster"),
                    "-Dcoherence.clusterport=" + System.getProperty("coherence.clusterport"),
                    "-Dcoherence.wka.port=" + System.getProperty("coherence.clusterport"),
                    "-Dcoherence.wka=127.0.0.1", "-Dcoherence.localhost=127.0.0.1",
                    "-Djava.net.preferIPv4Stack=true",
                    "-cp", System.getProperty("java.class.path"), ExtendClient.class.getName())
                    .redirectErrorStream(true).redirectOutput(clientLog.toFile()).start();
            try {
                assertTrue(client.waitFor(60, TimeUnit.SECONDS), "Extend client timed out:\n" + Files.readString(clientLog));
                assertEquals(0, client.exitValue(), Files.readString(clientLog));
                assertEquals("from-client", server.getSession().getCache("client-mode-test").get("key"));
                assertEquals(1, CacheFactory.getCluster().getMemberSet().size());
            }
            finally {
                client.destroyForcibly();
                client.waitFor(10, TimeUnit.SECONDS);
            }
        }
        finally {
            CacheFactory.shutdown();
            restoreProperty("coherence.proxy.enabled", previousProxyEnabled);
            restoreProperty("coherence.distributed.localstorage", previousLocalStorage);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        }
        else {
            System.setProperty(name, value);
        }
    }

    public static class ExtendClient {

        public static void main(String[] args) {
            final SessionFactoryOptions options = (SessionFactoryOptions) Proxy.newProxyInstance(
                    SessionFactoryOptions.class.getClassLoader(), new Class<?>[] {SessionFactoryOptions.class},
                    (proxy, method, arguments) -> null);
            final CoherenceRegionFactory factory = new CoherenceRegionFactory();
            try {
                factory.start(options, Map.of(
                        CoherenceHibernateProperties.COHERENCE_SESSION_TYPE_PROPERTY_NAME, "client",
                        CoherenceHibernateProperties.CACHE_CONFIG_FILE_PATH_PROPERTY_NAME, "coherence-cache-config.xml",
                        CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.tcmp.enabled", "false",
                        CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.proxy.enabled", "false",
                        CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.distributed.localstorage", "false"));
                assertNotNull(factory.getCoherenceSession());
                assertFalse(CacheFactory.getCluster().isRunning());
                final NamedCache<String, String> cache = factory.getCoherenceSession().getCache("client-mode-test");
                assertEquals(CacheService.TYPE_REMOTE, cache.getCacheService().getInfo().getServiceType());
                cache.put("key", "from-client");
                assertEquals("from-client", cache.get("key"));
                assertFalse(CacheFactory.getCluster().isRunning());
            }
            finally {
                factory.stop();
            }
        }
    }
}
