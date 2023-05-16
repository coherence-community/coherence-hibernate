/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.config;

import java.io.IOException;

import javax.sql.DataSource;

import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server hsqlServer() throws ServerAcl.AclFormatException, IOException {
        final Server server = new Server();
        server.setAddress("localhost");
        server.setPort(9001);
        server.setSilent(false);
        server.setDatabaseName(0, "mainDb");
        server.setDatabasePath(0, "mem:mainDb;user=coherence;password=rocks");
        return server;
    }

    @Bean
    @DependsOn("hsqlServer")
    public DataSource getDataSource(
            @Autowired DataSourceProperties dsProps) {
        final DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(dsProps.getDriverClassName());
        dataSourceBuilder.url(dsProps.getUrl());
        dataSourceBuilder.username(dsProps.getUsername());
        dataSourceBuilder.password(dsProps.getPassword());
        return dataSourceBuilder.build();
    }

}
