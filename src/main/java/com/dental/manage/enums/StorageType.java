package com.dental.manage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported storage provider types.
 */
public enum StorageType {
    LOCAL("local"),
    S3("s3");

    private final String value;

    StorageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static StorageType fromString(String text) {
        if (text == null || text.isBlank()) {
            return LOCAL;
        }
        String normalized = text.trim().toLowerCase();
        if ("s3".equals(normalized) || "amazons3".equals(normalized) || "amazon_s3".equals(normalized) || "aws".equals(normalized)) {
            return S3;
        }
        if ("local".equals(normalized) || "filesystem".equals(normalized) || "file_system".equals(normalized) || "file".equals(normalized)) {
            return LOCAL;
        }
        throw new IllegalArgumentException("Unknown storage provider: " + text + ". Supported values are 'local' or 's3'.");
    }
}
