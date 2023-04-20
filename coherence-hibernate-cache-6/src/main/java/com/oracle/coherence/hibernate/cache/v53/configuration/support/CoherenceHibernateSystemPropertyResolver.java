/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.configuration.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class that makes Coherence-bound configuration properties available as System property.
 * <p>
 * This class needs to be eagerly instantiated by Coherence Hibernate before any Coherence class that might need properties.
 *
 * @author Gunnar Hillert
 * @since 2.1
 */
public class CoherenceHibernateSystemPropertyResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoherenceHibernateSystemPropertyResolver.class);

	/**
	 * The Coherence properties to be used.
	 */
	private volatile Map<String, Object> coherenceProperties = new ConcurrentHashMap<>(0);

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
		this.coherenceProperties = coherenceProperties;

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Providing the following coherenceProperties: {}", coherenceProperties);
		}
	}

	public String getProperty(String coherenceProperty) {
		Assert.notNull(coherenceProperty, "coherenceProperty must not be null.");
		if (this.coherenceProperties != null && !this.coherenceProperties.isEmpty()) {
			final Object propertyValue = this.coherenceProperties.get(coherenceProperty);
			if (propertyValue == null) {
				return null;
			}

			if (propertyValue instanceof String) {
				return (String) propertyValue;
			}
			else {
				throw new IllegalStateException(
						String.format("Coherence property '%s' must be an instance of String.", coherenceProperty));
			}
		} else {
			return null;
		}
	}
	public void unset() {
		for (String propertyKey : this.coherenceProperties.keySet()) {
			System.clearProperty(propertyKey);
		}
	}

	public synchronized void addCoherenceProperty(String key, String value) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Adding Coherence Property - key: {}, value: {}.", key, value);
		}
		this.coherenceProperties.put(key, value);
	}

	public void initialize() {
		for (String propertyKey : this.coherenceProperties.keySet()) {
			System.setProperty(propertyKey, this.getProperty(propertyKey));
		}
	}
}
