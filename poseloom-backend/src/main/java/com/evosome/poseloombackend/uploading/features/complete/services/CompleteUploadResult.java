package com.evosome.poseloombackend.uploading.features.complete.services;

import com.evosome.poseloombackend.uploading.features.complete.models.VideoDto;

/**
 * Результат complete для контроллера: та же {@link VideoDto} при повторном
 * complete уже завершённой сессии, но {@code created=false} → HTTP
 * {@code 200} вместо {@code 201} (спека complete.yml, Tasks T3.5).
 */
public record CompleteUploadResult(VideoDto video, boolean created) {
}
