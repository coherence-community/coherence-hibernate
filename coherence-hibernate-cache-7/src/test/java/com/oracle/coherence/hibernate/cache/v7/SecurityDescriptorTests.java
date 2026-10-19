/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.oracle.coherence.hibernate.cache.v7.access.processor.ReadWritePutFromLoadProcessor;
import com.oracle.coherence.hibernate.cache.v7.region.CoherenceRegionValue;
import com.tangosol.util.InvocableMap;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.cache.internal.BasicCacheKeyImplementation;
import org.hibernate.cache.internal.CacheKeyImplementation;
import org.hibernate.cache.internal.NaturalIdCacheKey;
import org.hibernate.cache.internal.QueryResultsCacheImpl;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ZonedDateTimeComparator;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Derives adapter requirements from compiled types and Hibernate's serialized state,
 * independently of the classpath descriptor union used by functional tests.
 */
public class SecurityDescriptorTests {

    private static final String DESCRIPTOR = "META-INF/coherence/security-config.xml";

    private static final String ADAPTER = "com.oracle.coherence.hibernate.cache.v7.";

    @Test
    public void descriptorCoversAdapterTypesAndHibernatePayloads() throws Exception {
        final Map<String, Boolean> production = read(Path.of("src/main/resources", DESCRIPTOR));
        final Map<String, Boolean> required = requirements();
        validateRequirements(production, required);
        final Set<String> unexpected = new LinkedHashSet<>(production.keySet());
        unexpected.removeAll(required.keySet());
        assertTrue(unexpected.isEmpty(), "Production grants need a representative transport fixture: " + unexpected);
        System.out.println("Derived production permissions (executable flag): " + required);
        for (final String name : production.keySet()) {
            assertFalse(SecurityDescriptorTests.class.getProtectionDomain().getCodeSource().equals(
                    Class.forName(name, false, SecurityDescriptorTests.class.getClassLoader()).getProtectionDomain().getCodeSource()),
                    "Test-owned class in production descriptor: " + name);
        }
    }

    @Test
    public void fixturePermissionsCannotSupplyProductionRequirements() throws Exception {
        final Map<String, Boolean> production = read(Path.of("src/main/resources", DESCRIPTOR));
        final Map<String, Boolean> fixtures = read(Path.of("src/test/resources", DESCRIPTOR));
        validateFixtures(production, fixtures);
    }

    @Test
    public void omittedGrantAndTestOnlyGrantAreDetected() throws Exception {
        final Map<String, Boolean> production = read(Path.of("src/main/resources", DESCRIPTOR));
        final Map<String, Boolean> required = requirements();
        for (final String name : required.keySet()) {
            final Map<String, Boolean> missing = new LinkedHashMap<>(production);
            missing.remove(name);
            final AssertionError failure = assertThrows(AssertionError.class,
                    () -> validateRequirements(missing, required), name);
            assertTrue(failure.getMessage().contains(name), failure.getMessage());
            // The production check must still fail even if a test descriptor supplies the grant.
            final Map<String, Boolean> maskingFixture = Map.of(name, required.get(name));
            assertThrows(AssertionError.class, () -> validateFixtures(missing, maskingFixture), name);
        }
    }

    @Test
    public void invalidDuplicateAndExcessPrivilegesAreDetected() throws Exception {
        final Path source = Path.of("src/main/resources", DESCRIPTOR);
        final String entry = "<class name=\"java.sql.Timestamp\" source=\"manual\"/>";
        final String xml = "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\" version=\"1.0\">"
                + "<allowed-classes>" + entry + "</allowed-classes></security-config>";
        assertThrows(AssertionError.class, () -> readXml(xml.replace(entry, entry + entry)));
        assertThrows(AssertionError.class, () -> readXml(xml.replace("java.sql.Timestamp", "java.sql.*")));
        assertThrows(AssertionError.class, () -> readXml(xml.replace(entry,
                entry.replace("/>", " executable=\"true\"/>"))));
        assertThrows(AssertionError.class, () -> readXml(xml.replace(entry,
                entry.replace("/>", " lambda-target=\"true\"/>"))));
        final Map<String, Boolean> production = read(source);
        final Map<String, Boolean> required = requirements();
        final String operation = required.entrySet().stream().filter(Map.Entry::getValue).findFirst().orElseThrow().getKey();
        production.put(operation, false);
        assertThrows(AssertionError.class, () -> validateRequirements(production, required));
    }

    private static Map<String, Boolean> requirements() throws Exception {
        final Map<String, Boolean> required = new LinkedHashMap<>();
        final Path classes = Path.of(CoherenceRegionValue.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        // Bounded to the two transport packages, including private comparator enums.
        // Serializable configuration enums, application classes and Hibernate's entire
        // Serializable universe are deliberately excluded.
        try (var paths = Files.walk(classes.resolve(ADAPTER.replace('.', '/')))) {
            for (final Path path : paths.filter((file) -> file.toString().endsWith(".class")
                    && (file.startsWith(classes.resolve(ADAPTER.replace('.', '/') + "access/processor"))
                    || file.startsWith(classes.resolve(ADAPTER.replace('.', '/') + "region")))).sorted().toList()) {
                final String name = classes.relativize(path).toString().replace(java.io.File.separatorChar, '.').replaceAll("\\.class$", "");
                final Class<?> type = Class.forName(name, false, SecurityDescriptorTests.class.getClassLoader());
                final boolean operation = InvocableMap.EntryProcessor.class.isAssignableFrom(type);
                if ((operation || Serializable.class.isAssignableFrom(type)) && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                    required.put(name, operation);
                }
            }
        }
        assertFalse(required.isEmpty(), "Adapter class inventory must not be empty");
        for (final Class<?> type : serializedClasses(hibernatePayloads())) {
            Class<?> component = type;
            while (component.isArray()) {
                component = component.getComponentType();
            }
            final String name = component.getName();
            if (name.startsWith("org.hibernate.") || name.startsWith(ADAPTER) || name.startsWith("java.sql.")
                    || component == Serializable.class || name.equals("sun.util.calendar.ZoneInfo")) {
                required.putIfAbsent(name, false);
            }
        }
        return required;
    }

    private static Serializable[] hibernatePayloads() throws Exception {
        final StandardCacheEntryImpl state = hibernateState();
        final var collection = mock(PersistentCollection.class);
        final var collectionPersister = mock(CollectionPersister.class);
        when(collection.disassemble(collectionPersister)).thenReturn(new Serializable[] {1L, 2L});
        final var session = mock(SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS);
        when(session.getFactory().getTenantIdentifierJavaType()).thenAnswer((invocation) -> StringJavaType.INSTANCE);
        final var memento = QueryParameterBindingsImpl.EMPTY.generateQueryKeyMemento(session);
        final QueryKey query = new QueryKey("select id from DescriptorProbe", memento, null, null, Set.of());
        final QueryResultsRegion region = mock(QueryResultsRegion.class);
        final var constructor = QueryResultsCacheImpl.class.getDeclaredConstructor(QueryResultsRegion.class, TimestampsCache.class);
        constructor.setAccessible(true);
        final var queryCache = constructor.newInstance(region, mock(TimestampsCache.class));
        queryCache.put(query, List.of(1L), session);
        final ArgumentCaptor<Object> result = ArgumentCaptor.forClass(Object.class);
        verify(region).putIntoCache(eq(query), result.capture(), same(session));
        final CoherenceRegionValue value = new CoherenceRegionValue(state, 1L, 123L);
        return new Serializable[] {state, new CollectionCacheEntry(collection, collectionPersister),
                new CacheKeyImplementation(1L, "DescriptorProbe", "tenant", 1),
                new BasicCacheKeyImplementation(1L, "DescriptorProbe", 1),
                new NaturalIdCacheKey("natural", "DescriptorProbe", "tenant", 1), query, (Serializable) result.getValue(),
                new ReadWritePutFromLoadProcessor(false, 1L, value, ComparableComparator.INSTANCE),
                new ReadWritePutFromLoadProcessor(false, 1L, value, ZonedDateTimeComparator.INSTANCE)};
    }

    private static StandardCacheEntryImpl hibernateState() {
        final TypeConfiguration types = new TypeConfiguration();
        final EntityPersister persister = mock(EntityPersister.class);
        when(persister.getEntityName()).thenReturn("DescriptorProbe");
        when(persister.getPropertyLaziness()).thenReturn(new boolean[5]);
        when(persister.getPropertyTypes()).thenReturn(new Type[] {
                types.getBasicTypeForJavaType(Timestamp.class), types.getBasicTypeForJavaType(java.sql.Date.class),
                types.getBasicTypeForJavaType(java.sql.Time.class), types.getBasicTypeForJavaType(String.class),
                types.getBasicTypeForJavaType(String.class)});
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/Belgrade"));
        calendar.setTimeInMillis(1792405230123L);
        final StandardCacheEntryImpl entry = new StandardCacheEntryImpl(new Object[] {
                Timestamp.valueOf("2026-10-19 10:20:30.123456789"), java.sql.Date.valueOf("2026-10-19"),
                java.sql.Time.valueOf("10:20:30"), LazyPropertyInitializer.UNFETCHED_PROPERTY,
                PropertyAccessStrategyBackRefImpl.UNKNOWN}, persister, calendar, null, null);
        assertEquals(Timestamp.class, entry.getDisassembledState()[0].getClass());
        assertEquals(java.sql.Date.class, entry.getDisassembledState()[1].getClass());
        assertEquals(java.sql.Time.class, entry.getDisassembledState()[2].getClass());
        return entry;
    }

    private static Set<Class<?>> serializedClasses(Object value) throws Exception {
        final Set<Class<?>> classes = new LinkedHashSet<>();
        try (var output = new ObjectOutputStream(new ByteArrayOutputStream()) {
            @Override
            protected void annotateClass(Class<?> type) {
                classes.add(type);
            }
        }) {
            output.writeObject(value);
        }
        return classes;
    }

    private static void validateRequirements(Map<String, Boolean> production, Map<String, Boolean> required) {
        required.forEach((name, executable) -> {
            assertNotNull(production.get(name), "Missing production permission: " + name);
            assertEquals(executable, production.get(name), "Wrong executable permission: " + name);
        });
    }

    private static void validateFixtures(Map<String, Boolean> production, Map<String, Boolean> fixtures) throws Exception {
        for (final String name : fixtures.keySet()) {
            assertNull(production.get(name), "Fixture duplicates production permission: " + name);
            final Class<?> type = Class.forName(name, false, SecurityDescriptorTests.class.getClassLoader());
            assertEquals(SecurityDescriptorTests.class.getProtectionDomain().getCodeSource(),
                    type.getProtectionDomain().getCodeSource(), "Fixture grant must be test-owned: " + name);
        }
    }

    private static Map<String, Boolean> read(Path path) throws Exception {
        return readXml(Files.readString(path));
    }

    private static Map<String, Boolean> readXml(String xml) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        final Element root = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(
                xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))).getDocumentElement();
        final String namespace = "http://xmlns.oracle.com/coherence/coherence-security-config";
        assertEquals(namespace, root.getNamespaceURI());
        assertEquals("security-config", root.getLocalName());
        assertEquals("1.0", root.getAttribute("version"));
        final Map<String, Boolean> entries = new LinkedHashMap<>();
        final var nodes = root.getElementsByTagNameNS(namespace, "class");
        assertTrue(nodes.getLength() > 0, "Descriptor must not be empty");
        for (int i = 0; i < nodes.getLength(); i++) {
            final Element entry = (Element) nodes.item(i);
            assertEquals("allowed-classes", entry.getParentNode().getLocalName());
            for (int attribute = 0; attribute < entry.getAttributes().getLength(); attribute++) {
                assertTrue(Set.of("name", "source", "executable", "lambda-target")
                        .contains(entry.getAttributes().item(attribute).getNodeName()), "Unknown permission attribute");
            }
            final String name = entry.getAttribute("name");
            assertTrue(name.matches("[\\w$]+(\\.[\\w$]+)+"), "Exact class name required: " + name);
            assertEquals("manual", entry.getAttribute("source"));
            final Class<?> type = Class.forName(name, false, SecurityDescriptorTests.class.getClassLoader());
            final boolean operation = InvocableMap.EntryProcessor.class.isAssignableFrom(type);
            final String executable = entry.getAttribute("executable");
            assertTrue(executable.isEmpty() || executable.equals("false") || executable.equals("true"));
            assertEquals(operation, Boolean.parseBoolean(executable), "Executable must match EntryProcessor: " + name);
            assertTrue(entry.getAttribute("lambda-target").isEmpty() || entry.getAttribute("lambda-target").equals("false"),
                    "No lambda-target grants expected: " + name);
            assertNull(entries.put(name, operation), "Duplicate permission: " + name);
        }
        // Reject broad package rules and unexpected elements rather than silently ignoring them.
        assertEquals(nodes.getLength() + 1, root.getElementsByTagName("*").getLength());
        return entries;
    }
}
