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
package org.apache.geronimo.microprofile.reporter.storage.plugins.metrics;

import static java.util.Optional.ofNullable;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.geronimo.microprofile.reporter.storage.data.InMemoryDatabase;
import org.apache.geronimo.microprofile.reporter.storage.data.MicroprofileDatabase;
import org.apache.geronimo.microprofile.reporter.storage.plugins.Tick;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

@ApplicationScoped
public class MetricsService {
    @Inject
    private MicroprofileDatabase database;

    @Inject
    @RegistryType(type = BASE)
    private MetricRegistry baseRegistry;

    @Inject
    @RegistryType(type = VENDOR)
    private MetricRegistry vendorRegistry;

    @Inject
    private MetricRegistry applicationRegistry;

    private Map<String, MetricRegistry> metricsIndex;

    private void updateMetrics(final String type, final MetricRegistry registry) {
        registry.getCounters().forEach((name, counter) -> {
            final String virtualName = getMetricStorageName(type, name);
            final long count = counter.getCount();
            getDb(database.getCounters(), virtualName, registry, name).add(count);
        });

        registry.getGauges().forEach((name, gauge) -> {
            final String virtualName = getMetricStorageName(type, name);
            final Object value = gauge.getValue();
            if (Number.class.isInstance(value)) {
                try {
                    getDb(database.getGauges(), virtualName, registry, name).add(Number.class.cast(value).doubleValue());
                } catch (final NullPointerException | NumberFormatException nfe) {
                    // ignore, we can't do much if the value is not a double
                }
            } // else ignore, will not be able to do anything of it anyway
        });

        registry.getHistograms().forEach((name, histogram) -> {
            final String virtualName = getMetricStorageName(type, name);
            final Snapshot snapshot = histogram.getSnapshot();
            getDb(database.getHistograms(), virtualName, registry, name)
                    .add(new SnapshotStat(snapshot.size(), snapshot.getMedian(), snapshot.getMean(), snapshot.getMin(), snapshot.getMax(), snapshot.getStdDev(),
                            snapshot.get75thPercentile(), snapshot.get95thPercentile(), snapshot.get98thPercentile(), snapshot.get99thPercentile(), snapshot.get999thPercentile()));
        });

        registry.getMeters().forEach((name, meter) -> {
            final String virtualName = getMetricStorageName(type, name);
            final MeterSnapshot snapshot = new MeterSnapshot(
                    meter.getCount(), meter.getMeanRate(), meter.getOneMinuteRate(), meter.getFiveMinuteRate(), meter.getFifteenMinuteRate());
            getDb(database.getMeters(), virtualName, registry, name).add(snapshot);
        });

        registry.getTimers().forEach((name, timer) -> {
            final String virtualName = getMetricStorageName(type, name);
            final Snapshot snapshot = timer.getSnapshot();
            final TimerSnapshot timerSnapshot = new TimerSnapshot(new MeterSnapshot(
                    timer.getCount(), timer.getMeanRate(), timer.getOneMinuteRate(), timer.getFiveMinuteRate(), timer.getFifteenMinuteRate()),
                    new SnapshotStat(snapshot.size(), snapshot.getMedian(), snapshot.getMean(), snapshot.getMin(), snapshot.getMax(), snapshot.getStdDev(),
                    snapshot.get75thPercentile(), snapshot.get95thPercentile(), snapshot.get98thPercentile(), snapshot.get99thPercentile(), snapshot.get999thPercentile()));
            getDb(database.getTimers(), virtualName, registry, name).add(timerSnapshot);
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

    @PostConstruct
    private void init() {
        metricsIndex = new HashMap<>(3);
        metricsIndex.put("vendor", vendorRegistry);
        metricsIndex.put("base", baseRegistry);
        metricsIndex.put("application", applicationRegistry);
    }

    void onTick(@Observes final Tick tick) {
        metricsIndex.forEach(this::updateMetrics);
    }
}
