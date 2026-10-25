package com.dental.manage.exception;

/**
 * Exception thrown when a requested file is not found in the storage backend.
 */
public class FileNotFoundStorageException extends StorageException {

    public FileNotFoundStorageException(String message) {
        super(message);
    }

    public FileNotFoundStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
