package com.example.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public static class CachedResponse {
        private String requestHash;
        private int statusCode;
        private Object responseBody;

        public String getRequestHash() {
            return requestHash;
        }

        public void setRequestHash(String requestHash) {
            this.requestHash = requestHash;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public Object getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(Object responseBody) {
            this.responseBody = responseBody;
        }
    }

    public String hashBody(Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing request body", e);
        }
    }

    public CachedResponse getCachedResponse(String key, String endpoint) {
        String redisKey = "idempotency:" + endpoint + ":" + key;
        String val = redisTemplate.opsForValue().get(redisKey);
        if (val == null) {
            return null;
        }
        try {
            return objectMapper.readValue(val, CachedResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveResponse(String key, String endpoint, String requestHash, int statusCode, Object responseBody) {
        String redisKey = "idempotency:" + endpoint + ":" + key;
        CachedResponse cached = new CachedResponse();
        cached.setRequestHash(requestHash);
        cached.setStatusCode(statusCode);
        cached.setResponseBody(responseBody);

        try {
            String val = objectMapper.writeValueAsString(cached);
            redisTemplate.opsForValue().set(redisKey, val, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            // Log error
        }
    }
}
