/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v6.configuration.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.coherence.hibernate.cache.v6.configuration.session.SessionType;

/**
 * Defines all Hibernate Properties that are specific for Coherence Hibernate.
 *
 * @author Gunnar Hillert
 * @since 2.1
 */
public class CoherenceHibernateProperties {

    /**
     * The prefix of the names of all properties specific to this SPI implementation.
     */
    public static final String PROPERTY_NAME_PREFIX = "com.oracle.coherence.hibernate.cache.";

    /**
     * The name of the property specifying the path to the Coherence cache configuration file.
     */
    public static final String CACHE_CONFIG_FILE_PATH_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "cache_config_file_path";

    /**
     * The default path to the cache configuration file.
     */
    public static final String CACHE_CONFIG_FILE_PATH_DEFAULT_VALUE = "hibernate-second-level-cache-config.xml";

    /**
     * By default, the DefaultCacheServer will not be bootstrapped and this property is false by default.
     */
    public static final String START_CACHE_SERVER_DEFAULT_VALUE = "false";

    /**
     * The name of the property specifying the Coherence-specific logger. This will set the logger of the Coherence
     * sub-system.
     */
    public static final String COHERENCE_LOGGER_PROPERTY_NAME = "coherence.log";

    /**
     * The name of the property specifying the default value for the logger implementation to be used.
     */
    public static final String COHERENCE_LOGGER_DEFAULT_VALUE = "slf4j";

    /**
     * The name of the property specifying the session name.
     */
    public static final String COHERENCE_SESSION_NAME_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "session_name";

    /**
     * The name of the property specifying the Coherence session type.
     */
    public static final String COHERENCE_SESSION_TYPE_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "session_type";

    /**
     * The name of the property specifying whether to start the Coherence cache server.
     */
    public static final String START_CACHE_SERVER_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "start_cache_server";

    /**
     * By default, empty, indicating that no Coherence property prefix is applied.
     */
    public static final String DEFAULT_PROPERTY_PREFIX = PROPERTY_NAME_PREFIX + "coherence_properties.";

    private final String cacheConfigFilePath;
    private final String sessionName;
    private final SessionType sessionType;

    private final boolean startCacheServer;

    private final Map<String, Object> coherenceProperties;

    public CoherenceHibernateProperties(Map hibernateProperties) {

        this.coherenceProperties = this.getCoherenceSystemProperties(hibernateProperties);

        String cacheConfigFilePath = (hibernateProperties != null) ?
                (String) hibernateProperties.get(CoherenceHibernateProperties.CACHE_CONFIG_FILE_PATH_PROPERTY_NAME) : null;

        if (cacheConfigFilePath == null) {
            cacheConfigFilePath = System.getProperty(
                    CoherenceHibernateProperties.CACHE_CONFIG_FILE_PATH_PROPERTY_NAME,
                    CoherenceHibernateProperties.CACHE_CONFIG_FILE_PATH_DEFAULT_VALUE);
        }

        this.cacheConfigFilePath = cacheConfigFilePath;

        String sessionName = (hibernateProperties != null) ?
                (String) hibernateProperties.get(CoherenceHibernateProperties.COHERENCE_SESSION_NAME_PROPERTY_NAME) : null;

        if (sessionName == null) {
            sessionName = System.getProperty(
                    CoherenceHibernateProperties.COHERENCE_SESSION_NAME_PROPERTY_NAME,
                    null);
        }

        this.sessionName = sessionName;

        String sessionTypeAsString = (hibernateProperties != null) ?
                (String) hibernateProperties.get(CoherenceHibernateProperties.COHERENCE_SESSION_TYPE_PROPERTY_NAME) : null;

        if (sessionTypeAsString == null) {
            sessionTypeAsString = System.getProperty(
                    CoherenceHibernateProperties.COHERENCE_SESSION_TYPE_PROPERTY_NAME,
                    null);
        }

        if (sessionTypeAsString != null) {
            Assert.hasText(sessionTypeAsString, "The sessionType cannot be an empty String.");
            this.sessionType = SessionType.valueOf(sessionTypeAsString.toUpperCase());
        }
        else {
            this.sessionType = null;
        }

        String startCacheServer = (hibernateProperties != null) ?
                (String) hibernateProperties.get(CoherenceHibernateProperties.START_CACHE_SERVER_PROPERTY_NAME) : null;

        if (startCacheServer == null) {
            startCacheServer = System.getProperty(
                    CoherenceHibernateProperties.START_CACHE_SERVER_PROPERTY_NAME,
                    CoherenceHibernateProperties.START_CACHE_SERVER_DEFAULT_VALUE);
        }

        this.startCacheServer = Boolean.valueOf(startCacheServer);

    }

    public String getCacheConfigFilePath() {
        return this.cacheConfigFilePath;
    }

    public String getSessionName() {
        return this.sessionName;
    }

    public SessionType getSessionType() {
        return this.sessionType;
    }

    public Map<String, Object> getCoherenceProperties() {
        return this.coherenceProperties;
    }

    public boolean isStartCacheServer() {
        return this.startCacheServer;
    }

    private Map<String, Object> getCoherenceSystemProperties(Map hibernateProperties) {
        final Map<String, Object> resolvedCoherenceProperties = new ConcurrentHashMap<>(0);

        if (hibernateProperties != null) {
            for (Object rawEntry : hibernateProperties.entrySet()) {
                final Map.Entry entry = (Map.Entry) rawEntry;
                final Object key = entry.getKey();
                final Object value = entry.getValue();

                Assert.notNull(key, "The key of the Hibernate property must not be null.");
                Assert.notNull(value, "The value of the Hibernate property must not be null.");

                if (!(key instanceof String)) {
                    throw new IllegalStateException("Map key should be a String.");
                }

                final String keyAsString = (String) key;

                if (keyAsString.startsWith(DEFAULT_PROPERTY_PREFIX)) {
                    final String keyWithoutPrefix = keyAsString.replace(DEFAULT_PROPERTY_PREFIX, "");

                    if (value instanceof String) {
                        resolvedCoherenceProperties.put(keyWithoutPrefix, value);
                    }
                    else {
                        throw new IllegalStateException(String.format("Coherence property '%s' must be an instance of String.", value));
                    }
                }
            }
        }

        return resolvedCoherenceProperties;
    }
}
