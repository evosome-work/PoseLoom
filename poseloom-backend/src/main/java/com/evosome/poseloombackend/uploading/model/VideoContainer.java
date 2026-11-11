package com.evosome.poseloombackend.uploading.model;

/**
 * Контейнер видеоролика — распознанная без ffprobe часть доменного VO {@code VideoFormat}
 * (контейнер + кодек).
 *
 * <p>В эпике 001 {@code VideoFormat} разложен на два скаляра: {@code container}
 * ({@link Video#getContainer()}, CHECK {@code chk_videos_container}: MP4 | MOV) и
 * {@code codec} ({@link Video#getCodec()}, nullable до ffprobe). Кодек определяется
 * только в эпике 002, поэтому здесь — только контейнер.
 *
 * <p>Значения соответствуют {@code ftyp}-brand'ам, которые принимает проверка целостности
 * без ffprobe (diploma_v2 :: Upload Flow «Проверка формата без ffprobe»): mp4-семейство
 * ({@code mp41}, {@code mp42}, {@code avc1}, {@code iso5}, {@code dash}, {@code M4V }) —
 * {@link #MP4}, quicktime-бренд {@code qt  } — {@link #MOV}.
 */
public enum VideoContainer {

    /** ISO Base Media File Format с mp4-совместимым major brand. */
    MP4,

    /** QuickTime-контейнер (brand {@code qt  }). */
    MOV
}
