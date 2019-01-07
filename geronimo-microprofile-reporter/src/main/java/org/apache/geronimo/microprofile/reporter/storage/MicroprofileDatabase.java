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

import static java.lang.Thread.NORM_PRIORITY;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.apache.geronimo.microprofile.opentracing.common.impl.FinishedSpan;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.opentracing.Span;

// TODO: make span dependency optional and composable (events?)
@ApplicationScoped
public class MicroprofileDatabase {

    @Inject
    @ConfigProperty(name = "geronimo.microprofile.reporter.metrics.pollingInterval", defaultValue = "5000")
    private Long pollingInterval;

    @Inject
    @RegistryType(type = BASE)
    private MetricRegistry baseRegistry;

    @Inject
    @RegistryType(type = VENDOR)
    private MetricRegistry vendorRegistry;

    @Inject
    private MetricRegistry applicationRegistry;

    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> pollFuture;

    private Map<String, MetricRegistry> metrics;

    private final InMemoryDatabase<Span> spanDatabase = new InMemoryDatabase<>("none");
    private final Map<String, InMemoryDatabase<Long>> counters = new HashMap<>();
    private final Map<String, InMemoryDatabase<Double>> gauges = new HashMap<>();
    private final Map<String, InMemoryDatabase<Snapshot>> histograms = new HashMap<>();
    private final Map<String, InMemoryDatabase<MeterSnapshot>> meters = new HashMap<>();
    private final Map<String, InMemoryDatabase<TimerSnapshot>> timers = new HashMap<>();

    public InMemoryDatabase<Span> getSpans() {
        return spanDatabase;
    }

    public Map<String, InMemoryDatabase<Long>> getCounters() {
        return counters;
    }

    public Map<String, InMemoryDatabase<Double>> getGauges() {
        return gauges;
    }

    public Map<String, InMemoryDatabase<Snapshot>> getHistograms() {
        return histograms;
    }

    public Map<String, InMemoryDatabase<MeterSnapshot>> getMeters() {
        return meters;
    }

    public Map<String, InMemoryDatabase<TimerSnapshot>> getTimers() {
        return timers;
    }

    private void poll() {
        metrics.forEach((type, registry) -> {
            registry.getCounters().forEach((name, counter) -> {
                final String virtualName = getMetricStorageName(type, name);
                final long count = counter.getCount();
                getDb(counters, virtualName, registry, name).add(count);
            });

            registry.getGauges().forEach((name, gauge) -> {
                final String virtualName = getMetricStorageName(type, name);
                final Object value = gauge.getValue();
                if (Number.class.isInstance(value)) {
                    try {
                        getDb(gauges, virtualName, registry, name).add(Number.class.cast(value).doubleValue());
                    } catch (final NullPointerException | NumberFormatException nfe) {
                        // ignore, we can't do much if the value is not a double
                    }
                } // else ignore, will not be able to do anything of it anyway
            });

            registry.getHistograms().forEach((name, histogram) -> {
                final String virtualName = getMetricStorageName(type, name);
                final Snapshot snapshot = histogram.getSnapshot();
                getDb(histograms, virtualName, registry, name).add(snapshot);
            });

            registry.getMeters().forEach((name, meter) -> {
                final String virtualName = getMetricStorageName(type, name);
                final MeterSnapshot snapshot = new MeterSnapshot(meter);
                getDb(meters, virtualName, registry, name).add(snapshot);
            });

            registry.getTimers().forEach((name, timer) -> {
                final String virtualName = getMetricStorageName(type, name);
                final TimerSnapshot snapshot = new TimerSnapshot(new MeterSnapshot(timer), timer.getSnapshot());
                getDb(timers, virtualName, registry, name).add(snapshot);
            });
        });
    }

    // alternatively we can decorate the registries and register/unregister following the registry lifecycle
    // shouldnt be worth it for now
    private <T> InMemoryDatabase<T> getDb(final Map<String, InMemoryDatabase<T>> registry,
                                          final String virtualName, final MetricRegistry source,
                                          final String key) {
        InMemoryDatabase<T> db = registry.get(virtualName);
        if (db == null) {
            db = new InMemoryDatabase<>(ofNullable(source.getMetadata().get(key).getUnit()).orElse(""));
            final InMemoryDatabase<T> existing = registry.putIfAbsent(virtualName, db);
            if (existing != null) {
                db = existing;
            }
        }
        return db;
    }

    private String getMetricStorageName(final String type, final String name) {
        return type + "#" + name;
    }

    private String name(final Object start) {
        if (ServletContext.class.isInstance(start)) {
            final ServletContext context = ServletContext.class.cast(start);
            try {
                return "[web=" + context.getVirtualServerName() + '/' + context.getContextPath() + "]";
            } catch (final Error | Exception e) { // no getVirtualServerName() for this context
                return "[web=" + context.getContextPath() + "]";
            }
        }
        return start.toString();
    }

    void onSpan(@Observes final FinishedSpan span) {
        final Span value = span.getSpan();
        if (value.getClass().getName().equals("org.apache.geronimo.microprofile.opentracing.common.impl.SpanImpl")) {
            spanDatabase.add(value);
        } // else we will not be able to read the metadata
    }

    void onStart(@Observes @Initialized(ApplicationScoped.class) final Object start) {
        metrics = new HashMap<>(3);
        metrics.put("vendor", vendorRegistry);
        metrics.put("base", baseRegistry);
        metrics.put("application", applicationRegistry);

        final ClassLoader appLoader = Thread.currentThread().getContextClassLoader();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, "geronimo-microprofile-reporter-poller-" + name(start));
            thread.setContextClassLoader(appLoader);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != NORM_PRIORITY) {
                thread.setPriority(NORM_PRIORITY);
            }
            return thread;
        });
        pollFuture = scheduler.scheduleAtFixedRate(this::poll, pollingInterval, pollingInterval, MILLISECONDS);
    }

    void onStop(@Observes @Destroyed(ApplicationScoped.class) final Object stop) {
        if (pollFuture != null) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(10, SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        Stream.of(counters, gauges, histograms, meters, timers).forEach(Map::clear);
    }

    public static class TimerSnapshot {
        private final MeterSnapshot meter;
        private final Snapshot histogram;

        private TimerSnapshot(final MeterSnapshot meter, final Snapshot histogram) {
            this.meter = meter;
            this.histogram = histogram;
        }

        public MeterSnapshot getMeter() {
            return meter;
        }

        public Snapshot getHistogram() {
            return histogram;
        }
    }

    public static class MeterSnapshot {
        private final long count;
        private final double rateMean;
        private final double rate1;
        private final double rate5;
        private final double rate15;

        private MeterSnapshot(final Metered meter) {
            this(meter.getCount(), meter.getMeanRate(), meter.getOneMinuteRate(), meter.getFiveMinuteRate(), meter.getFifteenMinuteRate());
        }

        private MeterSnapshot(final long count, final double rateMean,
                              final double rate1, final double rate5, final double rate15) {
            this.count = count;
            this.rateMean = rateMean;
            this.rate1 = rate1;
            this.rate5 = rate5;
            this.rate15 = rate15;
        }

        public long getCount() {
            return count;
        }

        public double getRateMean() {
            return rateMean;
        }

        public double getRate1() {
            return rate1;
        }

        public double getRate5() {
            return rate5;
        }

        public double getRate15() {
            return rate15;
        }
    }
}
