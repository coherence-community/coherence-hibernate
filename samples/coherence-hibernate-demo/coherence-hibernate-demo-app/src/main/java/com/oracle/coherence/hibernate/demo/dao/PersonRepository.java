/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.dao;

import com.oracle.coherence.hibernate.demo.model.Person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Repository for {@link Person}s.
 * @author Gunnar Hillert
 *
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

}
