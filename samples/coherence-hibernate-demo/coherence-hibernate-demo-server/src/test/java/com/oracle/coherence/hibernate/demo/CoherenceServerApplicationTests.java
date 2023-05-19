/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hibernate.demo;

import org.assertj.core.api.Assertions;
import org.hsqldb.server.Server;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Gunnar Hillert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoherenceServerApplicationTests {

    @Autowired
    private Server databaseServer;

    @Test
    void contextLoads() {
        Assertions.assertThat(this.databaseServer.isNotRunning()).isFalse();
    }
}
