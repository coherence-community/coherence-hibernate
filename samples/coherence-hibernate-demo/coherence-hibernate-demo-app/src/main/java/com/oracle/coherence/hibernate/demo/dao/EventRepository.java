/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.dao;

import com.oracle.coherence.hibernate.demo.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;

/**
 * @author Gunnar Hillert
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
	@Query("SELECT event FROM Event event left join fetch event.participants p where event.id = :eventId")
	@QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
	Optional<Event> getEventWithParticipants(@Param("eventId") Long eventId);
}
