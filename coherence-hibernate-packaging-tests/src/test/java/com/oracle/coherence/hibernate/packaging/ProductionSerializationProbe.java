/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.packaging;

import java.io.InvalidClassException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.oracle.coherence.hibernate.cache.v7.access.processor.AfterInsertProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.AfterUpdateProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.GetProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.PutFromLoadProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.ReadWritePutFromLoadProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.SoftLockItemProcessor;
import com.oracle.coherence.hibernate.cache.v7.access.processor.SoftUnlockItemProcessor;
import com.oracle.coherence.hibernate.cache.v7.region.CoherenceRegionValue;
import com.tangosol.util.ExternalizableHelper;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.internal.util.ZonedDateTimeComparator;
import org.hibernate.internal.util.compare.CalendarComparator;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.internal.util.compare.RowVersionComparator;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;

/** A standalone child JVM with only production JARs and these two helper classes. */
public final class ProductionSerializationProbe {

    private ProductionSerializationProbe() {
    }

    public static void main(String[] args) throws Exception {
        final Class<?> mode = Class.forName("com.tangosol.internal.util.CoherenceMode");
        require(Boolean.TRUE.equals(mode.getMethod("isSecurityHardeningEnabled").invoke(null)), "Hardening must be active");
        final var resources = Collections.list(ProductionSerializationProbe.class.getClassLoader()
                .getResources("META-INF/coherence/security-config.xml"));
        require(resources.size() == 2, "Expected only adapter and Coherence descriptors: " + resources);
        for (final var resource : resources) {
            require(resource.getProtocol().equals("jar"), "Production descriptor must come from JAR: " + resource);
            System.out.println("Production descriptor: " + resource);
            try (var input = resource.openStream()) {
                final String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                require(!xml.contains("Tests$") && !xml.contains("WrappedKey"), "Fixture descriptor leaked: " + resource);
            }
        }
        System.out.println("Production Coherence: " + ExternalizableHelper.class.getProtectionDomain().getCodeSource());
        final Map<String, Serializable> payloads = payloads();
        if (args.length > 0) {
            final Serializable value = payloads.get(args[0]);
            require(value != null, "No omission control for " + args[0]);
            requireRejection(value);
            System.out.println("Omitted permission rejected: " + args[0]);
        }
        else {
            for (final var entry : payloads.entrySet()) {
                verifyGrant(entry.getKey(), entry.getValue() instanceof GetProcessor);
                final Object copy = roundTrip(entry.getValue());
                if (entry.getValue() == LazyPropertyInitializer.UNFETCHED_PROPERTY
                        || entry.getValue() == PropertyAccessStrategyBackRefImpl.UNKNOWN) {
                    require(copy == entry.getValue(), "Hibernate marker lost singleton identity");
                }
                else if (entry.getValue() instanceof Calendar || entry.getValue() instanceof java.util.Date) {
                    require(entry.getValue().equals(copy), "Temporal state changed: " + entry.getKey());
                }
            }
            final Calendar calendar = (Calendar) payloads.get("sun.util.calendar.ZoneInfo");
            verifyGrant(CoherenceRegionValue.class.getName(), false);
            verifyGrant(CoherenceRegionValue.SoftLock.class.getName(), false);
            final CoherenceRegionValue value = new CoherenceRegionValue(payloads.values().toArray(Serializable[]::new), calendar, 123L);
            value.addSoftLock(new CoherenceRegionValue.SoftLock(UUID.randomUUID(), 1L, 999L));
            final CoherenceRegionValue copy = (CoherenceRegionValue) roundTrip(value);
            require(copy.isSoftLocked() && copy.getTimestamp() == 123L && calendar.equals(copy.getVersion()), "Adapter state changed");
            final var lock = new CoherenceRegionValue.SoftLock(UUID.randomUUID(), 2L, 1000L);
            for (final Serializable operation : List.of(new GetProcessor(), new AfterInsertProcessor<>(value),
                    new AfterUpdateProcessor(value, lock, 1L), new PutFromLoadProcessor(false, value),
                    new SoftLockItemProcessor(value, lock), new SoftUnlockItemProcessor(lock, 1L))) {
                verifyGrant(operation.getClass().getName(), true);
                require(operation.getClass() == roundTrip(operation).getClass(), "Processor type changed");
            }
            for (final var comparator : List.of(ComparableComparator.INSTANCE, ZonedDateTimeComparator.INSTANCE,
                    RowVersionComparator.INSTANCE, CalendarComparator.INSTANCE, OffsetDateTimeJavaType.INSTANCE.getComparator())) {
                roundTrip(new ReadWritePutFromLoadProcessor(false, 1L, value, comparator));
            }
            System.out.println("Production payloads, singleton markers, temporal precision, locks and processors passed");
        }
        requireRejection(new UnapprovedValue());
        requireRejection(new CoherenceRegionValue(new Serializable[] {new UnapprovedValue()}, null, 1L));
        System.out.println("Unapproved data rejected; hardening active");
    }

    private static Map<String, Serializable> payloads() {
        final Map<String, Serializable> values = new LinkedHashMap<>();
        values.put(Timestamp.class.getName(), Timestamp.valueOf("2026-10-19 10:20:30.123456789"));
        values.put(java.sql.Date.class.getName(), java.sql.Date.valueOf("2026-10-19"));
        values.put(java.sql.Time.class.getName(), java.sql.Time.valueOf("10:20:30"));
        values.put(LazyPropertyInitializer.UNFETCHED_PROPERTY.getClass().getName(), LazyPropertyInitializer.UNFETCHED_PROPERTY);
        values.put(PropertyAccessStrategyBackRefImpl.UNKNOWN.getClass().getName(), PropertyAccessStrategyBackRefImpl.UNKNOWN);
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/Belgrade"));
        calendar.setTimeInMillis(OffsetDateTime.parse("2026-10-19T10:20:30.123Z").toInstant().toEpochMilli());
        require(calendar.getTimeZone().getClass().getName().equals("sun.util.calendar.ZoneInfo"), "Real ZoneInfo graph required");
        values.put(calendar.getTimeZone().getClass().getName(), calendar);
        values.put(GetProcessor.class.getName(), new GetProcessor());
        return values;
    }

    private static Object roundTrip(Serializable value) {
        return ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(value));
    }

    private static void verifyGrant(String name, boolean executable) throws Exception {
        final Class<?> configType = Class.forName("com.tangosol.internal.util.security.SecurityConfig");
        final Object config = configType.getMethod("current").invoke(null);
        require(Boolean.TRUE.equals(configType.getMethod("contains", String.class).invoke(config, name)), "Missing effective grant: " + name);
        require(Boolean.valueOf(executable).equals(configType.getMethod("isExecutable", String.class).invoke(config, name)),
                "Incorrect effective executable flag: " + name);
        require(Boolean.FALSE.equals(configType.getMethod("isLambdaTarget", String.class).invoke(config, name)),
                "Unexpected lambda-target privilege: " + name);
    }

    private static void requireRejection(Serializable value) {
        try {
            roundTrip(value);
        }
        catch (RuntimeException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof InvalidClassException && cause.getMessage().contains("REJECTED")) {
                    return;
                }
            }
            throw new AssertionError("Expected serialization filter rejection", failure);
        }
        throw new AssertionError("Expected rejection for " + value.getClass().getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class UnapprovedValue implements Serializable {
    }
}
