package com.evosome.poseloombackend.uploading.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Мост между null-object {@link FailureCode#NONE} в Java и {@code NULL} в колонке
 * {@code upload_sessions.failure_code}.
 */
@Converter(autoApply = false)
public class FailureCodeConverter implements AttributeConverter<FailureCode, String> {

    @Override
    public String convertToDatabaseColumn(FailureCode failureCode) {
        if (failureCode == null || failureCode == FailureCode.NONE) {
            return null;
        }
        return failureCode.name();
    }

    @Override
    public FailureCode convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return FailureCode.NONE;
        }
        try {
            return FailureCode.valueOf(databaseValue.trim());
        } catch (IllegalArgumentException unknownCode) {
            throw new IllegalStateException(
                    "Unknown failure_code in database: '" + databaseValue + "'", unknownCode);
        }
    }
}
