package com.dongnemarket.global.storage;

import java.time.Instant;

public record StoredObject(String filename, long sizeBytes, Instant lastModified) {
}
