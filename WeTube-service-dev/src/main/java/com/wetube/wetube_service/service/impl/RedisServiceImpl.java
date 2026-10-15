//package com.wetube.wetube_service.service.impl;
//
//import static com.wetube.wetube_service.constants.LogConstants.*;
//
//import com.wetube.wetube_service.service.RedisService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.Set;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RedisServiceImpl implements RedisService {
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    @Override
//    public void save(String key, Object value, Duration expiration) {
//        try {
//            redisTemplate.opsForValue().set(key, value, expiration);
//            log.debug(SAVE_SUCCESS_LOG, key);
//        } catch (Exception e) {
//            log.error(SAVE_ERROR_LOG, key, e);
//        }
//    }
//
//    @Override
//    public void save(String key, Object value) {
//        try {
//            redisTemplate.opsForValue().set(key, value);
//            log.debug(SAVE_SUCCESS_LOG, key);
//        } catch (Exception e) {
//            log.error(SAVE_ERROR_LOG, key, e);
//        }
//    }
//
//    @Override
//    public <T> T get(String key, Class<T> type) {
//        try {
//            Object value = redisTemplate.opsForValue().get(key);
//            if (value != null && type.isAssignableFrom(value.getClass())) {
//                return type.cast(value);
//            }
//            return null;
//        } catch (Exception e) {
//            log.error(GET_ERROR_LOG, key, e);
//            return null;
//        }
//    }
//
//    @Override
//    public boolean exists(String key) {
//        try {
//            return redisTemplate.hasKey(key);
//        } catch (Exception e) {
//            log.error(EXIST_ERROR_LOG, key, e);
//            return false;
//        }
//    }
//
//    @Override
//    public void delete(String key) {
//        try {
//            redisTemplate.delete(key);
//            log.debug(DELETE_SUCCESS_LOG, key);
//        } catch (Exception e) {
//            log.error(DELETE_ERROR_LOG, key, e);
//        }
//    }
//
//    @Override
//    public void expire(String key, Duration expiration) {
//        try {
//            redisTemplate.expire(key, expiration);
//            log.debug(EXPIRE_SUCCESS_LOG, key, expiration);
//        } catch (Exception e) {
//            log.error(EXPIRE_ERROR_LOG, key, e);
//        }
//    }
//
//    @Override
//    public Set<String> getKeys(String pattern) {
//        try {
//            return redisTemplate.keys(pattern);
//        } catch (Exception e) {
//            log.error(GET_KEYS_ERROR_LOG, pattern, e);
//            return Set.of();
//        }
//    }
//
//    @Override
//    public Long increment(String key) {
//        try {
//            return redisTemplate.opsForValue().increment(key);
//        } catch (Exception e) {
//            log.error(INCREMENT_ERROR_LOG, key, e);
//            return null;
//        }
//    }
//
//    @Override
//    public Long decrement(String key) {
//        try {
//            return redisTemplate.opsForValue().decrement(key);
//        } catch (Exception e) {
//            log.error(DECREMENT_ERROR_LOG, key, e);
//            return null;
//        }
//    }
//
//    @Override
//    public void hashSet(String key, String hashKey, Object value) {
//        try {
//            redisTemplate.opsForHash().put(key, hashKey, value);
//            log.debug(HASH_SET_SUCCESS_LOG, key, hashKey);
//        } catch (Exception e) {
//            log.error(HASH_SET_ERROR_LOG, key, hashKey, e);
//        }
//    }
//
//    @Override
//    public <T> T hashGet(String key, String hashKey, Class<T> type) {
//        try {
//            Object value = redisTemplate.opsForHash().get(key, hashKey);
//            if (value != null && type.isAssignableFrom(value.getClass())) {
//                return type.cast(value);
//            }
//            return null;
//        } catch (Exception e) {
//            log.error(HASH_GET_ERROR_LOG, key, hashKey, e);
//            return null;
//        }
//    }
//
//    @Override
//    public void hashDelete(String key, String hashKey) {
//        try {
//            redisTemplate.opsForHash().delete(key, hashKey);
//            log.debug(HASH_DELETE_SUCCESS_LOG, key, hashKey);
//        } catch (Exception e) {
//            log.error(HASH_DELETE_ERROR_LOG, key, hashKey, e);
//        }
//    }
//}
