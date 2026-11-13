package com.evosome.poseloombackend.uploading.feature.complete.services;

import com.evosome.poseloombackend.uploading.exception.ApiErrorCode;
import com.evosome.poseloombackend.uploading.exception.ApiException;
import com.evosome.poseloombackend.uploading.model.VideoContainer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Проверка сигнатуры ISO BMFF без ffprobe (Tasks T3.6, спека complete.yml):
 * байты 4–7 должны быть {@code "ftyp"}, байты 8–11 — major brand из whitelist.
 * Подтверждает только контейнер, но не кодек/длительность/целостность {@code moov}
 * — это эпик 002. Объект короче 12 байт — тоже {@code UNSUPPORTED_CONTAINER}.
 * {@code "qt  "} (QuickTime, два завершающих пробела) → {@link VideoContainer#MOV},
 * остальные бренды → {@link VideoContainer#MP4}.
 */
public final class FtypInspector {

    /** Число байт заголовка ftyp: 4 (size) + 4 (type) + 4 (major brand). */
    public static final int HEADER_BYTES = 12;

    private static final String FTYP_MARKER = "ftyp";

    private static final Map<String, VideoContainer> WHITELISTED_BRANDS = Map.of(
            "mp41", VideoContainer.MP4,
            "mp42", VideoContainer.MP4,
            "isom", VideoContainer.MP4,
            "iso5", VideoContainer.MP4,
            "avc1", VideoContainer.MP4,
            "dash", VideoContainer.MP4,
            "M4V ", VideoContainer.MP4,
            "qt  ", VideoContainer.MOV);

    private static final ApiException UNSUPPORTED = new ApiException(
            ApiErrorCode.UNSUPPORTED_CONTAINER,
            "ftyp box not found or major brand not whitelisted");

    private FtypInspector() {
    }

    /**
     * @param head первые {@value #HEADER_BYTES} байт собранного объекта
     * @return распознанный контейнер
     * @throws ApiException {@code UNSUPPORTED_CONTAINER} — terminal-ошибка,
     *                      сессию переводит в {@code FAILED} вызывающий сервис
     */
    public static VideoContainer inspect(byte[] head) {
        if (head == null || head.length < HEADER_BYTES) {
            throw UNSUPPORTED;
        }
        String marker = new String(head, 4, 4, StandardCharsets.US_ASCII);
        if (!FTYP_MARKER.equals(marker)) {
            throw UNSUPPORTED;
        }
        String majorBrand = new String(head, 8, 4, StandardCharsets.US_ASCII);
        VideoContainer container = WHITELISTED_BRANDS.get(majorBrand);
        if (container == null) {
            throw UNSUPPORTED;
        }
        return container;
    }
}
