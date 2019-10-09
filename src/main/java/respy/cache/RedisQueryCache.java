package respy.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static respy.cache.CRCUtils.calculateHashSlot;

/**
 * Simple "cache" to play around Redis' invalidation feature.
 * This can be used by multiple threads.
 * There is no limits for memory usage.
 */
public class RedisQueryCache {
    private volatile ConcurrentMap<Long, ConcurrentMap<String, Object>> state = new ConcurrentHashMap<>();

    /**
     * Put value into cache.
     *
     * @param key
     * @param value
     */
    public void put(String key, Object value) {
        long slot = calculateHashSlot(key.getBytes());
        ConcurrentMap<String, Object> slotCache = state.computeIfAbsent(slot, (k) -> new ConcurrentHashMap<>());
        slotCache.put(key, value);
    }

    /**
     * Get potentially cached value of {@param key}.
     *
     * @param key
     * @return value stored under {@param key} or null if there is not data.ś
     */
    public Object get(String key) {
        long slot = calculateHashSlot(key.getBytes());
        return state.computeIfAbsent(slot, (k) -> new ConcurrentHashMap<>()).get(key);
    }

    /**
     * Invalidate slot - used when Redis PUSH information about slot being invalidated.
     *
     * @param slot
     */
    public void invalidate(long slot) {
        state.remove(slot);
    }

    /**
     * Invalidate entire cache - use if any of RedisClient using that case lose connection to Redis.
     */
    public void invalidate() {
        state = new ConcurrentHashMap<>();
    }

}
