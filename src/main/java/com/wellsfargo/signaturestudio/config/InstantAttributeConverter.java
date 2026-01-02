package com.wellsfargo.signaturestudio.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Explicit AttributeConverter for Instant to Oracle TIMESTAMP via LocalDateTime.
 *
 * This converter uses LocalDateTime as an intermediary to avoid Oracle JDBC driver
 * issues with java.sql.Timestamp parameter binding.
 *
 * IMPORTANT: UTC Consistency
 * - Oracle TIMESTAMP column is timezone-naive (no timezone info stored)
 * - We use ZoneOffset.UTC to convert between Instant (UTC) and LocalDateTime
 * - All Instant values are treated as UTC
 * - hibernate.jdbc.time_zone=UTC ensures consistent timezone handling
 *
 * Conversion Flow:
 * 1. Save: Instant (UTC) → LocalDateTime (interpreted as UTC) → Oracle TIMESTAMP
 * 2. Load: Oracle TIMESTAMP → LocalDateTime (interpreted as UTC) → Instant (UTC)
 *
 * USAGE: Apply manually to entity fields using @Convert annotation:
 * @Convert(converter = InstantAttributeConverter.class)
 * private Instant nextExpirTmstp;
 *
 * This ensures the converter is only used for database entity fields, not DTOs.
 */
@Converter  // No autoApply - must be explicitly applied to fields
public class InstantAttributeConverter implements AttributeConverter<Instant, LocalDateTime> {

    /**
     * Convert Instant to LocalDateTime for database storage.
     * Uses UTC zone offset to maintain timezone consistency.
     *
     * @param instant The Instant value from entity field (always UTC)
     * @return LocalDateTime for Oracle TIMESTAMP column (interpreted as UTC)
     */
    @Override
    public LocalDateTime convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * Convert LocalDateTime from database to Instant for entity field.
     * Uses UTC zone offset to maintain timezone consistency.
     *
     * @param localDateTime The LocalDateTime value from Oracle TIMESTAMP column
     * @return Instant for entity field (UTC)
     */
    @Override
    public Instant convertToEntityAttribute(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }
}
