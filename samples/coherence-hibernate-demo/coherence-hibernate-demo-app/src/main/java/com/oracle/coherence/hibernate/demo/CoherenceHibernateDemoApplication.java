/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Main entry point to the Coherence Hibernate demo application.
 * @author Gunnar Hillert
 */
@SpringBootApplication
public class CoherenceHibernateDemoApplication {

    public static void main(String... args) {

        /*
         * Usually when testing Coherence locally, where the whole Coherence cluster runs on e.g. a laptop,
         * restricting the cluster formation to loopback (127.0.0.1) is often useful. The same properties are also
         * set by the Maven pom.xml for the JUnit tests.
         */
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("coherence.wka", "127.0.0.1");

        SpringApplication.run(CoherenceHibernateDemoApplication.class, args);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addCustomBigDecimalDeserialization() {
        return (builder) -> {
            // Customize if needed
        };
    }
}
