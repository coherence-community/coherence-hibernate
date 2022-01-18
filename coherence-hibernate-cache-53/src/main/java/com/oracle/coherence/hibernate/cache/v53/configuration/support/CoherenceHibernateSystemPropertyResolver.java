/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.configuration.support;

import com.tangosol.coherence.config.EnvironmentVariableResolver;
import com.tangosol.coherence.config.SystemPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Coherence {@link SystemPropertyResolver} and {@link EnvironmentVariableResolver} that uses a configurable {@link Map}
 * to return Coherence configuration properties.
 * <p>
 * This class needs to be eagerly instantiated by Coherence Hibernate before any Coherence class that might need properties.
 *
 * @author Gunnar Hillert
 * @since 2.1
 */
public class CoherenceHibernateSystemPropertyResolver
		implements EnvironmentVariableResolver, SystemPropertyResolver  {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceHibernateSystemPropertyResolver.class);


	/**
	 * The Coherence properties to be resolved.
	 */
	private static volatile Map<String, Object> coherenceProperties = new ConcurrentHashMap<>(0);

	/**
	 * This constructor is required so that Coherence can discover
	 * and instantiate this class using the Java ServiceLoader.
	 */
	public CoherenceHibernateSystemPropertyResolver() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Constructing CoherenceHibernateSystemPropertyResolver.");
		}
	}

	/**
	 * This constructor will be called by Coherence Hibernate to instantiate the
	 * singleton bean and set the {@link #coherenceProperties}.
	 * @param coherenceProperties the Coherence properties. Must not be null.
	 */
	public CoherenceHibernateSystemPropertyResolver(Map<String, Object> coherenceProperties) {
		this();
		Assert.notNull(coherenceProperties, "coherenceProperties must not be null.");
		CoherenceHibernateSystemPropertyResolver.coherenceProperties = coherenceProperties;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Providing the following coherenceProperties: {}", coherenceProperties);
		}
	}

	@Override
	public String getProperty(String coherenceProperty) {
		if (CoherenceHibernateSystemPropertyResolver.coherenceProperties != null) {
			final Object property = CoherenceHibernateSystemPropertyResolver.coherenceProperties.get(coherenceProperty);
			if (property != null) {
				if (property instanceof String) {
					return (String) property;
				}
				else {
					throw new IllegalStateException(
							String.format("Coherence property '%s' must be an instance of String.", coherenceProperty));
				}
			}
		}
		return System.getProperty(coherenceProperty);
	}

	@Override
	public String getEnv(String coherenceProperty) {
		if (CoherenceHibernateSystemPropertyResolver.coherenceProperties != null) {
			final Object property = CoherenceHibernateSystemPropertyResolver.coherenceProperties.get(coherenceProperty);
			if (property instanceof String) {
				return (String) property;
			}
			else {
				throw new IllegalStateException(
						String.format("Coherence property '%s' must be an instance of String.", coherenceProperty));
			}
		}
		return System.getenv(coherenceProperty);
	}

	public synchronized void addCoherenceProperty(String key, String value) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Adding Coherence Property - key: {}, value: {}.", key, value);
		}
		CoherenceHibernateSystemPropertyResolver.coherenceProperties.put(key, value);
	}
}
