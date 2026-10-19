/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.packaging;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.tangosol.util.ExternalizableHelper;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Inspects final artifacts after upstream jar, shade and repackage executions. */
public class SecurityPackagingIT {

    private static final String DESCRIPTOR = "META-INF/coherence/security-config.xml";

    private static final Path ROOT = Path.of(System.getProperty("project.root")).toAbsolutePath().normalize();

    @TempDir
    Path temporaryDirectory;

    @Test
    public void adapterJarContainsOnlyProductionPermissions() throws Exception {
        final Path adapter = adapterJar();
        final byte[] expected = Files.readAllBytes(ROOT.resolve("coherence-hibernate-cache-7/src/main/resources/" + DESCRIPTOR));
        try (var jar = new JarFile(adapter.toFile())) {
            assertArrayEquals(expected, resource(jar, DESCRIPTOR), "Packaged descriptor differs from production source");
            final var entries = jar.stream().filter((entry) -> entry.getName().endsWith(".class")).toList();
            assertTrue(!entries.isEmpty(), "Adapter JAR must contain classes");
            for (final var entry : entries) {
                final String source = entry.getName().replaceFirst("(\\$.*)?\\.class$", ".java");
                assertTrue(Files.isRegularFile(ROOT.resolve("coherence-hibernate-cache-7/src/main/java/" + source)),
                        "Packaged class has no production source: " + entry.getName());
                final byte[] bytes = resource(jar, entry.getName());
                assertEquals(61, ((bytes[6] & 255) << 8) | (bytes[7] & 255), "Java 17 bytecode: " + entry.getName());
            }
        }
        permissions(expected);
    }

    @Test
    public void shadePreservesEveryInputPermissionAndItsFlags() throws Exception {
        final Map<String, List<Boolean>> expected = permissions(Files.readAllBytes(
                ROOT.resolve("coherence-hibernate-cache-7/src/main/resources/" + DESCRIPTOR)));
        try (var coherence = new JarFile(location(ExternalizableHelper.class).toFile())) {
            mergePermissions(expected, permissions(resource(coherence, DESCRIPTOR)));
        }
        final Path demo = ROOT.resolve("samples/coherence-hibernate-cache-store-demo");
        final byte[] applicationPermissions = Files.readAllBytes(demo.resolve("src/main/resources/" + DESCRIPTOR));
        mergePermissions(expected, permissions(applicationPermissions));
        try (var sample = new JarFile(ROOT.resolve("samples/coherence-hibernate-cache-store-demo/target/"
                + "coherence-hibernate-cache-store-demo.jar").toFile())) {
            final Map<String, List<Boolean>> actual = permissions(resource(sample, DESCRIPTOR));
            final var missing = new LinkedHashSet<>(expected.keySet());
            missing.removeAll(actual.keySet());
            final var extra = new LinkedHashSet<>(actual.keySet());
            extra.removeAll(expected.keySet());
            assertTrue(missing.isEmpty(), "Missing shaded permissions: " + missing);
            assertTrue(extra.isEmpty(), "Unexpected shaded permissions: " + extra);
            expected.forEach((name, flags) -> assertEquals(flags, actual.get(name), "Changed shaded flags: " + name));
            System.out.println("Shaded permission union preserved: " + actual.size() + " exact entries");
        }
    }

    @Test
    public void bootPackagesRetainTheAdapterAndCoherenceDescriptors() throws Exception {
        for (final String sample : List.of("app", "server")) {
            final String artifact = "coherence-hibernate-demo-" + sample;
            final Path path = ROOT.resolve("samples/coherence-hibernate-demo/" + artifact + "/target/" + artifact + ".jar");
            try (var boot = new JarFile(path.toFile())) {
                for (final Path input : List.of(adapterJar(), location(ExternalizableHelper.class))) {
                    final String nested = "BOOT-INF/lib/" + input.getFileName();
                    try (var jar = new JarFile(input.toFile())) {
                        assertArrayEquals(resource(jar, DESCRIPTOR), nestedDescriptor(resource(boot, nested)),
                                path + ": nested descriptor changed");
                    }
                }
                assertNull(boot.getEntry("BOOT-INF/classes/" + DESCRIPTOR), "Sample must not mask missing library grants");
            }
        }
    }

    @Test
    public void isolatedProductionJarsRoundTripPayloadsAndRejectUnapprovedData() throws Exception {
        runProbe(adapterJar(), null);
    }

    @Test
    public void omittedProductionPermissionsFailWithoutFixtureResources() throws Exception {
        final Map<String, List<Boolean>> entries;
        try (var jar = new JarFile(adapterJar().toFile())) {
            entries = permissions(resource(jar, DESCRIPTOR));
        }
        final var names = entries.keySet().stream().filter((name) -> name.startsWith("java.sql.")
                || name.equals("sun.util.calendar.ZoneInfo") || name.endsWith("LazyPropertyInitializer$1")
                || name.endsWith("PropertyAccessStrategyBackRefImpl$1") || name.endsWith(".GetProcessor")).toList();
        assertEquals(7, names.size(), "All regression controls must run");
        for (final String name : names) {
            final Path modified = this.temporaryDirectory.resolve("without-" + name + ".jar");
            withoutPermission(adapterJar(), modified, name);
            runProbe(modified, name);
        }
    }

    private void runProbe(Path adapter, String omitted) throws Exception {
        final Path helpers = this.temporaryDirectory.resolve("probe");
        for (final String name : List.of(ProductionSerializationProbe.class.getName(),
                ProductionSerializationProbe.class.getName() + "$UnapprovedValue")) {
            final String resource = name.replace('.', '/') + ".class";
            final Path copy = helpers.resolve(resource);
            Files.createDirectories(copy.getParent());
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                Files.copy(input, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // Explicit production dependencies: no target/classes, test-classes, test JARs,
        // shaded sample, Surefire or shared Hibernate functional fixture resources.
        final List<Path> classpath = List.of(helpers, adapter, location(ExternalizableHelper.class),
                location(LazyPropertyInitializer.class), location(jakarta.persistence.Entity.class),
                location(Class.forName("org.jboss.logging.Logger")));
        final List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dcoherence.security.mode=hardened", "-cp", String.join(File.pathSeparator, classpath.stream().map(Path::toString).toList()),
                ProductionSerializationProbe.class.getName()));
        if (omitted != null) {
            command.add(omitted);
        }
        final Path log = this.temporaryDirectory.resolve(((omitted != null) ? omitted : "production") + ".log");
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Production probe timed out: " + log);
            final String output = Files.readString(log);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("Unapproved data rejected; hardening active"), output);
            if (omitted != null) {
                assertTrue(output.contains("denied-class=" + omitted + "|"), output);
            }
            System.out.println(output);
        }
        finally {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    private static Path adapterJar() {
        return ROOT.resolve("coherence-hibernate-cache-7/target/coherence-hibernate-cache-7-"
                + System.getProperty("project.version") + ".jar");
    }

    private static Path location(Class<?> type) throws Exception {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static byte[] resource(JarFile jar, String name) throws Exception {
        assertEquals(1, jar.stream().filter((entry) -> entry.getName().equals(name)).count(), jar.getName() + ": " + name);
        try (var input = jar.getInputStream(jar.getJarEntry(name))) {
            return input.readAllBytes();
        }
    }

    private static byte[] nestedDescriptor(byte[] jar) throws Exception {
        byte[] descriptor = null;
        try (var input = new ZipInputStream(new ByteArrayInputStream(jar))) {
            for (var entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (entry.getName().equals(DESCRIPTOR)) {
                    assertNull(descriptor, "Duplicate nested descriptor");
                    descriptor = input.readAllBytes();
                }
            }
        }
        assertNotNull(descriptor, "Nested descriptor missing");
        return descriptor;
    }

    private static Map<String, List<Boolean>> permissions(byte[] xml) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        final Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getDocumentElement();
        assertEquals("http://xmlns.oracle.com/coherence/coherence-security-config", root.getNamespaceURI());
        assertEquals("security-config", root.getLocalName());
        assertEquals("1.0", root.getAttribute("version"));
        final Map<String, List<Boolean>> entries = new LinkedHashMap<>();
        final var nodes = root.getElementsByTagNameNS("http://xmlns.oracle.com/coherence/coherence-security-config", "class");
        assertTrue(nodes.getLength() > 0, "Packaged descriptor is empty");
        for (int i = 0; i < nodes.getLength(); i++) {
            final Element entry = (Element) nodes.item(i);
            for (int attribute = 0; attribute < entry.getAttributes().getLength(); attribute++) {
                assertTrue(Set.of("name", "source", "executable", "lambda-target")
                        .contains(entry.getAttributes().item(attribute).getNodeName()), "Unknown permission attribute");
            }
            final String name = entry.getAttribute("name");
            assertTrue(name.matches("[\\w$]+(\\.[\\w$]+)+"), name);
            assertEquals("allowed-classes", entry.getParentNode().getLocalName());
            for (final String flag : List.of("executable", "lambda-target")) {
                assertTrue(List.of("", "false", "true").contains(entry.getAttribute(flag)), "Invalid " + flag + ": " + name);
            }
            assertNull(entries.put(name, List.of(Boolean.parseBoolean(entry.getAttribute("executable")),
                    Boolean.parseBoolean(entry.getAttribute("lambda-target")))), "Duplicate permission: " + name);
        }
        assertEquals(nodes.getLength() + 1, root.getElementsByTagName("*").getLength(), "Unexpected security rule");
        return entries;
    }

    private static void mergePermissions(Map<String, List<Boolean>> target, Map<String, List<Boolean>> input) {
        input.forEach((name, flags) -> target.merge(name, flags,
                (first, second) -> List.of(first.get(0) || second.get(0), first.get(1) || second.get(1))));
    }

    private static void withoutPermission(Path original, Path modified, String name) throws Exception {
        try (var input = new JarFile(original.toFile()); var output = new JarOutputStream(Files.newOutputStream(modified))) {
            for (final JarEntry entry : input.stream().toList()) {
                byte[] bytes = resource(input, entry.getName());
                if (entry.getName().equals(DESCRIPTOR)) {
                    final String xml = new String(bytes, StandardCharsets.UTF_8);
                    final String changed = xml.replaceAll("(?m)^.*<class name=\"" + java.util.regex.Pattern.quote(name) + "\"[^>]+/>\\R?", "");
                    assertTrue(!changed.equals(xml), "Control did not remove " + name);
                    bytes = changed.getBytes(StandardCharsets.UTF_8);
                    assertTrue(!permissions(bytes).containsKey(name));
                }
                output.putNextEntry(new JarEntry(entry.getName()));
                output.write(bytes);
                output.closeEntry();
            }
        }
    }
}
