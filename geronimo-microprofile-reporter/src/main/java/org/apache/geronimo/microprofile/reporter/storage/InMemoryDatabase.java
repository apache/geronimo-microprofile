/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.microprofile.reporter.storage;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// copy of metrics Histogram impl
public class InMemoryDatabase<T> {
    private static final double ALPHA = Double.parseDouble(System.getProperty("geronimo.reporter.storage.alpha", "0.015"));

    private static final int BUCKET_SIZE = Integer.getInteger("geronimo.reporter.storage.size", 12 /* one point/5s */ * 60 * 60);

    private static final long REFRESH_INTERVAL = TimeUnit.HOURS.toNanos(1);

    private final String unit;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicLong count = new AtomicLong();

    private final AtomicLong nextRefreshTime = new AtomicLong(System.nanoTime() + REFRESH_INTERVAL);

    private volatile long startTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

    private final ConcurrentSkipListMap<Double, Value<T>> bucket = new ConcurrentSkipListMap<>();

    public InMemoryDatabase(final String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public Collection<Value<T>> snapshot() {
        ensureUpToDate();
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return new ArrayList<>(bucket.values());
        } finally {
            lock.unlock();
        }
    }

    void add(final T value) {
        ensureUpToDate();

        final long now = System.currentTimeMillis();
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            final Value<T> sample = new Value<>(value, now, Math.exp(ALPHA * (TimeUnit.MILLISECONDS.toSeconds(now) - startTime)));
            final double priority = sample.weight / Math.random();

            final long size = count.incrementAndGet();
            if (size <= BUCKET_SIZE) {
                bucket.put(priority, sample);
            } else { // iterate through the bucket until we need removing low priority entries to get a new space
                double first = bucket.firstKey();
                if (first < priority && bucket.putIfAbsent(priority, sample) == null) {
                    while (bucket.remove(first) == null) {
                        first = bucket.firstKey();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void ensureUpToDate() {
        final long next = nextRefreshTime.get();
        final long now = System.nanoTime();
        if (now < next) {
            return;
        }

        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            if (nextRefreshTime.compareAndSet(next, now + REFRESH_INTERVAL)) {
                final long oldStartTime = startTime;
                startTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                final double updateFactor = Math.exp(-ALPHA * (startTime - oldStartTime));
                if (updateFactor != 0.) {
                    bucket.putAll(new ArrayList<>(bucket.keySet()).stream().collect(toMap(k -> k * updateFactor, k -> {
                        final Value<T> previous = bucket.remove(k);
                        return new Value<>(previous.value, previous.timestamp, previous.weight * updateFactor);
                    })));
                    count.set(bucket.size()); // N keys can lead to the same key so we must update it
                } else {
                    bucket.clear();
                    count.set(0);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static final class Value<T> {

        private final T value;

        private final long timestamp;

        private final double weight;

        private Value(final T value, final long timestamp, final double weight) {
            this.value = value;
            this.weight = weight;
            this.timestamp = timestamp;
        }

        public T getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
