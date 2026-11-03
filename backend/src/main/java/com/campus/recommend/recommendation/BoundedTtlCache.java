package com.campus.recommend.recommendation;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Small process-local LRU cache. Loads are serialized to prevent duplicate snapshot builds. */
public final class BoundedTtlCache<K, V> {
    private record Entry<V>(V value, long loadedAt) { }

    private final int capacity;
    private final long ttlNanos;
    private final LongSupplier clock;
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);

    public BoundedTtlCache(int capacity, Duration ttl) {
        this(capacity, ttl, System::nanoTime);
    }

    public BoundedTtlCache(int capacity, Duration ttl, LongSupplier clock) {
        if (capacity < 1 || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("positive capacity and TTL required");
        this.capacity = capacity;
        this.ttlNanos = ttl.toNanos();
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized V get(K key, Supplier<V> loader) {
        Entry<V> cached = entries.get(key);
        if (cached != null && clock.getAsLong() - cached.loadedAt() < ttlNanos) return cached.value();
        V value = Objects.requireNonNull(loader.get(), "cache loader returned null");
        entries.put(key, new Entry<>(value, clock.getAsLong()));
        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
        return value;
    }

    public synchronized void clear() {
        entries.clear();
    }
}
