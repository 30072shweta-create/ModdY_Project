package com.example.flight.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    public static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss")
        };

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();

            // Date-only string (e.g. "2026-09-04" or "2026/09/04")
            if (text.length() == 10) {
                try {
                    if (text.contains("/")) {
                        return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy/MM/dd")).atStartOfDay();
                    }
                    return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                } catch (Exception ignored) {
                }
            }

            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDateTime.parse(text, formatter);
                } catch (Exception ignored) {
                }
            }

            try {
                return Instant.parse(text).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception ignored) {
            }

            try {
                return LocalDate.parse(text).atStartOfDay();
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse LocalDateTime from: " + text, e);
            }
        }
    }

    public static class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();
            if (text.contains("T")) {
                text = text.split("T")[0];
            } else if (text.contains(" ")) {
                text = text.split(" ")[0];
            }
            return LocalDate.parse(text.replace("/", "-"));
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
            javaTimeModule.addDeserializer(LocalDate.class, new FlexibleLocalDateDeserializer());

            builder.modules(javaTimeModule);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.featuresToEnable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
            builder.featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        };
    }
}
