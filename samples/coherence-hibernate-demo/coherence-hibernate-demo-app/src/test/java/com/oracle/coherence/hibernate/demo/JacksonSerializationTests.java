package com.oracle.coherence.hibernate.demo;

import java.time.LocalDate;

import com.oracle.coherence.hibernate.demo.controller.dto.EventDto;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonSerializationTests {
	private final String test = "{\n" +
			"  \"id\" : 1,\n" +
			"  \"title\" : \"My Event\",\n" +
			"  \"date\" : \"2020-11-30\",\n" +
			"  \"participants\" : [ 3, 2 ]\n" +
			"}";

	@Test
	void deserializationTest() throws JacksonException {
		final ObjectMapper objectMapper = new ObjectMapper();
		final EventDto event = objectMapper.readValue(this.test, EventDto.class);
		assertThat(event.getId()).isEqualTo(1L);
		assertThat(event.getTitle()).isEqualTo("My Event");
		assertThat(event.getDate()).isEqualTo(LocalDate.of(2020, 11, 30));
		assertThat(event.getParticipants().size()).isEqualTo(2);
	}
}
