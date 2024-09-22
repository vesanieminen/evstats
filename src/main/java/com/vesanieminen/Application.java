package com.vesanieminen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "evstats")
@JsModule("src/prefers-color-scheme.js")
@PWA(name = "Auto Liukuri", shortName = "Auto Liukuri", offlineResources = {})
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO format
        //// Configure a PolymorphicTypeValidator to allow specific packages or classes
        //PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        //        .allowIfSubType("com.vesanieminen") // Replace with your package name
        //        .allowIfSubType("java.time")
        //        .build();

        //// Activate default typing with the validator
        //mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

}
