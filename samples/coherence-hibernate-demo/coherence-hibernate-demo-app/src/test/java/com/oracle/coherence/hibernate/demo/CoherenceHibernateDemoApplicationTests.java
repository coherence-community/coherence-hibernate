/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.coherence.hibernate.cache.v53.region.CoherenceRegionValue;
import com.oracle.coherence.hibernate.demo.model.Event;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import org.hibernate.Session;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.stat.Statistics;
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

import javax.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoherenceHibernateDemoApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    private String eventId;
    private String person1Id;
    private String person2Id;

    /*
     * When using @TestInstance(TestInstance.Lifecycle.PER_CLASS), you can't use @BeforeAll as that is too late for setting
     * system property "coherence.cacheconfig".
     */
    {
        System.setProperty("coherence.cacheconfig", "hibernate-second-level-cache-config.xml");
        System.setProperty("coherence.distributed.localstorage", "true");
    }

    @Test
    @Order(1)
    void contextLoads() {
    }

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
        System.out.println(this.eventId);
        assertThat(this.eventId).containsOnlyDigits();

        NamedCache<Object, CoherenceRegionValue> namedCache = CacheFactory.getCache("com.oracle.coherence.hibernate.demo.model.Event");
        assertThat(namedCache).hasSize(1);

        CoherenceRegionValue cacheValue = namedCache.values().iterator().next();

        assertThat(cacheValue.getValue()).isInstanceOf(StandardCacheEntryImpl.class);

        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(1L);
    }

    @Test
    @Order(3)
    void getEvent() throws Exception {
        final MockHttpServletRequestBuilder authenticationRequestBuilder = get("/api/events/{eventId}", this.eventId);
        String eventResponse = this.mvc.perform(authenticationRequestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).isEmpty();

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        NamedCache<Object, CoherenceRegionValue> namedCache = CacheFactory.getCache("com.oracle.coherence.hibernate.demo.model.Event");
        assertThat(namedCache).hasSize(1);

        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(1L);
    }

    @Test
    @Order(4)
    void createTwoPeople() throws Exception {
        this.person1Id = this.mvc.perform(
                        post("/api/people")
                                .param("firstName", "Conrad")
                                .param("lastName", "Zuse")
                                .param("age", "85"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        this.person2Id = this.mvc.perform(
                        post("/api/people")
                                .param("firstName", "Alan")
                                .param("lastName", "Turing")
                                .param("age", "41"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();

        NamedCache<Object, CoherenceRegionValue> namedCache = CacheFactory.getCache("com.oracle.coherence.hibernate.demo.model.Event");
        assertThat(namedCache).hasSize(1);

        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(3L);
    }

    @Test
    @Order(5)
    void addPeopleToEvent() throws Exception {
        this.mvc.perform(
                        post("/api/people/{personId}/add-to-event/{eventId}", this.person1Id, this.eventId))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
        this.mvc.perform(
                        post("/api/people/{personId}/add-to-event/{eventId}", this.person2Id, this.eventId))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));

        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(9L);
    }

    @Test
    @Order(6)
    void getEventWithPeople() throws Exception {
        final MockHttpServletRequestBuilder authenticationRequestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("withParticipants", "true");
        String eventResponse = this.mvc.perform(authenticationRequestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).hasSize(2);

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(12L);
    }

    @Test
    @Order(7)
    void getEventWithPeopleAgain() throws Exception {
        final MockHttpServletRequestBuilder authenticationRequestBuilder = get("/api/events/{eventId}", this.eventId)
                .param("withParticipants", "true");
        String eventResponse = this.mvc.perform(authenticationRequestBuilder)
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn().getResponse().getContentAsString();
        final Event event = this.objectMapper.readValue(eventResponse, Event.class);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getParticipants()).hasSize(2);

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
        assertThat(event.getTitle()).isEqualTo("My Event");

        Statistics statistics = em.unwrap(Session.class).getSessionFactory().getStatistics();
        assertThat(statistics.getSecondLevelCachePutCount()).isEqualTo(12L);
        assertThat(statistics.getQueryCacheHitCount()).isEqualTo(1L);
    }

}
