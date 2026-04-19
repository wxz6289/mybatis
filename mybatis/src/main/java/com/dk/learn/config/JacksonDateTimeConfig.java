package com.dk.learn.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring Boot 4 默认 Jackson 3（{@code tools.jackson.*}），不再使用
 * {@code com.fasterxml.jackson.datatype.jsr310}。
 * 通过 {@link JacksonComponent} 注册 {@link LocalDateTime} 的全局格式，与 {@code application.yaml} 中
 * {@code spring.jackson.*} 配置一并生效。
 */
@SuppressWarnings("java:S1118")
@JacksonComponent
public class JacksonDateTimeConfig {

	public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

	public static class LocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

		@Override
		public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext serializers) {
			if (value == null) {
				gen.writeNull();
				return;
			}
			gen.writeString(value.format(FORMATTER));
		}
	}

	public static class LocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

		@Override
		public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) {
			String text = parser.getString();
			if (text == null || text.isBlank()) {
				return null;
			}
			return LocalDateTime.parse(text, FORMATTER);
		}
	}
}
