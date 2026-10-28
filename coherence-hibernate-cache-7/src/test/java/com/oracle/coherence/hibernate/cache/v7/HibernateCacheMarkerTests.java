/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.oracle.coherence.hibernate.cache.v7.configuration.support.CoherenceHibernateProperties;
import com.oracle.coherence.hibernate.cache.v7.region.CoherenceRegionValue;
import com.oracle.coherence.hibernate.cache.v7.support.LazyMarkerItem;
import com.oracle.coherence.hibernate.cache.v7.support.UniqueCoherenceClusterExtension;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.util.ExternalizableHelper;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hibernate placeholders must survive hardened serialization and retain their singleton identity.
 */
@UniqueCoherenceClusterExtension.ClassScoped
public class HibernateCacheMarkerTests {

    private static Coherence server;
    private static String previousProxy;
    private static String previousStorage;

    @BeforeAll
    static void startStorage() throws Exception {
        previousProxy = System.getProperty("coherence.proxy.enabled");
        previousStorage = System.getProperty("coherence.distributed.localstorage");
        System.setProperty("coherence.proxy.enabled", "true");
        System.setProperty("coherence.distributed.localstorage", "true");
        verifyHardening();
        // Keep the cluster identity and server stable between client workloads.
        // NameService threads can reload the preceding configuration during shutdown.
        server = Coherence.clusterMember();
        server.start().get(60, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopStorage() {
        try {
            Coherence.closeAll();
        }
        finally {
            try {
                CacheFactory.shutdown();
            }
            finally {
                restoreProperty("coherence.proxy.enabled", previousProxy);
                restoreProperty("coherence.distributed.localstorage", previousStorage);
            }
        }
    }

    @TempDir
    Path temporaryDirectory;

    @Test
    public void unfetchedPropertyRetainsIdentityInSerializedCacheState() {
        assertMarkerRoundTrip(LazyPropertyInitializer.UNFETCHED_PROPERTY,
                "org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer$1");
    }

    @Test
    public void unknownBackReferenceRetainsIdentityInSerializedCacheState() {
        assertMarkerRoundTrip(PropertyAccessStrategyBackRefImpl.UNKNOWN,
                "org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl$1");
    }

    private static void assertMarkerRoundTrip(Serializable marker, String expectedClass) {
        verifyHardening();
        assertEquals(expectedClass, marker.getClass().getName());
        final var original = new CoherenceRegionValue(new Serializable[] {"ordinary", marker, null}, null, 123L);
        final CoherenceRegionValue copy = ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(original));
        assertNotSame(original, copy);
        assertEquals(123L, copy.getTimestamp());
        final var state = assertInstanceOf(Serializable[].class, copy.getValue());
        assertEquals("ordinary", state[0]);
        assertSame(marker, state[1], "Hibernate recognizes placeholders by identity");
        assertNull(state[2]);
    }

    @Test
    public void enhancedLazyAttributeUsesSecondLevelCacheOverExtend() throws Exception {
        exerciseClient("lazy");
    }

    @Test
    public void unidirectionalCollectionBackReferenceUsesSecondLevelCacheOverExtend() throws Exception {
        exerciseClient("backref");
    }

    private void exerciseClient(String workload) throws Exception {
        verifyHardening();
        assertEquals(System.getProperty("coherence.cluster"), server.getCluster().getClusterName());
        assertEquals(Integer.parseInt(System.getProperty("coherence.clusterport")),
                server.getCluster().getDependencies().getGroupPort());
        final Path log = this.temporaryDirectory.resolve(workload + ".log");
        final List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dcoherence.cluster=" + System.getProperty("coherence.cluster"),
                "-Dcoherence.clusterport=" + System.getProperty("coherence.clusterport"),
                "-Dcoherence.wka.port=" + System.getProperty("coherence.clusterport"),
                "-Dcoherence.wka=127.0.0.1", "-Dcoherence.localhost=127.0.0.1",
                "-Djava.net.preferIPv4Stack=true",
                "-cp", System.getProperty("java.class.path"), MarkerClient.class.getName(), workload));
        if (System.getProperty("coherence.security.mode") != null) {
            command.add(1, "-Dcoherence.security.mode=" + System.getProperty("coherence.security.mode"));
        }
        final Process client = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(client.waitFor(60, TimeUnit.SECONDS), "Marker client timed out:\n" + Files.readString(log));
            final String output = Files.readString(log);
            assertEquals(0, client.exitValue(), output);
            assertTrue(output.contains("Marker client Java home: " + System.getProperty("java.home")), output);
            if ("hardened".equals(System.getProperty("coherence.security.mode"))) {
                assertTrue(output.contains("Marker security hardening verified: true"), output);
            }
            output.lines().filter((line) -> line.startsWith("Marker ")).forEach(System.out::println);
            assertEquals(1, server.getCluster().getMemberSet().size());
        }
        finally {
            client.destroyForcibly();
            client.waitFor(10, TimeUnit.SECONDS);
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

    private static void verifyHardening() {
        System.out.println("Marker Coherence: " + CacheFactory.class.getProtectionDomain().getCodeSource());
        if ("hardened".equals(System.getProperty("coherence.security.mode"))) {
            try {
                final Class<?> mode = Class.forName("com.tangosol.internal.util.CoherenceMode");
                assertEquals(true, mode.getMethod("isSecurityHardeningEnabled").invoke(null));
            }
            catch (ReflectiveOperationException ex) {
                throw new AssertionError("Requested hardened mode must be active", ex);
            }
            assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(
                    ExternalizableHelper.toBinary(new UnapprovedValue())));
            System.out.println("Marker security hardening verified: true");
        }
    }

    public static class MarkerClient {

        public static void main(String[] args) throws Exception {
            verifyHardening();
            System.out.println("Marker client Java home: " + System.getProperty("java.home"));
            final boolean lazy = "lazy".equals(args[0]);
            final Class<?> entity = lazy ? enhancedLazyItem() : BackReferenceItem.class;
            Thread.currentThread().setContextClassLoader(entity.getClassLoader());
            final Configuration configuration = new Configuration(new BootstrapServiceRegistryBuilder()
                    .applyClassLoader(entity.getClassLoader()).build()).addAnnotatedClass(entity);
            if (!lazy) {
                configuration.addAnnotatedClass(Owner.class);
            }
            configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:markers");
            configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            configuration.setProperty("hibernate.generate_statistics", "true");
            configuration.setProperty("hibernate.cache.region.factory_class", CoherenceRegionFactory.class.getName());
            configuration.setProperty(CoherenceHibernateProperties.COHERENCE_SESSION_TYPE_PROPERTY_NAME, "client");
            configuration.setProperty(CoherenceHibernateProperties.CACHE_CONFIG_FILE_PATH_PROPERTY_NAME, "coherence-cache-config.xml");
            configuration.setProperty(CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.tcmp.enabled", "false");
            configuration.setProperty(CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.proxy.enabled", "false");
            configuration.setProperty(CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.distributed.localstorage", "false");
            try (SessionFactory sessions = configuration.buildSessionFactory()) {
                final var factory = (CoherenceRegionFactory) ((SessionFactoryImplementor) sessions).getCache().getRegionFactory();
                assertEquals(CacheService.TYPE_REMOTE, factory.getCoherenceSession().getCache("marker-probe")
                        .getCacheService().getInfo().getServiceType());
                assertFalse(CacheFactory.getCluster().isRunning());
                if (lazy) {
                    exerciseLazy(sessions, entity);
                }
                else {
                    exerciseBackReference(sessions);
                }
                assertFalse(CacheFactory.getCluster().isRunning());
            }
        }

        private static Class<?> enhancedLazyItem() throws Exception {
            final String name = LazyMarkerItem.class.getName();
            final byte[] original;
            try (var input = LazyMarkerItem.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
                assertNotNull(input);
                original = input.readAllBytes();
            }
            final byte[] enhanced = new BytecodeProviderImpl().getEnhancer(new DefaultEnhancementContext()).enhance(name, original);
            assertNotNull(enhanced, "The lazy mapping must use real Hibernate bytecode enhancement");
            final Class<?> entity = new ClassLoader(LazyMarkerItem.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
                    if (name.equals(className)) {
                        final Class<?> loaded = findLoadedClass(name);
                        return (loaded != null) ? loaded : defineClass(name, enhanced, 0, enhanced.length);
                    }
                    return super.loadClass(className, resolve);
                }
            }.loadClass(name);
            assertTrue(PersistentAttributeInterceptable.class.isAssignableFrom(entity));
            return entity;
        }

        private static void exerciseLazy(SessionFactory sessions, Class<?> entity) throws Exception {
            final Object item = entity.getConstructor().newInstance();
            sessions.inTransaction((session) -> {
                session.setCacheMode(CacheMode.IGNORE);
                session.persist(item);
            });
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            sessions.inTransaction((session) -> assertFalse(Hibernate.isPropertyInitialized(session.find(entity, 1L), "details")));
            assertEquals(1, sessions.getStatistics().getSecondLevelCachePutCount());
            assertCachedMarker(sessions, entity, LazyPropertyInitializer.UNFETCHED_PROPERTY);
            sessions.getStatistics().clear();
            final long before = sessions.getStatistics().getPrepareStatementCount();
            try (var session = sessions.openSession()) {
                final Object cached = session.find(entity, 1L);
                assertFalse(Hibernate.isPropertyInitialized(cached, "details"));
                assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
                assertEquals(before, sessions.getStatistics().getPrepareStatementCount(), "L2 load must avoid SQL");
                assertEquals("lazy details", entity.getMethod("getDetails").invoke(cached));
                assertTrue(Hibernate.isPropertyInitialized(cached, "details"));
                assertEquals(before + 1, sessions.getStatistics().getPrepareStatementCount());
            }
            System.out.println("Marker enhanced lazy attribute, disassembled singleton, L2 hit and deferred SQL passed");
        }

        private static void exerciseBackReference(SessionFactory sessions) {
            sessions.inTransaction((session) -> {
                session.setCacheMode(CacheMode.IGNORE);
                session.persist(new Owner());
            });
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            sessions.inTransaction((session) -> {
                final BackReferenceItem item = session.find(BackReferenceItem.class, 1L);
                assertEquals("child", item.name);
                // Dirty checking reads the synthetic back-reference through Hibernate's UNKNOWN getter.
                item.name = "updated";
            });
            assertTrue(sessions.getStatistics().getSecondLevelCachePutCount() > 0);
            assertCachedMarker(sessions, BackReferenceItem.class, PropertyAccessStrategyBackRefImpl.UNKNOWN);
            sessions.getStatistics().clear();
            sessions.inTransaction((session) -> assertEquals("updated", session.find(BackReferenceItem.class, 1L).name));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
            assertEquals(0, sessions.getStatistics().getPrepareStatementCount(), "L2 load must avoid SQL");
            System.out.println("Marker collection back-reference, disassembled singleton and L2 hit passed");
        }

        private static void assertCachedMarker(SessionFactory sessions, Class<?> entity, Serializable marker) {
            final var factory = (SessionFactoryImplementor) sessions;
            final var persister = factory.getMappingMetamodel().getEntityDescriptor(entity);
            final var access = persister.getCacheAccessStrategy();
            sessions.inTransaction((session) -> {
                final Object key = access.generateCacheKey(1L, persister, factory, null);
                final var entry = assertInstanceOf(StandardCacheEntryImpl.class,
                        access.get((SharedSessionContractImplementor) session, key));
                assertTrue(Arrays.stream(entry.getDisassembledState()).anyMatch((value) -> value == marker),
                        "The real remote Hibernate cache entry must retain the placeholder singleton: "
                                + List.of(persister.getPropertyNames()) + " = " + Arrays.toString(entry.getDisassembledState()));
            });
        }
    }

    @Entity(name = "MarkerOwner")
    public static class Owner {

        @Id
        public Long id = 1L;

        @OneToMany(cascade = CascadeType.ALL)
        @JoinColumn(name = "owner_id", nullable = false)
        public List<BackReferenceItem> children = new ArrayList<>(List.of(new BackReferenceItem()));
    }

    @Entity(name = "MarkerBackReferenceItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class BackReferenceItem {

        @Id
        public Long id = 1L;

        public String name = "child";
    }

    private static final class UnapprovedValue implements Serializable {
    }
}
