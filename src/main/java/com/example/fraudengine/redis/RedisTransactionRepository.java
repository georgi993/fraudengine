package com.example.fraudengine.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisTransactionRepository {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveFraud(String key, Set<String> values) {
        redisTemplate.opsForSet().add(key, values.toArray(new String[0]));
    }

    public void saveBlacklistCountry(String key, Set<String> values) {
        redisTemplate.opsForSet().add(key, values.toArray(new String[0]));
    }

    public boolean isMember(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

}
