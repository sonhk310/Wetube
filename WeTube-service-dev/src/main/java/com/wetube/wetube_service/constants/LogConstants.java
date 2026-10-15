
package com.wetube.wetube_service.constants;

public final class LogConstants {
    private LogConstants() {

    }
    public static final String SAVE_SUCCESS_LOG = "Saved to Redis - Key: {}";
    public static final String SAVE_ERROR_LOG = "Error saving to Redis - Key: {}";
    public static final String GET_ERROR_LOG = "Error getting from Redis - Key: {}";
    public static final String EXIST_ERROR_LOG = "Error checking key existence in Redis - Key: {}";
    public static final String DELETE_SUCCESS_LOG = "Deleted from Redis - Key: {}";
    public static final String DELETE_ERROR_LOG = "Error deleting from Redis - Key: {}";
    public static final String EXPIRE_SUCCESS_LOG = "Set expiration for Redis key: {} - Duration: {}";
    public static final String EXPIRE_ERROR_LOG = "Error setting expiration for Redis key: {}";
    public static final String GET_KEYS_ERROR_LOG = "Error getting keys with pattern: {}";
    public static final String INCREMENT_ERROR_LOG = "Error incrementing key: {}";
    public static final String DECREMENT_ERROR_LOG = "Error decrementing key: {}";
    public static final String HASH_SET_SUCCESS_LOG = "Hash set - Key: {}, HashKey: {}";
    public static final String HASH_SET_ERROR_LOG = "Error setting hash - Key: {}, HashKey: {}";
    public static final String HASH_GET_ERROR_LOG = "Error getting hash - Key: {}, HashKey: {}";
    public static final String HASH_DELETE_SUCCESS_LOG = "Hash deleted - Key: {}, HashKey: {}";
    public static final String HASH_DELETE_ERROR_LOG = "Error deleting hash - Key: {}, HashKey: {}";

}
