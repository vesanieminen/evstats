package com.vesanieminen.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vesanieminen.views.charging.ChargingView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Slf4j
public class ObjectMapperService {

    private final ObjectMapper objectMapper;

    public ObjectMapperService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <C extends AbstractField<C, T>, T> void saveFieldValue(AbstractField<C, T> field) {
        try {
            WebStorage.setItem(field.getId().orElseThrow(), objectMapper.writeValueAsString(field.getValue()));
        } catch (JsonProcessingException e) {
            log.info("Could not save value: %s".formatted(e.toString()));
        }
    }

    public <C extends HasValue.ValueChangeEvent<T>, T> void readValue(String key, HasValue<C, T> hasValue) {
        if (key == null) {
            return;
        }
        try {
            T value = objectMapper.readValue(key, new TypeReference<>() {
            });
            hasValue.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }


    public void readLocalDateTime(String key, DateTimePicker dateTimePicker) {
        if (key == null) {
            return;
        }
        try {
            var value = objectMapper.readValue(key, new TypeReference<LocalDateTime>() {
            });
            dateTimePicker.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }

    public void readCalculationTarget(String key, Select<ChargingView.CalculationTarget> select) {
        if (key == null) {
            return;
        }
        try {
            var value = objectMapper.readValue(key, new TypeReference<ChargingView.CalculationTarget>() {
            });
            select.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }

}
