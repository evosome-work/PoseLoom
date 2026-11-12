package com.evosome.poseloombackend.uploading.model;

/**
 * Контейнер видеоролика — распознанная часть доменного VO {@code VideoFormat}
 * (контейнер + кодек).
 *
 * <p>Значения соответствуют {@code ftyp}-brand'ам: mp4-семейство
 * ({@code mp41}, {@code mp42}, {@code avc1}, {@code iso5}, {@code dash}, {@code M4V }) —
 * {@link #MP4}, quicktime-бренд {@code qt  } — {@link #MOV}.
 */
public enum VideoContainer {

    /** ISO Base Media File Format с mp4-совместимым major brand. */
    MP4,

    /** QuickTime-контейнер (brand {@code qt}). */
    MOV
}
