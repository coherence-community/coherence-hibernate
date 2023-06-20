/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.config;

import javax.sql.DataSource;

import org.hsqldb.server.Server;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Start HSQLDB in server mode on port 9001 with username "coherence" and password "rocks".
 *
 * @author Gunnar Hillert
 */
@Configuration
public class DatabaseConfig {

    /**
     * Bootstraps a simple in-memory HSQSL Server instance.
     * @return the HSQLDB Server object that acts as a network database server
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server hsqlServer() {
        final Server server = new Server();
        server.setAddress("localhost");
        server.setPort(9001);
        server.setSilent(false);
        server.setDatabaseName(0, "mainDb");
        server.setDatabasePath(0, "mem:mainDb;user=coherence;password=rocks");
        return server;
    }

    /**
     * Returns the datasource. Spring bean specifies a dependency on the {@link DatabaseConfig#hsqlServer()} bean.
     * @param dataSourceProperties the Spring Boot DataSource properties
     * @return the configured DataSource
     */
    @Bean
    @DependsOn("hsqlServer")
    public DataSource getDataSource(DataSourceProperties dataSourceProperties) {
        final DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(dataSourceProperties.getDriverClassName());
        dataSourceBuilder.url(dataSourceProperties.getUrl());
        dataSourceBuilder.username(dataSourceProperties.getUsername());
        dataSourceBuilder.password(dataSourceProperties.getPassword());
        return dataSourceBuilder.build();
    }

}
