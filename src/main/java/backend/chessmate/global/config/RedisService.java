package backend.chessmate.global.config;



import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key , String refreshToken, long expirationSeconds) {
        redisTemplate.opsForValue().set(key, refreshToken, expirationSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        var value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    public void deleteRefreshToken(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasRefreshToken(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

}
