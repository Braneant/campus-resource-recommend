package com.campus.recommend.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Process-local limit; reverse proxies must enforce a shared limit in multi-instance deployments. */
public final class ResetRequestLimiter {
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Window> entries = new LinkedHashMap<>(128, 0.75f, true);
    private record Window(long start, int count) {}

    public synchronized boolean allow(String client) {
        long now = System.currentTimeMillis();
        Window old = entries.get(client);
        if (old == null || now - old.start() >= WINDOW_MS || now < old.start()) old = new Window(now, 0);
        boolean allowed = old.count() < 5;
        entries.put(client, new Window(old.start(), old.count() + (allowed ? 1 : 0)));
        while (entries.size() > 4096) entries.remove(entries.keySet().iterator().next());
        return allowed;
    }
}
