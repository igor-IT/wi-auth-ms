package com.microservice.auth.data;

import io.github.bucket4j.Bucket;

public class UserBucket {
    private final Bucket bucket;
    private long lastAccessTime;

    public UserBucket(Bucket bucket) {
        this.bucket = bucket;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public Bucket getBucket() {
        this.lastAccessTime = System.currentTimeMillis();
        return this.bucket;
    }

    public long getLastAccessTime() {
        return this.lastAccessTime;
    }
}
