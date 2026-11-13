package com.evosome.poseloombackend.uploading.storage;

/**
 * Transient-ошибка объектного хранилища (спека StorageError.yml): операция
 * повторяема, сессия не разрушается, клиент получает 502 {@code STORAGE_ERROR}.
 * Внутренние детали SDK в сообщение не кладутся.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
