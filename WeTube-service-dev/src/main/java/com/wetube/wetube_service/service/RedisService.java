// package com.wetube.wetube_service.service;

// import java.time.Duration;
// import java.util.Set;

// public interface RedisService {
//    void save(String key, Object value, Duration expiration);
//    void save(String key, Object value);
//    <T> T get(String key, Class<T> type);
//    boolean exists(String key);
//    void delete(String key);
//    void expire(String key, Duration expiration);
//    Set<String> getKeys(String pattern);
//    Long increment(String key);
//    Long decrement(String key);
//    void hashSet(String key, String hashKey, Object value);
//    <T> T hashGet(String key, String hashKey, Class<T> type);
//    void hashDelete(String key, String hashKey);
// }
