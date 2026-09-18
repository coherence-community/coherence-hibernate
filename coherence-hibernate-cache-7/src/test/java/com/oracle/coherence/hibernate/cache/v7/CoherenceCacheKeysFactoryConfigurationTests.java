/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v7;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tangosol.net.Session;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.service.NullServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies supported cache-key factory configuration forms and invalid configuration handling.
 */
public class CoherenceCacheKeysFactoryConfigurationTests {

    private final Logger logger = (Logger) LoggerFactory.getLogger(CoherenceRegionFactory.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final CoherenceRegionFactory factory = new CoherenceRegionFactory(noOpProxy(Session.class));
    private final StandardServiceRegistry registry = new StandardServiceRegistryBuilder().build();
    private final SessionFactoryOptions options = optionsWithRegistry(this.registry);
    private Level previousLevel;
    private String previousCoherenceLog;

    @BeforeEach
    public void captureDiagnostics() {
        this.previousLevel = this.logger.getLevel();
        this.previousCoherenceLog = System.getProperty("coherence.log");
        this.logger.setLevel(Level.WARN);
        this.appender.start();
        this.logger.addAppender(this.appender);
    }

    @AfterEach
    public void cleanup() {
        try {
            this.factory.stop();
        }
        finally {
            StandardServiceRegistryBuilder.destroy(this.registry);
            this.logger.detachAppender(this.appender);
            this.appender.stop();
            this.logger.setLevel(this.previousLevel);
            if (this.previousCoherenceLog == null) {
                System.clearProperty("coherence.log");
            }
            else {
                System.setProperty("coherence.log", this.previousCoherenceLog);
            }
        }
    }

    @Test
    public void explicitSimpleFactoryIsUsedWithoutWarning() {
        startWithFactory("simple");

        assertThat(this.factory.getImplicitCacheKeysFactory()).isInstanceOf(SimpleCacheKeysFactory.class);
        assertThat(this.appender.list).isEmpty();
    }

    @Test
    public void absentSettingUsesDefaultFactoryWithoutWarning() {
        this.factory.start(this.options, Map.of());

        assertThat(this.factory.getImplicitCacheKeysFactory()).isSameAs(DefaultCacheKeysFactory.INSTANCE);
        assertThat(this.appender.list).isEmpty();
    }

    @Test
    public void explicitDefaultFactoryIsSupported() {
        startWithFactory("default");

        assertThat(this.factory.getImplicitCacheKeysFactory()).isInstanceOf(DefaultCacheKeysFactory.class);
    }

    @Test
    public void customFactoryInstanceIsPreserved() {
        final CustomCacheKeysFactory customFactory = new CustomCacheKeysFactory();
        startWithFactory(customFactory);

        assertThat(this.factory.getImplicitCacheKeysFactory()).isSameAs(customFactory);
    }

    @Test
    public void customFactoryClassIsSupported() {
        startWithFactory(CustomCacheKeysFactory.class);

        assertThat(this.factory.getImplicitCacheKeysFactory()).isInstanceOf(CustomCacheKeysFactory.class);
    }

    @Test
    public void customFactoryClassNameIsSupported() {
        startWithFactory(CustomCacheKeysFactory.class.getName());

        assertThat(this.factory.getImplicitCacheKeysFactory()).isInstanceOf(CustomCacheKeysFactory.class);
    }

    @Test
    public void unknownFactoryPreventsRegionCreation() {
        startWithFactory("no.such.CacheKeysFactory");

        assertResolutionError("no.such.CacheKeysFactory", StrategySelectionException.class);
        assertThatThrownBy(() -> this.factory.buildDomainDataRegion(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(StrategySelectionException.class);
    }

    @Test
    public void incompatibleFactoryClassPreventsRegionCreation() {
        startWithFactory(String.class);

        assertResolutionError(String.class.toString(), ClassCastException.class);
        assertThatThrownBy(() -> this.factory.buildQueryResultsRegion("invalid", null))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(ClassCastException.class);
    }

    @Test
    public void missingRegistryReportsConfigurationError() {
        this.factory.start(optionsWithRegistry(null), Map.of("hibernate.cache.keys_factory", "simple"));

        assertResolutionError("simple", CacheException.class);
        assertThatThrownBy(() -> this.factory.buildDomainDataRegion(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(CacheException.class)
                .hasRootCauseMessage("A Hibernate service registry is required to configure hibernate.cache.keys_factory");
    }

    @Test
    public void missingStrategySelectorReportsMissingService() {
        final StandardServiceRegistry missingSelectorRegistry = StandardServiceRegistry.class.cast(Proxy.newProxyInstance(
                StandardServiceRegistry.class.getClassLoader(), new Class<?>[] {StandardServiceRegistry.class},
                (proxy, method, arguments) -> method.isDefault()
                        ? InvocationHandler.invokeDefault(proxy, method, arguments) : null));
        this.factory.start(optionsWithRegistry(missingSelectorRegistry), Map.of("hibernate.cache.keys_factory", "simple"));

        assertResolutionError("simple", NullServiceException.class);
        assertThatThrownBy(() -> this.factory.buildDomainDataRegion(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(NullServiceException.class);
    }

    private void assertResolutionError(String configuredValue, Class<? extends Throwable> causeType) {
        assertThat(this.appender.list).singleElement().satisfies((event) -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).contains("hibernate.cache.keys_factory", configuredValue);
            assertThat(event.getThrowableProxy()).isNotNull();
            assertThat(event.getThrowableProxy().getClassName()).isEqualTo(causeType.getName());
        });
    }

    private void startWithFactory(Object configuredFactory) {
        this.factory.start(this.options, Map.of("hibernate.cache.keys_factory", configuredFactory));
    }

    private static SessionFactoryOptions optionsWithRegistry(StandardServiceRegistry registry) {
        return SessionFactoryOptions.class.cast(Proxy.newProxyInstance(
                SessionFactoryOptions.class.getClassLoader(), new Class<?>[] {SessionFactoryOptions.class},
                (proxy, method, arguments) -> "getServiceRegistry".equals(method.getName()) ? registry : null));
    }

    private static <T> T noOpProxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, arguments) -> null));
    }

    public static class CustomCacheKeysFactory extends DefaultCacheKeysFactory {
    }
}
