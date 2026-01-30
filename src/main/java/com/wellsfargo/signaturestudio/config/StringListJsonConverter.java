package com.wellsfargo.signaturestudio.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * AttributeConverter for converting List<String> to JSON string for database storage.
 *
 * This converter is used for storing field selections and other string lists
 * in CLOB columns as JSON arrays.
 *
 * Conversion Flow:
 * 1. Save: List<String> → JSON array string → Oracle CLOB
 * 2. Load: Oracle CLOB → JSON array string → List<String>
 *
 * USAGE: Apply manually to entity fields using @Convert annotation:
 * @Convert(converter = StringListJsonConverter.class)
 * @Column(columnDefinition = "CLOB")
 * private List<String> selectedFields;
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final Logger logger = LoggerFactory.getLogger(StringListJsonConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

    /**
     * Convert List<String> to JSON string for database storage.
     *
     * @param list The List<String> value from entity field
     * @return JSON array string for database CLOB column
     */
    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("Error converting List<String> to JSON", e);
            return "[]";
        }
    }

    /**
     * Convert JSON string from database to List<String> for entity field.
     *
     * @param json The JSON array string from database CLOB column
     * @return List<String> for entity field
     */
    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to List<String>: {}", json, e);
            return new ArrayList<>();
        }
    }
}
