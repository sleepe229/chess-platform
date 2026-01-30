package com.chess.matchmaking.repo;

import com.chess.matchmaking.dto.MatchmakingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisMatchmakingRequestStore implements MatchmakingRequestStore {

    private static final String REQ_KEY_PREFIX = "mm:req:";
    private static final String USER_KEY_PREFIX = "mm:user:";
    private static final String IDEMP_JOIN_PREFIX = "mm:idemp:join:";
    private static final String IDEMP_LEAVE_PREFIX = "mm:idemp:leave:";

    private static final Duration IDEMP_TTL = Duration.ofHours(24);
    private static final Duration REQUEST_TTL = Duration.ofHours(3);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String createOrGetActiveRequest(UUID userId, int baseSeconds, int incrementSeconds, boolean rated, String idempotencyKey, String requestIdHeader) {
        String userKey = USER_KEY_PREFIX + userId;

        // Idempotency: if same key used, return same requestId
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = IDEMP_JOIN_PREFIX + userId + ":" + idempotencyKey;
            String existing = redisTemplate.opsForValue().get(idemKey);
            if (existing != null && !existing.isBlank()) {
                return existing;
            }
        }

        // Single active request per user: if exists, return it
        String existingReqId = redisTemplate.opsForValue().get(userKey);
        if (existingReqId != null && !existingReqId.isBlank()) {
            return existingReqId;
        }

        String requestId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        String reqKey = REQ_KEY_PREFIX + requestId;
        Map<String, String> data = Map.of(
                "requestId", requestId,
                "userId", userId.toString(),
                "status", MatchmakingStatus.QUEUED.name(),
                "baseSeconds", String.valueOf(baseSeconds),
                "incrementSeconds", String.valueOf(incrementSeconds),
                "rated", String.valueOf(rated),
                "queuedAtMs", String.valueOf(now),
                "xRequestId", requestIdHeader != null ? requestIdHeader : ""
        );
        redisTemplate.opsForHash().putAll(reqKey, data);
        redisTemplate.expire(reqKey, REQUEST_TTL);

        redisTemplate.opsForValue().set(userKey, requestId, REQUEST_TTL);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = IDEMP_JOIN_PREFIX + userId + ":" + idempotencyKey;
            redisTemplate.opsForValue().set(idemKey, requestId, IDEMP_TTL);
        }

        return requestId;
    }

    @Override
    public void enrichQueuedRequest(String requestId, String timeControlType, double rating, double ratingDeviation) {
        String reqKey = REQ_KEY_PREFIX + requestId;
        redisTemplate.opsForHash().put(reqKey, "timeControlType", timeControlType);
        redisTemplate.opsForHash().put(reqKey, "rating", String.valueOf(rating));
        redisTemplate.opsForHash().put(reqKey, "ratingDeviation", String.valueOf(ratingDeviation));
    }

    @Override
    public void markMatched(String requestId, String gameId) {
        String reqKey = REQ_KEY_PREFIX + requestId;
        redisTemplate.opsForHash().put(reqKey, "status", MatchmakingStatus.MATCHED.name());
        redisTemplate.opsForHash().put(reqKey, "gameId", gameId);
        redisTemplate.expire(reqKey, Duration.ofHours(6));
    }

    @Override
    public void markExpired(String requestId) {
        String reqKey = REQ_KEY_PREFIX + requestId;
        Object statusObj = redisTemplate.opsForHash().get(reqKey, "status");
        String status = statusObj != null ? statusObj.toString() : null;
        if (MatchmakingStatus.MATCHED.name().equals(status) || MatchmakingStatus.CANCELLED.name().equals(status)) {
            return;
        }

        redisTemplate.opsForHash().put(reqKey, "status", MatchmakingStatus.EXPIRED.name());
        redisTemplate.expire(reqKey, Duration.ofMinutes(30));

        Object userIdObj = redisTemplate.opsForHash().get(reqKey, "userId");
        if (userIdObj != null) {
            String userKey = USER_KEY_PREFIX + userIdObj;
            String activeReq = redisTemplate.opsForValue().get(userKey);
            if (requestId.equals(activeReq)) {
                redisTemplate.delete(userKey);
            }
        }
    }

    @Override
    public StoredRequest getRequest(String requestId) {
        String reqKey = REQ_KEY_PREFIX + requestId;
        Map<Object, Object> map = redisTemplate.opsForHash().entries(reqKey);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new StoredRequest(
                requestId,
                (String) map.get("userId"),
                (String) map.get("status"),
                (String) map.get("gameId"),
                (String) map.get("timeControlType"),
                (String) map.get("baseSeconds"),
                (String) map.get("incrementSeconds"),
                (String) map.get("rated"),
                (String) map.get("rating"),
                (String) map.get("ratingDeviation"),
                (String) map.get("queuedAtMs")
        );
    }

    @Override
    public void cancelRequest(UUID userId, String requestId, String idempotencyKey, String requestIdHeader) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idemKey = IDEMP_LEAVE_PREFIX + userId + ":" + idempotencyKey;
            String already = redisTemplate.opsForValue().get(idemKey);
            if (already != null) {
                return;
            }
            redisTemplate.opsForValue().set(idemKey, "1", IDEMP_TTL);
        }

        String reqKey = REQ_KEY_PREFIX + requestId;
        redisTemplate.opsForHash().put(reqKey, "status", MatchmakingStatus.CANCELLED.name());
        redisTemplate.expire(reqKey, Duration.ofMinutes(30));

        String userKey = USER_KEY_PREFIX + userId;
        String activeReq = redisTemplate.opsForValue().get(userKey);
        if (requestId.equals(activeReq)) {
            redisTemplate.delete(userKey);
        }
    }
}

