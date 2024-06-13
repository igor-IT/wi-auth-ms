package com.microservice.auth.service;

import com.microservice.auth.data.UserBucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class LimiterService {
    private Map<String, UserBucket> buckets = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);


    public LimiterService() {
        executorService.scheduleAtFixedRate(this::cleanBuckets, 1, 1, TimeUnit.HOURS);
    }

    public void initUser(String userId) {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        this.buckets.put(userId, new UserBucket(Bucket.builder().addLimit(limit).build()));
    }

    public boolean tryConsume(String userId) {
        UserBucket userBucket = buckets.get(userId);
        if (null != userBucket){
            return userBucket.getBucket().tryConsume(1);
        }
        initUser(userId);
        return buckets.get(userId).getBucket().tryConsume(1);
    }

    private void cleanBuckets() {
        Iterator<Map.Entry<String, UserBucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, UserBucket> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue().getLastAccessTime() > 60 * 60 * 1000) {
                iterator.remove();
            }
        }
    }
}
