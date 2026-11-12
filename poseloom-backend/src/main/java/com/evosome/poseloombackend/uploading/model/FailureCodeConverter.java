package com.evosome.poseloombackend.uploading.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Мост между null-object {@link FailureCode#NONE} в Java и {@code NULL} в колонке
 * {@code upload_sessions.failure_code}.
 *
 * <p>Основание — diploma_v2 :: ER Deviations: в схеме сентинел-константа
 * {@code 'NO_FAILURE'} отсутствует, отсутствие отказа = SQL {@code NULL}, а закрытый
 * список кодов защищает вторым CHECK. Конвертер сохраняет оба инварианта:
 * <ul>
 *   <li>{@code NONE} (и защитный {@code null}) в сущности → {@code NULL} в БД;</li>
 *   <li>{@code NULL} из БД → {@code NONE} в сущности, поэтому
 *       {@code UploadSession#getFailureCode()} никогда не вернёт {@code null}.</li>
 * </ul>
 *
 * <p>{@code autoApply = false}: конвертер подключается явно аннотацией
 * {@code @Convert} только на {@link UploadSession#FAILURE_CODE_PROPERTY}, чтобы не
 * перехватить другие enum-поля ({@code status}, {@code container} идут через
 * {@link jakarta.persistence.EnumType#STRING}).
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
