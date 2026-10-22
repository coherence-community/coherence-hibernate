/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.coherence.hibernate.cache.v7.access.processor.GetProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.PutFromLoadProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.ReadWritePutFromLoadProcessor;
import com.oracle.coherence.hibernate.cache.v7.configuration.support.CoherenceHibernateProperties;
import com.oracle.coherence.hibernate.cache.v7.region.CoherenceRegionValue;
import com.oracle.coherence.hibernate.cache.v7.support.UniqueCoherenceClusterExtension;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;
import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.Type;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.internal.util.compare.RowVersionComparator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserVersionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises a region factory in a separate client JVM against a real Extend proxy.
 */
@UniqueCoherenceClusterExtension.ClassScoped
public class CoherenceClientSessionTests {

    private static Coherence server;
    private static String previousProxyEnabled;
    private static String previousLocalStorage;

    @BeforeAll
    static void startStorage() throws Exception {
        previousProxyEnabled = System.getProperty("coherence.proxy.enabled");
        previousLocalStorage = System.getProperty("coherence.distributed.localstorage");
        System.setProperty("coherence.proxy.enabled", "true");
        System.setProperty("coherence.distributed.localstorage", "true");
        // Keep one storage cluster for this class. Restarting it while changing identity
        // lets NameService threads reload configuration for the preceding test.
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
                restoreProperty("coherence.proxy.enabled", previousProxyEnabled);
                restoreProperty("coherence.distributed.localstorage", previousLocalStorage);
            }
        }
    }

    @TempDir
    Path temporaryDirectory;

    @Test
    public void clientSessionUsesExtendWithoutJoiningCluster() throws Exception {
        exerciseClient("standard");
    }

    @Test
    public void offsetDateTimeVersionUsesTimelineOrderingOverExtend() throws Exception {
        exerciseClient("offset-date-time");
    }

    @Test
    public void zonedDateTimeVersionUsesTimelineOrderingOverExtend() throws Exception {
        exerciseClient("zoned-date-time");
    }

    @Test
    public void calendarVersionUsesMillisecondOrderingOverExtend() throws Exception {
        exerciseClient("calendar");
    }

    @Test
    public void dateAndJdbcTemporalStateUsesSecondLevelCacheOverExtend() throws Exception {
        exerciseClient("sql-temporal");
    }

    @Test
    public void serializableUserVersionTypeRunsOnStorageOverExtend() throws Exception {
        DescendingVersionType.STORAGE_COMPARISONS.set(0);
        exerciseClient("custom-version");
        assertTrue(DescendingVersionType.STORAGE_COMPARISONS.get() > 0,
                "The application comparator must execute in the storage JVM, which has no SessionFactory");
        System.out.println("Custom version comparisons on storage: " + DescendingVersionType.STORAGE_COMPARISONS.get());
    }

    private void exerciseClient(String workload) throws Exception {
        verifySecurityConfiguration();
        System.out.println("Extend storage JVM: " + ProcessHandle.current().pid() + " / " + System.getProperty("java.home"));
        assertEquals(System.getProperty("coherence.cluster"), server.getCluster().getClusterName());
        assertEquals(Integer.parseInt(System.getProperty("coherence.clusterport")),
                server.getCluster().getDependencies().getGroupPort());
        // Every workload must write this value itself, even with shared storage.
        server.getSession().getCache("client-mode-test").remove("key");
        final Path clientLog = this.temporaryDirectory.resolve("client.log");
        final List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dcoherence.cluster=" + System.getProperty("coherence.cluster"),
                "-Dcoherence.clusterport=" + System.getProperty("coherence.clusterport"),
                "-Dcoherence.wka.port=" + System.getProperty("coherence.clusterport"),
                "-Dcoherence.wka=127.0.0.1", "-Dcoherence.localhost=127.0.0.1",
                "-Djava.net.preferIPv4Stack=true",
                "-cp", System.getProperty("java.class.path"), ExtendClient.class.getName(), workload));
        if (System.getProperty("coherence.security.mode") != null) {
            command.add(1, "-Dcoherence.security.mode=" + System.getProperty("coherence.security.mode"));
        }
        final Process client = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(clientLog.toFile()).start();
        try {
            assertTrue(client.waitFor(60, TimeUnit.SECONDS), "Extend client timed out:\n" + Files.readString(clientLog));
            final String output = Files.readString(clientLog);
            assertEquals(0, client.exitValue(), output);
            assertTrue(output.contains(" / " + System.getProperty("java.home")), "Child must use the parent JDK");
            if ("hardened".equals(System.getProperty("coherence.security.mode"))) {
                assertTrue(output.contains("Extend security hardening verified: true"), output);
            }
            output.lines().filter((line) -> line.startsWith("Extend application JVM:")
                    || line.startsWith("Extend security hardening verified:") || line.endsWith(" passed"))
                    .forEach(System.out::println);
            if ("standard".equals(workload)) {
                assertEquals("from-client", server.getSession().getCache("client-mode-test").get("key"));
            }
            assertEquals(1, CacheFactory.getCluster().getMemberSet().size());
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

    private static void verifySecurityConfiguration() throws Exception {
        final String descriptor = "META-INF/coherence/security-config.xml";
        boolean adapterVisible = false;
        boolean fixturesVisible = false;
        for (final var resource : Collections.list(CoherenceClientSessionTests.class.getClassLoader().getResources(descriptor))) {
            System.out.println("Extend security descriptor: " + resource);
            try (var input = resource.openStream()) {
                final String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                adapterVisible |= xml.contains("access.processor.GetProcessor");
                fixturesVisible |= xml.contains("CustomCacheKeysFactoryTests$WrappedKey");
            }
        }
        assertTrue(adapterVisible, "Adapter descriptor must be visible in both JVMs");
        assertTrue(fixturesVisible, "Fixture descriptor must be visible in both JVMs");
        System.out.println("Extend Coherence: " + CacheFactory.class.getProtectionDomain().getCodeSource());
        if ("hardened".equals(System.getProperty("coherence.security.mode"))) {
            final Class<?> mode = Class.forName("com.tangosol.internal.util.CoherenceMode");
            assertEquals(true, mode.getMethod("isSecurityHardeningEnabled").invoke(null));
            System.out.println("Extend security hardening verified: true");
            assertThrows(RuntimeException.class, () -> ExternalizableHelper.fromBinary(
                    ExternalizableHelper.toBinary(new UnapprovedValue())));
        }
    }

    public static class ExtendClient {

        public static void main(String[] args) throws Exception {
            verifySecurityConfiguration();
            System.out.println("Extend application JVM: " + ProcessHandle.current().pid() + " / " + System.getProperty("java.home"));
            final String workload = args[0];
            final Class<?> entityType = switch (workload) {
                case "offset-date-time" -> OffsetDateTimeItem.class;
                case "zoned-date-time" -> ZonedDateTimeItem.class;
                case "calendar" -> CalendarItem.class;
                case "sql-temporal" -> TemporalItem.class;
                case "custom-version" -> CustomVersionItem.class;
                default -> VersionedItem.class;
            };
            final Configuration configuration = new Configuration().addAnnotatedClass(entityType);
            configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:extend");
            configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            configuration.setProperty("hibernate.generate_statistics", "true");
            configuration.setProperty("hibernate.cache.region.factory_class", CoherenceRegionFactory.class.getName());
            configuration.setProperty(CoherenceHibernateProperties.COHERENCE_SESSION_TYPE_PROPERTY_NAME, "client");
            configuration.setProperty(CoherenceHibernateProperties.CACHE_CONFIG_FILE_PATH_PROPERTY_NAME, "coherence-cache-config.xml");
            configuration.setProperty(CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.tcmp.enabled", "false");
            configuration.setProperty(CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.proxy.enabled", "false");
            configuration.setProperty(CoherenceHibernateProperties.DEFAULT_PROPERTY_PREFIX + "coherence.distributed.localstorage", "false");
            try (SessionFactory sessions = configuration.buildSessionFactory()) {
                final CoherenceRegionFactory factory = (CoherenceRegionFactory) ((SessionFactoryImplementor) sessions)
                        .getCache().getRegionFactory();
                assertNotNull(factory.getCoherenceSession());
                assertFalse(CacheFactory.getCluster().isRunning());
                final NamedCache<Object, Object> cache = factory.getCoherenceSession().getCache("client-mode-test");
                assertEquals(CacheService.TYPE_REMOTE, cache.getCacheService().getInfo().getServiceType());
                cache.put("key", "from-client");
                assertEquals("from-client", cache.get("key"));
                switch (workload) {
                    case "offset-date-time" -> exerciseOffsetDateTimeVersion(sessions);
                    case "zoned-date-time" -> exerciseZonedDateTimeVersion(sessions);
                    case "calendar" -> exerciseCalendarVersion(sessions);
                    case "sql-temporal" -> exerciseTemporalState(sessions);
                    case "custom-version" -> exerciseCustomVersion(sessions);
                    default -> {
                        exerciseHibernate(sessions);
                        exerciseApplicationKeysAndBinaryVersions(cache);
                    }
                }
                assertFalse(CacheFactory.getCluster().isRunning());
            }
        }

        private static void exerciseHibernate(SessionFactory sessions) {
            sessions.inTransaction((session) -> session.persist(new VersionedItem()));
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            sessions.inTransaction((session) -> assertEquals("before", session.find(VersionedItem.class, 1L).name));
            sessions.inTransaction((session) -> {
                final VersionedItem item = session.find(VersionedItem.class, 1L);
                assertEquals("before", item.name);
                assertEquals(List.of("tag"), item.tags);
            });
            assertTrue(sessions.getStatistics().getSecondLevelCacheHitCount() > 0);
            sessions.inTransaction((session) -> assertNotNull(session.bySimpleNaturalId(VersionedItem.class).load("code")));
            sessions.inTransaction((session) -> assertNotNull(session.bySimpleNaturalId(VersionedItem.class).load("code")));
            assertTrue(sessions.getStatistics().getNaturalIdCacheHitCount() > 0);
            assertEquals(List.of("before"), queryNames(sessions));
            assertEquals(List.of("before"), queryNames(sessions));
            assertEquals(1, sessions.getStatistics().getQueryCacheHitCount());
            sessions.inTransaction((session) -> session.find(VersionedItem.class, 1L).name = "after");
            assertEquals(List.of("after"), queryNames(sessions));
            sessions.inTransaction((session) -> assertEquals(1, session.find(VersionedItem.class, 1L).version));
            try (var session = sessions.openSession()) {
                session.beginTransaction();
                session.find(VersionedItem.class, 1L).name = "rolled-back";
                session.flush();
                session.getTransaction().rollback();
            }
            sessions.inTransaction((session) -> assertEquals("after", session.find(VersionedItem.class, 1L).name));
            sessions.inTransaction((session) -> session.remove(session.find(VersionedItem.class, 1L)));
            sessions.inTransaction((session) -> assertEquals(null, session.find(VersionedItem.class, 1L)));
        }

        private static void exerciseOffsetDateTimeVersion(SessionFactory sessions) {
            final OffsetDateTimeItem item = new OffsetDateTimeItem();
            sessions.inTransaction((session) -> session.persist(item));
            final OffsetDateTime initialVersion = item.version;
            assertNotNull(initialVersion, "Hibernate must generate the initial version");
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            // This database load transports Hibernate's actual mapped comparator to storage.
            sessions.inTransaction((session) -> assertEquals(initialVersion.toInstant(),
                    session.find(OffsetDateTimeItem.class, 1L).version.toInstant()));
            assertEquals(1, sessions.getStatistics().getSecondLevelCachePutCount());
            sessions.inTransaction((session) -> assertEquals("before", session.find(OffsetDateTimeItem.class, 1L).name));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
            sessions.inTransaction((session) -> session.find(OffsetDateTimeItem.class, 1L).name = "after");
            final OffsetDateTime updatedVersion = sessions.fromTransaction((session) -> {
                final OffsetDateTimeItem updated = session.find(OffsetDateTimeItem.class, 1L);
                assertEquals("after", updated.name);
                return updated.version;
            });
            assertTrue(updatedVersion.isAfter(initialVersion));
            assertStaleMergeRejected(sessions, item);

            // Equal instants at different offsets compare as equal in Hibernate, unlike compareTo().
            // Nanosecond neighbors also catch accidental conversion to milliseconds or microseconds.
            final OffsetDateTime precise = OffsetDateTime.parse("2026-10-15T10:20:30.123456789Z");
            final OffsetDateTime sameInstant = precise.withOffsetSameInstant(ZoneOffset.ofHours(2));
            assertTrue(precise.compareTo(sameInstant) < 0);
            assertPutFromLoadOrdering(sessions, OffsetDateTimeItem.class, precise, sameInstant,
                    precise.plusNanos(1), precise.minusNanos(1));
            System.out.println("OffsetDateTime ORM load, cache hit, update, stale rejection and nanosecond timeline ordering passed");
        }

        private static void exerciseZonedDateTimeVersion(SessionFactory sessions) {
            final ZonedDateTimeItem item = new ZonedDateTimeItem();
            sessions.inTransaction((session) -> session.persist(item));
            final ZonedDateTime initialVersion = item.version;
            assertNotNull(initialVersion, "Hibernate must generate the initial version");
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            // This database load transports Hibernate's serializable mapped comparator to storage.
            sessions.inTransaction((session) -> assertEquals(initialVersion.toInstant(),
                    session.find(ZonedDateTimeItem.class, 1L).version.toInstant()));
            assertEquals(1, sessions.getStatistics().getSecondLevelCachePutCount());
            sessions.inTransaction((session) -> assertEquals("before", session.find(ZonedDateTimeItem.class, 1L).name));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
            sessions.inTransaction((session) -> session.find(ZonedDateTimeItem.class, 1L).name = "after");
            final ZonedDateTime updatedVersion = sessions.fromTransaction((session) -> {
                final ZonedDateTimeItem updated = session.find(ZonedDateTimeItem.class, 1L);
                assertEquals("after", updated.name);
                return updated.version;
            });
            assertTrue(updatedVersion.isAfter(initialVersion));
            assertStaleMergeRejected(sessions, item);

            // Hibernate compares instants, including nanoseconds, rather than local time or zone IDs.
            final ZonedDateTime precise = ZonedDateTime.parse("2026-10-15T10:20:30.123456789Z[UTC]");
            final ZonedDateTime sameInstant = precise.withZoneSameInstant(ZoneId.of("Europe/Belgrade"));
            assertTrue(precise.compareTo(sameInstant) < 0);
            assertPutFromLoadOrdering(sessions, ZonedDateTimeItem.class, precise, sameInstant,
                    precise.plusNanos(1), precise.minusNanos(1));
            System.out.println("ZonedDateTime ORM load, cache hit, update, stale rejection and nanosecond timeline ordering passed");
        }

        private static void exerciseCalendarVersion(SessionFactory sessions) {
            final var factory = (SessionFactoryImplementor) sessions;
            assertSame(CalendarComparator.INSTANCE, factory.getMappingMetamodel().getEntityDescriptor(CalendarItem.class)
                    .getVersionType().getJavaTypeDescriptor().getComparator());
            final CalendarItem item = new CalendarItem();
            sessions.inTransaction((session) -> session.persist(item));
            assertNotNull(item.version, "Hibernate must generate the initial version");
            final Calendar initialVersion = (Calendar) item.version.clone();
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            // This database load transports Hibernate's actual mapped comparator to storage.
            sessions.inTransaction((session) -> assertEquals(initialVersion.getTimeInMillis(),
                    session.find(CalendarItem.class, 1L).version.getTimeInMillis()));
            assertEquals(1, sessions.getStatistics().getSecondLevelCachePutCount());
            sessions.inTransaction((session) -> assertEquals("before", session.find(CalendarItem.class, 1L).name));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
            sessions.inTransaction((session) -> session.find(CalendarItem.class, 1L).name = "after");
            final Calendar updatedVersion = sessions.fromTransaction((session) -> {
                final CalendarItem updated = session.find(CalendarItem.class, 1L);
                assertEquals("after", updated.name);
                return updated.version;
            });
            assertTrue(updatedVersion.getTimeInMillis() > initialVersion.getTimeInMillis());
            assertStaleMergeRejected(sessions, item);
            sessions.getCache().evictAllRegions();
            sessions.inTransaction((session) -> {
                final CalendarItem updated = session.find(CalendarItem.class, 1L);
                assertEquals("after", updated.name);
                assertEquals(updatedVersion.getTimeInMillis(), updated.version.getTimeInMillis());
            });

            // Calendar precision is milliseconds; different time zones must not break instant ties.
            final Calendar precise = GregorianCalendar.from(ZonedDateTime.parse("2026-10-15T10:20:30.123Z[UTC]"));
            final Calendar sameInstant = GregorianCalendar.from(precise.toInstant().atZone(ZoneId.of("Europe/Belgrade")));
            assertFalse(precise.equals(sameInstant));
            assertEquals(0, CalendarComparator.INSTANCE.compare(precise, sameInstant));
            final Calendar newer = (Calendar) precise.clone();
            newer.add(Calendar.MILLISECOND, 1);
            final Calendar older = (Calendar) precise.clone();
            older.add(Calendar.MILLISECOND, -1);
            assertPutFromLoadOrdering(sessions, CalendarItem.class, precise, sameInstant, newer, older);
            System.out.println("Calendar ORM load, cache hit, update, stale rejection and millisecond ordering passed");
        }

        private static void exerciseTemporalState(SessionFactory sessions) {
            final TemporalItem expected = new TemporalItem();
            assertEquals(Date.class, expected.legacyDate.getClass(), "Exercise an ordinary java.util.Date mapping");
            sessions.inTransaction((session) -> {
                session.setCacheMode(CacheMode.IGNORE);
                session.persist(expected);
            });
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            // Populate L2 from a database load, using Hibernate's real disassembly and Extend transport.
            sessions.inTransaction((session) -> assertTemporalValues(expected, session.find(TemporalItem.class, 1L)));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheMissCount());
            assertEquals(1, sessions.getStatistics().getSecondLevelCachePutCount());

            final SessionFactoryImplementor factory = (SessionFactoryImplementor) sessions;
            final var persister = factory.getMappingMetamodel().getEntityDescriptor(TemporalItem.class);
            final var access = persister.getCacheAccessStrategy();
            sessions.inTransaction((session) -> {
                final Object key = access.generateCacheKey(1L, persister, factory, null);
                final var entry = assertInstanceOf(StandardCacheEntryImpl.class,
                        access.get((SharedSessionContractImplementor) session, key));
                final Serializable[] state = entry.getDisassembledState();
                final List<String> properties = List.of(persister.getPropertyNames());
                assertEquals(Timestamp.class, state[properties.indexOf("legacyDate")].getClass(),
                        "Hibernate's default Date mapping disassembles to java.sql.Timestamp");
                assertEquals(Timestamp.class, state[properties.indexOf("jdbcTimestamp")].getClass());
                assertEquals(java.sql.Date.class, state[properties.indexOf("jdbcDate")].getClass());
                assertEquals(java.sql.Time.class, state[properties.indexOf("jdbcTime")].getClass());
                assertEquals(expected.legacyDate.getTime(), ((Timestamp) state[properties.indexOf("legacyDate")]).getTime());
                assertEquals(expected.jdbcTimestamp, state[properties.indexOf("jdbcTimestamp")]);
                assertEquals(expected.jdbcDate, state[properties.indexOf("jdbcDate")]);
                assertEquals(expected.jdbcTime, state[properties.indexOf("jdbcTime")]);
            });

            final long statements = sessions.getStatistics().getPrepareStatementCount();
            sessions.inTransaction((session) -> assertTemporalValues(expected, session.find(TemporalItem.class, 1L)));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
            assertEquals(1, sessions.getStatistics().getEntityLoadCount());
            assertEquals(statements, sessions.getStatistics().getPrepareStatementCount(), "L2 hit must avoid SQL");
            System.out.println("Date and JDBC temporal ORM disassembly, Extend cache hit and nanosecond precision passed");
        }

        private static void assertTemporalValues(TemporalItem expected, TemporalItem actual) {
            assertNotNull(actual);
            assertEquals(expected.legacyDate.getTime(), actual.legacyDate.getTime());
            assertEquals(expected.jdbcTimestamp, actual.jdbcTimestamp);
            assertEquals(123456789, actual.jdbcTimestamp.getNanos());
            assertEquals(expected.jdbcDate.toLocalDate(), actual.jdbcDate.toLocalDate());
            assertEquals(expected.jdbcTime.toLocalTime(), actual.jdbcTime.toLocalTime());
        }

        private static void exerciseCustomVersion(SessionFactory sessions) {
            final CustomVersionItem item = new CustomVersionItem();
            sessions.inTransaction((session) -> session.persist(item));
            assertEquals(new DescendingVersion(100), item.version, "UserVersionType.seed must run");
            sessions.getCache().evictAllRegions();
            sessions.getStatistics().clear();
            sessions.inTransaction((session) -> assertEquals(new DescendingVersion(100),
                    session.find(CustomVersionItem.class, 1L).version));
            assertEquals(1, sessions.getStatistics().getSecondLevelCachePutCount());
            sessions.inTransaction((session) -> assertEquals("before", session.find(CustomVersionItem.class, 1L).name));
            assertEquals(1, sessions.getStatistics().getSecondLevelCacheHitCount());
            sessions.inTransaction((session) -> session.find(CustomVersionItem.class, 1L).name = "after");
            sessions.inTransaction((session) -> {
                final CustomVersionItem updated = session.find(CustomVersionItem.class, 1L);
                assertEquals("after", updated.name);
                assertEquals(new DescendingVersion(99), updated.version, "UserVersionType.next must run");
            });
            assertStaleMergeRejected(sessions, item);
            // The value is deliberately not Comparable; lower numbers are newer versions.
            assertPutFromLoadOrdering(sessions, CustomVersionItem.class, new DescendingVersion(99),
                    new DescendingVersion(99), new DescendingVersion(98), new DescendingVersion(100));
            System.out.println("Serializable custom UserVersionType seed, next, cache hit, update and stale rejection passed");
        }

        private static void assertStaleMergeRejected(SessionFactory sessions, Object staleEntity) {
            assertThrows(OptimisticLockException.class,
                    () -> sessions.inTransaction((session) -> session.merge(staleEntity)));
        }

        private static void assertPutFromLoadOrdering(SessionFactory sessions, Class<?> entityType,
                Object initial, Object equal, Object newer, Object stale) {
            final SessionFactoryImplementor factory = (SessionFactoryImplementor) sessions;
            final var persister = factory.getMappingMetamodel().getEntityDescriptor(entityType);
            final var access = persister.getCacheAccessStrategy();
            assertEquals(org.hibernate.cache.spi.access.AccessType.READ_WRITE, access.getAccessType());
            sessions.inTransaction((session) -> {
                final var context = (SharedSessionContractImplementor) session;
                final Object key = access.generateCacheKey(1L, persister, factory, null);
                final Object payload = access.get(context, key);
                assertNotNull(payload, "Use the real Hibernate disassembled cache entry");
                access.evict(key);
                assertTrue(access.putFromLoad(context, key, payload, initial, false));
                assertFalse(access.putFromLoad(context, key, payload, equal, false), "Equal versions cannot replace");
                assertFalse(access.putFromLoad(context, key, payload, stale, false), "Stale versions cannot replace");
                assertFalse(access.putFromLoad(context, key, payload, newer, true), "Minimal puts preserve existing entries");
                assertTrue(access.putFromLoad(context, key, payload, newer, false), "The mapped comparator must accept newer versions");
                assertFalse(access.putFromLoad(context, key, payload, initial, false), "The advanced version rejects an older load");
                access.evict(key);
            });
        }

        private static List<String> queryNames(SessionFactory sessions) {
            return sessions.fromTransaction((session) -> session.createQuery(
                    "select i.name from ExtendVersionedItem i where i.code = :code", String.class)
                    .setParameter("code", "code").setCacheable(true).getResultList());
        }

        private static void exerciseApplicationKeysAndBinaryVersions(NamedCache<Object, Object> cache) {
            final Object key = new CustomCacheKeysFactoryTests.WrappedKey(new CacheKeySemanticsTests.CompositeId("group", 7));
            assertEquals(true, cache.invoke(key, new PutFromLoadProcessor(false,
                    new CoherenceRegionValue("initial", new byte[] {0x7f}, 1L))));
            assertEquals(true, cache.invoke(key, new ReadWritePutFromLoadProcessor(false, 2L,
                    new CoherenceRegionValue("newer", new byte[] {(byte) 0x80}, 2L), RowVersionComparator.INSTANCE)));
            assertEquals(false, cache.invoke(key, new ReadWritePutFromLoadProcessor(false, 3L,
                    new CoherenceRegionValue("stale", new byte[] {0x7f}, 3L), RowVersionComparator.INSTANCE)));
            assertEquals("newer", cache.invoke(key, new GetProcessor()));
            cache.remove(key);
        }
    }

    @Entity(name = "ExtendVersionedItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NaturalIdCache
    public static class VersionedItem {
        @Id
        private Long id = 1L;

        @Version
        private int version;

        @NaturalId
        private String code = "code";

        private String name = "before";

        @ElementCollection
        @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
        private List<String> tags = new ArrayList<>(List.of("tag"));
    }

    @Entity(name = "ExtendOffsetDateTimeItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class OffsetDateTimeItem {
        @Id
        private Long id = 1L;

        @Version
        @Column(secondPrecision = 9)
        private OffsetDateTime version;

        private String name = "before";
    }

    @Entity(name = "ExtendZonedDateTimeItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class ZonedDateTimeItem {
        @Id
        private Long id = 1L;

        @Version
        @Column(secondPrecision = 9)
        private ZonedDateTime version;

        private String name = "before";
    }

    @Entity(name = "ExtendCalendarItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class CalendarItem {
        @Id
        private Long id = 1L;

        @Version
        @Column(secondPrecision = 3)
        private Calendar version;

        private String name = "before";
    }

    @Entity(name = "ExtendTemporalItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class TemporalItem {
        @Id
        private Long id = 1L;

        @Column(secondPrecision = 3)
        private Date legacyDate = new Date(Timestamp.valueOf("2026-10-19 10:20:30.123").getTime());

        @Column(secondPrecision = 9)
        private Timestamp jdbcTimestamp = Timestamp.valueOf("2026-10-19 10:20:30.123456789");

        private java.sql.Date jdbcDate = java.sql.Date.valueOf("2026-10-19");

        private java.sql.Time jdbcTime = java.sql.Time.valueOf("10:20:30");
    }

    @Entity(name = "ExtendCustomVersionItem")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class CustomVersionItem {
        @Id
        private Long id = 1L;

        @Version
        @Type(DescendingVersionType.class)
        private DescendingVersion version;

        private String name = "before";
    }

    public record DescendingVersion(long number) implements Serializable {
    }

    private record VersionOrder(int direction) implements Serializable {
        int compare(DescendingVersion first, DescendingVersion second) {
            return this.direction * Long.compare(first.number(), second.number());
        }
    }

    /** Application-owned serializable comparator with a nontrivial, fully serializable object graph. */
    public static class DescendingVersionType implements UserVersionType<DescendingVersion>, Serializable {
        private static final long serialVersionUID = 1L;
        private static final AtomicInteger STORAGE_COMPARISONS = new AtomicInteger();
        private final VersionOrder order = new VersionOrder(-1);

        @Override
        public int compare(DescendingVersion first, DescendingVersion second) {
            if (CacheFactory.getCluster().isRunning()) {
                STORAGE_COMPARISONS.incrementAndGet();
            }
            return this.order.compare(first, second);
        }

        @Override
        public DescendingVersion seed(SharedSessionContractImplementor session) {
            return new DescendingVersion(100);
        }

        @Override
        public DescendingVersion next(DescendingVersion current, SharedSessionContractImplementor session) {
            return new DescendingVersion(current.number() - 1);
        }

        @Override
        public int getSqlType() {
            return Types.BIGINT;
        }

        @Override
        public Class<DescendingVersion> returnedClass() {
            return DescendingVersion.class;
        }

        @Override
        public DescendingVersion nullSafeGet(ResultSet result, int position, WrapperOptions options) throws SQLException {
            final long number = result.getLong(position);
            return result.wasNull() ? null : new DescendingVersion(number);
        }

        @Override
        public void nullSafeSet(PreparedStatement statement, DescendingVersion value, int position, WrapperOptions options)
                throws SQLException {
            if (value == null) {
                statement.setNull(position, Types.BIGINT);
            }
            else {
                statement.setLong(position, value.number());
            }
        }

        @Override
        public DescendingVersion deepCopy(DescendingVersion value) {
            return value;
        }

        @Override
        public boolean isMutable() {
            return false;
        }
    }

    private static final class UnapprovedValue implements Serializable {
    }
}
