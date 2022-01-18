/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.cache.v53.configuration;

import com.oracle.coherence.hibernate.cache.v53.configuration.session.SessionType;
import com.oracle.coherence.hibernate.cache.v53.configuration.support.CoherenceHibernateProperties;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Test covering {@link CoherenceHibernateProperties}.
 *
 * @author Gunnar Hillert
 */
public class CoherenceHibernatePropertiesTests {

	@Test
	public void retrieveEmptyCoherenceProperties() {

		final Map rawHibernateProperties = new HashMap();

		final CoherenceHibernateProperties coherenceHibernateProperties = new CoherenceHibernateProperties(rawHibernateProperties);

		assertThat(coherenceHibernateProperties).isNotNull();
		assertThat(coherenceHibernateProperties.getSessionType()).isNull();
		assertThat(coherenceHibernateProperties.getSessionName()).isNull();
		assertThat(coherenceHibernateProperties.getCacheConfigFilePath()).isEqualTo("hibernate-second-level-cache-config.xml");
		assertThat(coherenceHibernateProperties.getCoherenceProperties()).isEmpty();
	}

	@Test
	public void retrieveCoherenceProperties() {

		final Map rawHibernateProperties = new HashMap();

		rawHibernateProperties.put("com.oracle.coherence.hibernate.cache.session_name", "bar");
		rawHibernateProperties.put("com.oracle.coherence.hibernate.cache.session_type", "SERVER");
		rawHibernateProperties.put("com.oracle.coherence.hibernate.cache.cache_config_file_path", "foo.xml");
		rawHibernateProperties.put("com.oracle.coherence.hibernate.cache.coherence_properties.foo.bar", "hello world");

		final CoherenceHibernateProperties coherenceHibernateProperties = new CoherenceHibernateProperties(rawHibernateProperties);

		assertThat(coherenceHibernateProperties).isNotNull();
		assertThat(coherenceHibernateProperties.getSessionType()).isEqualTo(SessionType.SERVER);
		assertThat(coherenceHibernateProperties.getSessionName()).isEqualTo("bar");
		assertThat(coherenceHibernateProperties.getCacheConfigFilePath()).isEqualTo("foo.xml");
		assertThat(coherenceHibernateProperties.getCoherenceProperties()).hasSize(1);
	}

	@Test
	public void retrieveEmptySessionTypeOfCoherenceProperties() {

		final Map rawHibernateProperties = new HashMap();

		rawHibernateProperties.put("com.oracle.coherence.hibernate.cache.session_type", "client");

		final CoherenceHibernateProperties coherenceHibernateProperties = new CoherenceHibernateProperties(rawHibernateProperties);

		assertThat(coherenceHibernateProperties.getSessionType()).isEqualTo(SessionType.CLIENT);
	}

	@Test
	public void retrieveLowerCaseSessionTypeOfCoherenceProperties() {

		final Map rawHibernateProperties = new HashMap();

		rawHibernateProperties.put("com.oracle.coherence.hibernate.cache.session_type", "");

		try {
			new CoherenceHibernateProperties(rawHibernateProperties);
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage()).isEqualTo("The sessionType cannot be an empty String.");
			return;
		}
		fail("Was expecting an IllegalArgumentException to be thrown.");
	}

}
