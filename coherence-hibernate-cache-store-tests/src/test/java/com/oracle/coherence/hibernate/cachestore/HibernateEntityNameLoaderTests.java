/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cachestore;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies entity-name identity for bulk reads and writes, including multiple mappings of one Java class.
 */
public class HibernateEntityNameLoaderTests {

    @Test
    public void dynamicMapBulkLoad() {
        verifyLoads(true, false);
    }

    @Test
    public void dynamicMapCustomQuery() {
        verifyLoads(true, true);
    }

    @Test
    public void namedPojoBulkLoad() {
        verifyLoads(false, false);
    }

    @Test
    public void namedPojoCustomQuery() {
        verifyLoads(false, true);
    }

    @Test
    public void dynamicMapErase() {
        verifyErase(true, false);
    }

    @Test
    public void dynamicMapEraseAll() {
        verifyErase(true, true);
    }

    @Test
    public void namedPojoErase() {
        verifyErase(false, false);
    }

    @Test
    public void namedPojoEraseAll() {
        verifyErase(false, true);
    }

    @Test
    public void dynamicMapEraseAllRollsBackOnInvalidKey() {
        verifyEraseRollback(true);
    }

    @Test
    public void namedPojoEraseAllRollsBackOnInvalidKey() {
        verifyEraseRollback(false);
    }

    @Test
    public void dynamicMapBulkStore() {
        verifyStores(true);
    }

    @Test
    public void namedPojoBulkStore() {
        verifyStores(false);
    }

    @Test
    public void dynamicMapBulkStoreRollsBackOnConflictingIdentifier() {
        verifyStoreRollback(true);
    }

    @Test
    public void namedPojoBulkStoreRollsBackOnConflictingIdentifier() {
        verifyStoreRollback(false);
    }

    @Test
    public void ordinaryPojoUsesClassBasedBulkLoad() {
        final String mapping = """
                <hibernate-mapping>
                    <class name="%s" table="FIRST_RECORD">
                        <id name="id" type="long"><generator class="assigned"/></id>
                        <property name="label" type="string"/>
                    </class>
                </hibernate-mapping>
                """.formatted(Record.class.getName());
        try (SessionFactory factory = createFactory(mapping, true)) {
            factory.inTransaction((session) -> session.createNativeMutationQuery(
                    "insert into FIRST_RECORD (id, label) values (1, 'first'), (2, 'second')").executeUpdate());
            final TrackingLoader loader = new TrackingLoader(Record.class.getName(), factory);
            final Map results = loader.loadAll(List.of(2L, 999L, 1L));

            assertThat(results.keySet()).containsExactly(2L, 1L);
            assertThat(label(results.get(1L))).isEqualTo("first");
            assertThat(label(results.get(2L))).isEqualTo("second");
            assertThat(loader.classLoads).isEqualTo(1);
            assertThat(loader.namedLoads).isZero();
        }
    }

    private SessionFactory createFactory(boolean dynamic) {
        final String classAttribute = dynamic ? "" : "name=\"" + Record.class.getName() + "\"";
        final String mapping = """
                <hibernate-mapping>
                    <class %s entity-name="FirstRecord" table="FIRST_RECORD">
                        <id name="id" type="long"><generator class="assigned"/></id>
                        <property name="label" type="string"/>
                    </class>
                    <class %s entity-name="SecondRecord" table="SECOND_RECORD">
                        <id name="id" type="long"><generator class="assigned"/></id>
                        <property name="label" type="string"/>
                    </class>
                </hibernate-mapping>
                """.formatted(classAttribute, classAttribute);
        // Multiple POJO entity names remain supported by the legacy HBM reader, not its mapping.xml transformer.
        return createFactory(mapping, dynamic);
    }

    private SessionFactory createFactory(String mapping, boolean transform) {
        final Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:entity-name-loader-" + UUID.randomUUID());
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.setProperty("hibernate.cache.use_second_level_cache", "false");
        configuration.setProperty("hibernate.transform_hbm_xml.enabled", Boolean.toString(transform));
        configuration.addInputStream(new ByteArrayInputStream(mapping.getBytes(StandardCharsets.UTF_8)));
        return configuration.buildSessionFactory();
    }

    private void verifyLoads(boolean dynamic, boolean customQuery) {
        try (SessionFactory factory = createFactory(dynamic)) {
            factory.inTransaction((session) -> {
                session.createNativeMutationQuery("insert into FIRST_RECORD (id, label) values (1, 'first'), (2, 'first-two')").executeUpdate();
                session.createNativeMutationQuery("insert into SECOND_RECORD (id, label) values (1, 'second'), (2, 'second-two')").executeUpdate();
            });
            int classLoads = 0;
            int namedLoads = 0;
            for (String entityName : List.of("FirstRecord", "SecondRecord")) {
                final String expected = entityName.equals("FirstRecord") ? "first" : "second";
                final TrackingLoader loader = new TrackingLoader(entityName, factory);
                if (customQuery) {
                    loader.setLoadAllQuery("from " + entityName + " where id in (:ids)");
                }
                assertThat(label(loader.load(1L))).isEqualTo(expected);
                assertThat(loader.load(999L)).isNull();
                final Map results = loader.loadAll(List.of(2L, 999L, 1L));
                assertThat(results).containsOnlyKeys(1L, 2L);
                assertThat(label(results.get(1L))).isEqualTo(expected);
                assertThat(label(results.get(2L))).isEqualTo(expected + "-two");
                if (!customQuery) {
                    assertThat(results.keySet()).containsExactly(2L, 1L);
                    if (dynamic) {
                        assertThat(loader.graphLoads).isEqualTo(1);
                    }
                }
                classLoads += loader.classLoads;
                namedLoads += loader.namedLoads;
            }
            if (!dynamic && !customQuery) {
                // Class lookup selects exactly one of the two mappings; the other must retain name-based lookup.
                assertThat(classLoads).isEqualTo(1);
                assertThat(namedLoads).isEqualTo(1);
            }
        }
    }

    private void verifyStores(boolean dynamic) {
        try (SessionFactory factory = createFactory(dynamic)) {
            for (String entityName : List.of("FirstRecord", "SecondRecord")) {
                final HibernateCacheStore store = new HibernateCacheStore(entityName, factory);
                store.store(1L, record(dynamic, 1L, entityName + "-original"));
            }
            for (String entityName : List.of("FirstRecord", "SecondRecord")) {
                final HibernateCacheStore store = new HibernateCacheStore(entityName, factory);
                store.storeAll(Map.of(1L, record(dynamic, 1L, entityName + "-updated"),
                        2L, record(dynamic, 2L, entityName + "-inserted")));

                final String otherName = entityName.equals("FirstRecord") ? "SecondRecord" : "FirstRecord";
                final HibernateCacheLoader other = new HibernateCacheLoader(otherName, factory);
                if (entityName.equals("FirstRecord")) {
                    assertThat(label(other.load(1L))).isEqualTo("SecondRecord-original");
                    assertThat(other.load(2L)).isNull();
                }
                else {
                    assertThat(label(other.load(1L))).isEqualTo("FirstRecord-updated");
                    assertThat(label(other.load(2L))).isEqualTo("FirstRecord-inserted");
                }
                assertThat(label(store.load(1L))).isEqualTo(entityName + "-updated");
                assertThat(label(store.load(2L))).isEqualTo(entityName + "-inserted");
            }
        }
    }

    private void verifyStoreRollback(boolean dynamic) {
        try (SessionFactory factory = createFactory(dynamic)) {
            for (String entityName : List.of("FirstRecord", "SecondRecord")) {
                final HibernateCacheStore store = new HibernateCacheStore(entityName, factory);
                store.store(1L, record(dynamic, 1L, "original"));
                final Map<Long, Object> entries = new LinkedHashMap<>();
                entries.put(1L, record(dynamic, 1L, "updated"));
                entries.put(2L, record(dynamic, 2L, "inserted"));
                entries.put(3L, record(dynamic, 4L, "conflicting"));

                assertThatThrownBy(() -> store.storeAll(entries)).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Conflicting identifier information");
                assertThat(label(store.load(1L))).isEqualTo("original");
                assertThat(store.load(2L)).isNull();
                assertThat(store.load(3L)).isNull();
                assertThat(store.load(4L)).isNull();
            }
        }
    }

    private void verifyErase(boolean dynamic, boolean bulk) {
        for (String entityName : List.of("FirstRecord", "SecondRecord")) {
            try (SessionFactory factory = createFactory(dynamic)) {
                seedEraseRecords(factory, dynamic);
                final HibernateCacheStore store = new HibernateCacheStore(entityName, factory);
                if (bulk) {
                    store.eraseAll(List.of(1L, 999L, 1L, 2L));
                }
                else {
                    store.erase(1L);
                }
                assertThat(store.load(1L)).isNull();
                if (bulk) {
                    assertThat(store.load(2L)).isNull();
                    store.eraseAll(List.of(1L, 2L, 999L));
                    store.eraseAll(List.of());
                }
                else {
                    assertThat(label(store.load(2L))).isEqualTo(entityName + "-2");
                    store.erase(1L);
                    store.erase(999L);
                }
                assertThat(label(store.load(3L))).isEqualTo(entityName + "-3");

                final String otherName = entityName.equals("FirstRecord") ? "SecondRecord" : "FirstRecord";
                assertEraseRecordsPresent(new HibernateCacheLoader(otherName, factory), otherName);
            }
        }
    }

    private void verifyEraseRollback(boolean dynamic) {
        for (String entityName : List.of("FirstRecord", "SecondRecord")) {
            try (SessionFactory factory = createFactory(dynamic)) {
                seedEraseRecords(factory, dynamic);
                final HibernateCacheStore store = new HibernateCacheStore(entityName, factory);

                assertThatThrownBy(() -> store.eraseAll(List.of(1L, new Object())))
                        .isInstanceOf(RuntimeException.class);
                for (String name : List.of("FirstRecord", "SecondRecord")) {
                    assertEraseRecordsPresent(new HibernateCacheLoader(name, factory), name);
                }
            }
        }
    }

    private void seedEraseRecords(SessionFactory factory, boolean dynamic) {
        for (String entityName : List.of("FirstRecord", "SecondRecord")) {
            final HibernateCacheStore store = new HibernateCacheStore(entityName, factory);
            for (long id = 1; id <= 3; id++) {
                store.store(id, record(dynamic, id, entityName + "-" + id));
            }
        }
    }

    private void assertEraseRecordsPresent(HibernateCacheLoader loader, String entityName) {
        for (long id = 1; id <= 3; id++) {
            assertThat(label(loader.load(id))).isEqualTo(entityName + "-" + id);
        }
    }

    private Object record(boolean dynamic, Long id, String label) {
        if (dynamic) {
            // Application-created maps deliberately lack Hibernate's embedded entity-name marker.
            return new LinkedHashMap<>(Map.of("id", id, "label", label));
        }
        final Record record = new Record();
        record.setId(id);
        record.setLabel(label);
        return record;
    }

    private String label(Object entity) {
        return (entity instanceof Map<?, ?> map) ? (String) map.get("label") : ((Record) entity).getLabel();
    }

    private static class TrackingLoader extends HibernateCacheLoader {
        private int classLoads;
        private int namedLoads;
        private int graphLoads;

        TrackingLoader(String entityName, SessionFactory factory) {
            super(entityName, factory);
        }

        @Override
        protected Session openSession() {
            final Session session = super.openSession();
            return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[] {SessionImplementor.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("byMultipleIds")) {
                            this.namedLoads++;
                        }
                        else if (method.getName().equals("findMultiple")) {
                            if (arguments[0] instanceof Class<?>) {
                                this.classLoads++;
                            }
                            else {
                                this.graphLoads++;
                            }
                        }
                        try {
                            return method.invoke(session, arguments);
                        }
                        catch (InvocationTargetException ex) {
                            throw ex.getCause();
                        }
                    });
        }
    }

    public static class Record {
        private Long id;
        private String label;

        public Long getId() {
            return this.id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLabel() {
            return this.label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
