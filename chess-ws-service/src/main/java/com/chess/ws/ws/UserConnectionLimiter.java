package com.chess.ws.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UserConnectionLimiter {

    private final ConcurrentHashMap<UUID, AtomicInteger> counts = new ConcurrentHashMap<>();

    @Value("${ws.max-connections-per-user:5}")
    private int maxConnectionsPerUser;

    public boolean tryAcquire(UUID userId) {
        AtomicInteger ai = counts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        while (true) {
            int current = ai.get();
            if (current >= maxConnectionsPerUser) {
                return false;
            }
            if (ai.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    public void release(UUID userId) {
        AtomicInteger ai = counts.get(userId);
        if (ai == null) {
            return;
        }
        int next = ai.decrementAndGet();
        if (next <= 0) {
            counts.remove(userId);
        }
    }
}

