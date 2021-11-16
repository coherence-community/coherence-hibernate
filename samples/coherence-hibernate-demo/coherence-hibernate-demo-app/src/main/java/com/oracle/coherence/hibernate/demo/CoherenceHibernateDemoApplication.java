/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import com.oracle.coherence.hibernate.demo.configuration.EventMixin;
import com.oracle.coherence.hibernate.demo.model.Event;

/**
 *
 * @author Gunnar Hillert
 *
 */
@SpringBootApplication
public class CoherenceHibernateDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoherenceHibernateDemoApplication.class, args);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer addCustomBigDecimalDeserialization() {
		return builder -> {
			builder.mixIn(Event.class, EventMixin.class);
		};
	}
}
