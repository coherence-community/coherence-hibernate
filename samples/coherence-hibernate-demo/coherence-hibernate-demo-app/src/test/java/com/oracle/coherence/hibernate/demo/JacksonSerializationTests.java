package com.oracle.coherence.hibernate.demo;

import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oracle.coherence.hibernate.demo.controller.dto.EventDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonSerializationTests {
	private final String test = "{\n" +
			"  \"id\" : 1,\n" +
			"  \"title\" : \"My Event\",\n" +
			"  \"date\" : \"2020-11-30\",\n" +
			"  \"participants\" : [ 3, 2 ]\n" +
			"}";

	@Test
	void deserializationTest() throws JsonProcessingException {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		final EventDto event = objectMapper.readValue(this.test, EventDto.class);
		assertThat(event.getId()).isEqualTo(1L);
		assertThat(event.getTitle()).isEqualTo("My Event");
		assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
		assertThat(event.getParticipants().size()).isEqualTo(2);
	}
}
