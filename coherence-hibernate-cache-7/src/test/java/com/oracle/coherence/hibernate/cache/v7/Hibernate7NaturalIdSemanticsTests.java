/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.util.List;
import java.util.Objects;

import com.tangosol.net.CacheFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.KeyType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Hibernate 7 natural-ID loading and cache strategy behavior.
 */
@ServiceRegistry(settings = {
        @Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
        @Setting(name = "hibernate.cache.region.factory_class", value = "com.oracle.coherence.hibernate.cache.v7.CoherenceRegionFactory"),
        @Setting(name = "com.oracle.coherence.hibernate.cache.cache_config_file_path", value = "tests-hibernate-second-level-cache-config.xml")
})
@org.hibernate.testing.orm.junit.SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
        Hibernate7NaturalIdSemanticsTests.MutableNaturalEntity.class,
        Hibernate7NaturalIdSemanticsTests.ReadOnlyNaturalEntity.class,
        Hibernate7NaturalIdSemanticsTests.CompositeNaturalEntity.class
})
public class Hibernate7NaturalIdSemanticsTests {

    @AfterEach
    public void cleanup(SessionFactoryScope scope) {
        scope.dropData();
        scope.getSessionFactory().getCache().evictAllRegions();
    }

    @AfterAll
    public static void shutdownCoherence() {
        CacheFactory.shutdown();
    }

    @Test
    public void mutableNonstrictNaturalIdInvalidatesOldValue(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final MutableNaturalEntity entity = new MutableNaturalEntity("old-code", "value");
            session.persist(entity);
            return entity.id;
        });
        assertThat(scope.getSessionFactory().getMappingMetamodel()
                .getEntityDescriptor(MutableNaturalEntity.class)
                .getNaturalIdCacheAccessStrategy().getAccessType())
                .isEqualTo(AccessType.NONSTRICT_READ_WRITE);

        scope.inTransaction((session) -> assertThat(session.find(
                MutableNaturalEntity.class, "old-code", KeyType.NATURAL).id).isEqualTo(id));
        scope.inTransaction((session) -> session.find(MutableNaturalEntity.class, id).code = "new-code");

        scope.inTransaction((session) -> {
            assertThat(session.find(MutableNaturalEntity.class, "old-code", KeyType.NATURAL)).isNull();
            assertThat(session.find(MutableNaturalEntity.class, "new-code", KeyType.NATURAL).id).isEqualTo(id);
        });
    }

    @Test
    public void readOnlyNaturalIdUsesReadOnlyStrategy(SessionFactoryScope scope) {
        final Long id = scope.fromTransaction((session) -> {
            final ReadOnlyNaturalEntity entity = new ReadOnlyNaturalEntity("fixed-code");
            session.persist(entity);
            return entity.id;
        });
        assertThat(scope.getSessionFactory().getMappingMetamodel()
                .getEntityDescriptor(ReadOnlyNaturalEntity.class)
                .getNaturalIdCacheAccessStrategy().getAccessType())
                .isEqualTo(AccessType.READ_ONLY);

        scope.inTransaction((session) -> assertThat(session.find(
                ReadOnlyNaturalEntity.class, "fixed-code", KeyType.NATURAL).id).isEqualTo(id));
        scope.inTransaction((session) -> assertThat(session.find(
                ReadOnlyNaturalEntity.class, "fixed-code", KeyType.NATURAL).id).isEqualTo(id));
    }

    @Test
    public void findAndFindMultipleLoadSimpleNaturalIds(SessionFactoryScope scope) {
        final Long firstId = persistMutable(scope, "first", "one");
        final Long secondId = persistMutable(scope, "second", "two");
        scope.getSessionFactory().getCache().evictEntityData(MutableNaturalEntity.class);
        scope.getSessionFactory().getCache().evictNaturalIdData(MutableNaturalEntity.class);

        scope.inTransaction((session) -> {
            assertThat(session.find(MutableNaturalEntity.class, "first", KeyType.NATURAL).id)
                    .isEqualTo(firstId);
            final List<MutableNaturalEntity> entities = session.findMultiple(
                    MutableNaturalEntity.class,
                    List.of("second", "missing", "first"),
                    KeyType.NATURAL);
            assertThat(entities).hasSize(3);
            assertThat(entities.get(0).id).isEqualTo(secondId);
            assertThat(entities.get(1)).isNull();
            assertThat(entities.get(2).id).isEqualTo(firstId);
        });
    }

    @Test
    public void naturalIdClassLoadsCompositeNaturalIds(SessionFactoryScope scope) {
        final Long firstId = persistComposite(scope, "US", "100");
        final Long secondId = persistComposite(scope, "GB", "200");
        scope.getSessionFactory().getCache().evictEntityData(CompositeNaturalEntity.class);
        scope.getSessionFactory().getCache().evictNaturalIdData(CompositeNaturalEntity.class);

        scope.inTransaction((session) -> {
            assertThat(session.find(
                    CompositeNaturalEntity.class,
                    new CompositeNaturalKey("US", "100"),
                    KeyType.NATURAL).id).isEqualTo(firstId);
            final List<CompositeNaturalEntity> entities = session.findMultiple(
                    CompositeNaturalEntity.class,
                    List.of(new CompositeNaturalKey("GB", "200"), new CompositeNaturalKey("US", "100")),
                    KeyType.NATURAL);
            assertThat(entities).extracting((entity) -> entity.id).containsExactly(secondId, firstId);
        });
    }

    private Long persistMutable(SessionFactoryScope scope, String code, String value) {
        return scope.fromTransaction((session) -> {
            final MutableNaturalEntity entity = new MutableNaturalEntity(code, value);
            session.persist(entity);
            return entity.id;
        });
    }

    private Long persistComposite(SessionFactoryScope scope, String country, String code) {
        return scope.fromTransaction((session) -> {
            final CompositeNaturalEntity entity = new CompositeNaturalEntity(country, code);
            session.persist(entity);
            return entity.id;
        });
    }

    @Entity(name = "MutableNaturalEntity")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "natural.mutable.entity")
    @NaturalIdCache(region = "natural.mutable.id")
    public static class MutableNaturalEntity {
        @Id
        @GeneratedValue
        private Long id;

        @NaturalId(mutable = true)
        private String code;

        private String value;

        public MutableNaturalEntity() {
        }

        public MutableNaturalEntity(String code, String value) {
            this.code = code;
            this.value = value;
        }
    }

    @Entity(name = "ReadOnlyNaturalEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "natural.read-only.entity")
    @NaturalIdCache(region = "natural.read-only.id")
    public static class ReadOnlyNaturalEntity {
        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String code;

        public ReadOnlyNaturalEntity() {
        }

        public ReadOnlyNaturalEntity(String code) {
            this.code = code;
        }
    }

    public static class CompositeNaturalKey {
        public String country;
        public String code;

        public CompositeNaturalKey() {
        }

        public CompositeNaturalKey(String country, String code) {
            this.country = country;
            this.code = code;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof CompositeNaturalKey)) {
                return false;
            }
            final CompositeNaturalKey that = (CompositeNaturalKey) object;
            return Objects.equals(this.country, that.country) && Objects.equals(this.code, that.code);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.country, this.code);
        }
    }

    @Entity(name = "CompositeNaturalEntity")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "natural.composite.entity")
    @NaturalIdCache(region = "natural.composite.id")
    @NaturalIdClass(CompositeNaturalKey.class)
    public static class CompositeNaturalEntity {
        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String country;

        @NaturalId
        private String code;

        public CompositeNaturalEntity() {
        }

        public CompositeNaturalEntity(String country, String code) {
            this.country = country;
            this.code = code;
        }
    }
}
