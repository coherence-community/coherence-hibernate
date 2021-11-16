/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.configuration;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oracle.coherence.hibernate.demo.model.Person;

public abstract class EventMixin {
	@JsonIgnore public Set<Person> participants;
}
