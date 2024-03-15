/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import com.oracle.coherence.hibernate.demo.controller.dto.PersonDto;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.oracle.coherence.hibernate.demo.model.Person;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full integration test that verifies several Hibernate second-level cache scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoherenceHibernateDemoApplicationTests {

    private static final String CACHE_PREFIX = "foobar.";
    private static final String EVENT_ENTITY_CACHE_NAME = CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Event";
    private static final String PERSON_ENTITY_CACHE_NAME = CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Person";

    private static final String EVENT_PARTICIPANTS_COLLECTION_CACHE_NAME = CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Event.participants";
    private static final String PERSON_EVENTS_COLLECTION_CACHE_NAME = CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Person.events";

    private static final String DEFAULT_QUERY_RESULTS_REGION = CACHE_PREFIX + "default-query-results-region";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    private String eventId;
    private String person1Id;
    private String person2Id;

    private TestStats testStats;

    /*
     * When using @TestInstance(TestInstance.Lifecycle.PER_CLASS), you can't use @BeforeAll as that is too late for setting
     * system property "coherence.cacheconfig".
     */
    static {
        System.setProperty("coherence.cacheconfig", "hibernate-second-level-cache-config.xml");
        System.setProperty("coherence.distributed.localstorage", "true");
    }

    @BeforeEach
    void setup() {
        this.testStats = new TestStats(this.em);
    }

    /**
     * First, lets make sure the context loads.
     */
    @Test
    @Order(1)
    void contextLoads() {
        assertThat(this.testStats.getCoherenceEventCacheSize()).isZero();
        assertThat(this.testStats.getCoherencePersonCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isZero();
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isZero();
        assertThat(this.testStats.getCacheMissCount()).isZero();
        assertThat(this.testStats.getCachePutCount()).isZero();

        assertThat(this.testStats.getQueryCacheHitCount()).isZero();
        assertThat(this.testStats.getQueryCacheMissCount()).isZero();
        assertThat(this.testStats.getQueryCachePutCount()).isZero();

        assertThat(this.testStats.getCacheRegionNamesSize()).isEqualTo(5);
    }

    /**
     * In this test, we create a single {@link Event}. This should lead to the following cache expectations:
     *
     * <ul>
     *     <li>Upon persisting the Event, the event will also be PUT into the cache</li>
     *     <li>The size of the Coherence Event cache should be 1</li>
     * </ul>
     *
     */
    @Test
    @Order(2)
    void createOneEvent() throws Exception {

        final MockHttpServletRequestBuilder authenticationRequestBuilder = post("/api/events")
                .param("title", "My Event")
                .param("date", "2020-11-30");
        this.eventId = this.mvc.perform(authenticationRequestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();

        assertThat(this.eventId).containsOnlyDigits();

        final Object cacheValue = this.testStats.getCoherenceEventCache().values().iterator().next();
        assertThat(cacheValue).isInstanceOf(CoherenceRegionValue.class);
        assertThat(((CoherenceRegionValue) cacheValue).getValue()).isInstanceOf(StandardCacheEntryImpl.class);

        final NamedCache<Object, CoherenceRegionValue> namedCache = CacheFactory.getCache(CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Event");
        assertThat(namedCache).hasSize(1);

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isZero();
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isZero();
        assertThat(this.testStats.getCacheMissCount()).isZero();
        assertThat(this.testStats.getCachePutCount()).isEqualTo(1);

        assertThat(this.testStats.getQueryCacheHitCount()).isZero();
        assertThat(this.testStats.getQueryCacheMissCount()).isZero();
        assertThat(this.testStats.getQueryCachePutCount()).isZero();
    }

    /**
     * In this test, we retrieve a single {@link Event}. This should lead to the following cache expectations:
     *
     * <ul>
     *     <li>We should see a SecondLevelCacheHitCount of 1</li>
     *     <li>all other numbers stay the same</li>
     * </ul>
     *
     */
    @Test
    @Order(3)
    void getEvent() throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = get("/api/events/{eventId}", this.eventId);
        final String eventResponse = this.mvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).isEmpty();

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isZero();
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        final NamedCache<Object, CoherenceRegionValue> namedCache = CacheFactory.getCache(CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Event");
        assertThat(namedCache).hasSize(1);

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getCacheMissCount()).isZero();
        assertThat(this.testStats.getCachePutCount()).isEqualTo(1);

        assertThat(this.testStats.getQueryCacheHitCount()).isZero();
        assertThat(this.testStats.getQueryCacheMissCount()).isZero();
        assertThat(this.testStats.getQueryCachePutCount()).isZero();
    }

    /**
     * A slight variation, in this test, we retrieve a single {@link Event} but we also trigger a fetch on the
     * lazy property {@link Event#getParticipants()}, which is currently still empty. This should lead to the following
     * cache expectations:
     *
     * <ul>
     *     <li>We should see a SecondLevelCacheHitCount in creased from 1 to 2</li>
     *     <li>See our first SecondLevelCacheMissCount (1) when accessing the Set of participants</li>
     *     <li>The SecondLevelCachePutCount increases from 1 to 2 as the empty collection of participants will be added to
     *     the Coherence cache</li>
     * </ul>
     *
     * See also:
     * <a href="https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#caching-collection">Hibernate User Guide</a>
     * <p>
     * "The collection cache is not write-through". "Collections are read-through, meaning they are cached upon being
     * accessed for the first time".
     * <p>
     * Therefore, the empty collection of participants was not added to the cache when the {@link Event} was created in
     * the previous test.
     *
     */
    @Test
    @Order(4)
    void getEventWithParticipants() throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("withParticipants", "true");
        final String eventResponse = this.mvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).isEmpty();

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(2);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(2);

        assertThat(this.testStats.getQueryCacheHitCount()).isZero();
        assertThat(this.testStats.getQueryCacheMissCount()).isZero();
        assertThat(this.testStats.getQueryCachePutCount()).isZero();
    }

    /**
     * In this test we are going to use the Query cache for the first time. Instead of triggering the lazy participant
     * collection on the {@link Event} (see previous test), we will use a JPA query that employs a fetch join on the
     * Event participants. As a result, we will see the following cache behavior:
     *
     * <ul>
     *     <li>We should see the QueryCacheMissCount increase from 0 to 1</li>
     *     <li>We should see the QueryCachePutCount increase from 0 to 1</li>
     *     <li>The Event and the Set of participants will be updated, thus the
     *     SecondLevelCachePutCount will increase from 2 to 4</li>
     * </ul>
     *
     * See also:
     * <a href="https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html#caching-query">Hibernate_User_Guide</a>
     * <p>
     * "For entity queries, the query cache does not cache the state of the actual entities. Instead, it stores the
     * entity identifiers".
     *
     */
    @Test
    @Order(5)
    void getEventWithParticipantsAndQueryCache() throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("usingJpaQuery", "true");
        final String eventResponse = this.mvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).isEmpty();

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(2);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(4);

        assertThat(this.testStats.getQueryCacheHitCount()).isZero();
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    @Test
    @Order(6)
    void getEventWithParticipantsAndQueryCacheSecondTime() throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("usingJpaQuery", "true");
        final String eventResponse = this.mvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).isEmpty();

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isZero();
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(2);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(5); // Not sure about this one, why is
        // Why is Event#participants added to the cache again? No SQL query is executed though

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    /**
     * Now we are going to 1 {@link Person}.
     */
    @Test
    @Order(7)
    void createFirstPerson() throws Exception {
        this.person1Id = this.mvc.perform(
                        post("/api/people")
                                .param("firstName", "Conrad")
                                .param("lastName", "Zuse")
                                .param("age", "85"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(2);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(6);

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    /**
     * We are going to create a second {@link Person}.
     */
    @Test
    @Order(8)
    void createSecondPerson() throws Exception {
        this.person2Id = this.mvc.perform(
                        post("/api/people")
                                .param("firstName", "Alan")
                                .param("lastName", "Turing")
                                .param("age", "41"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(2);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        final NamedCache<Object, CoherenceRegionValue> namedCache = CacheFactory.getCache(CACHE_PREFIX + "com.oracle.coherence.hibernate.demo.model.Event");
        assertThat(namedCache).hasSize(1);

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(2);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(7);

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    @Test
    @Order(9)
    void getTwoPeople() throws Exception {

        final MockHttpServletRequestBuilder requestBuilder = get("/api/people/{personId}", this.person1Id);
        final String eventResponse = this.mvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final PersonDto person1 = this.objectMapper.readValue(eventResponse, PersonDto.class);

        assertThat(person1.getId()).isEqualTo(1L);
        assertThat(person1.getAge()).isEqualTo(85);
        assertThat(person1.getFirstname()).isEqualTo("Conrad");
        assertThat(person1.getLastname()).isEqualTo("Zuse");
        assertThat(person1.getEvents()).isEmpty();

        final MockHttpServletRequestBuilder requestBuilder2 = get("/api/people/{personId}", this.person2Id);
        final String eventResponse2 = this.mvc.perform(requestBuilder2)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final PersonDto person2 = this.objectMapper.readValue(eventResponse2, PersonDto.class);

        assertThat(person2.getId()).isEqualTo(2L);
        assertThat(person2.getAge()).isEqualTo(41);
        assertThat(person2.getFirstname()).isEqualTo("Alan");
        assertThat(person2.getLastname()).isEqualTo("Turing");
        assertThat(person2.getEvents()).isEmpty();

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(2);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(4);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(7);

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    @Test
    @Order(10)
    void addPeopleToEvent() throws Exception {
        this.mvc.perform(
                        post("/api/people/{personId}/add-to-event/{eventId}", this.person1Id, this.eventId))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(2);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isZero();
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(7); // Person, Event, Event#participants
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(2); //Person#events
        assertThat(this.testStats.getCachePutCount()).isEqualTo(8); //Person#events

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);

        this.mvc.perform(
                        post("/api/people/{personId}/add-to-event/{eventId}", this.person2Id, this.eventId))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(2);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isZero();
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(9);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(4);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(11);

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    @Test
    @Order(11)
    void getEventWithPeople() throws Exception {
        final MockHttpServletRequestBuilder authenticationRequestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("withParticipants", "true");
        final String eventResponse = this.mvc.perform(authenticationRequestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).hasSize(2);

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(2);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(10);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(5);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(14);

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    @Test
    @Order(12)
    void getEventWithPeopleAgain() throws Exception {
        final MockHttpServletRequestBuilder authenticationRequestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("withParticipants", "true");
        final String eventResponse = this.mvc.perform(authenticationRequestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).hasSize(2);

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        assertThat(this.testStats.getCoherenceEventCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonCacheSize()).isEqualTo(2);
        assertThat(this.testStats.getCoherenceDefaultQueryResultsRegionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherenceEventParticipantsCollectionCacheSize()).isEqualTo(1);
        assertThat(this.testStats.getCoherencePersonEventsCollectionCacheSize()).isZero();

        assertThat(this.testStats.getCacheHitCount()).isEqualTo(14);
        assertThat(this.testStats.getCacheMissCount()).isEqualTo(5);
        assertThat(this.testStats.getCachePutCount()).isEqualTo(14);

        assertThat(this.testStats.getQueryCacheHitCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCacheMissCount()).isEqualTo(1);
        assertThat(this.testStats.getQueryCachePutCount()).isEqualTo(1);
    }

    private final class TestStats {

        private final EntityManager em;

        private TestStats(EntityManager em) {
            this.em = em;
        }

        private long getCacheHitCount() {
            return getHibernateStatistics().getSecondLevelCacheHitCount();
        }

        private long getCacheMissCount() {
            return getHibernateStatistics().getSecondLevelCacheMissCount();
        }

        private long getCachePutCount() {
            return getHibernateStatistics().getSecondLevelCachePutCount();
        }

        private long getQueryCacheHitCount() {
            return getHibernateStatistics().getQueryCacheHitCount();
        }

        private long getQueryCacheMissCount() {
            return getHibernateStatistics().getQueryCacheMissCount();
        }

        private long getQueryCachePutCount() {
            return getHibernateStatistics().getQueryCachePutCount();
        }

        private Statistics getHibernateStatistics() {
            return this.em.unwrap(Session.class).getSessionFactory().getStatistics();
        }

        private long getCoherenceEventCacheSize() {
            return getCoherenceEventCache().size();
        }

        private NamedCache<?, ?> getCoherenceEventCache() {
            return CacheFactory.getCache(EVENT_ENTITY_CACHE_NAME);
        }

        private long getCoherencePersonCacheSize() {
            return CacheFactory.getCache(PERSON_ENTITY_CACHE_NAME).size();
        }

        private long getCoherencePersonEventsCollectionCacheSize() {
            return CacheFactory.getCache(PERSON_EVENTS_COLLECTION_CACHE_NAME).size();
        }

        private long getCoherenceEventParticipantsCollectionCacheSize() {
            return CacheFactory.getCache(EVENT_PARTICIPANTS_COLLECTION_CACHE_NAME).size();
        }

        private long getCoherenceDefaultQueryResultsRegionCacheSize() {
            return CacheFactory.getCache(DEFAULT_QUERY_RESULTS_REGION).size();
        }

        private long getCacheRegionNamesSize() {
            return getHibernateStatistics().getSecondLevelCacheRegionNames().length;
        }
    }
}
