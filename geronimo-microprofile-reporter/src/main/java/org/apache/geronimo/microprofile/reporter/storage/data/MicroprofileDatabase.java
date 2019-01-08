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
package org.apache.geronimo.microprofile.reporter.storage.data;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.event.Observes;

import org.apache.geronimo.microprofile.reporter.storage.plugins.health.CheckSnapshot;
import org.apache.geronimo.microprofile.reporter.storage.plugins.metrics.MeterSnapshot;
import org.apache.geronimo.microprofile.reporter.storage.plugins.metrics.SnapshotStat;
import org.apache.geronimo.microprofile.reporter.storage.plugins.metrics.TimerSnapshot;
import org.apache.geronimo.microprofile.reporter.storage.plugins.tracing.SpanEntry;

@ApplicationScoped
public class MicroprofileDatabase {
    private final InMemoryDatabase<SpanEntry> spanDatabase = new InMemoryDatabase<>("none");
    private final Map<String, InMemoryDatabase<Long>> counters = new HashMap<>();
    private final Map<String, InMemoryDatabase<Double>> gauges = new HashMap<>();
    private final Map<String, InMemoryDatabase<SnapshotStat>> histograms = new HashMap<>();
    private final Map<String, InMemoryDatabase<MeterSnapshot>> meters = new HashMap<>();
    private final Map<String, InMemoryDatabase<TimerSnapshot>> timers = new HashMap<>();
    private final Map<String, InMemoryDatabase<CheckSnapshot>> checks = new HashMap<>();

    public InMemoryDatabase<SpanEntry> getSpans() {
        return spanDatabase;
    }

    public Map<String, InMemoryDatabase<Long>> getCounters() {
        return counters;
    }

    public Map<String, InMemoryDatabase<Double>> getGauges() {
        return gauges;
    }

    public Map<String, InMemoryDatabase<SnapshotStat>> getHistograms() {
        return histograms;
    }

    public Map<String, InMemoryDatabase<MeterSnapshot>> getMeters() {
        return meters;
    }

    public Map<String, InMemoryDatabase<TimerSnapshot>> getTimers() {
        return timers;
    }

    public Map<String, InMemoryDatabase<CheckSnapshot>> getChecks() {
        return checks;
    }

    void onStop(@Observes @Destroyed(ApplicationScoped.class) final Object stop) {
        Stream.of(counters, gauges, histograms, meters, timers).forEach(Map::clear);
    }
}
