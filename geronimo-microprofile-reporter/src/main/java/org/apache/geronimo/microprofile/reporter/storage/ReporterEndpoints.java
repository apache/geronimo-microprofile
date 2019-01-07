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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.metrics.Snapshot;

import io.opentracing.Span;

@Path("geronimo/microprofile/reporter")
@ApplicationScoped
@Produces(TEXT_HTML)
public class ReporterEndpoints {
    private static final Colors COLORS = new Colors("#007bff", "#0000CD");

    @Inject
    private MicroprofileDatabase database;

    @Inject
    private SpanMapper spanMapper;

    @GET
    public Html get() {
        return new Html("main.html")
                .with("view", "index.html")
                .with("colors", COLORS)
                .with("title", "Home")
                .with("tiles", asList("Spans", "Counters", "Gauges", "Histograms", "Meters", "Timers"));
    }

    @GET
    @Path("index.html") // we are too used to that to not provide it
    public Html getIndex() {
        return get();
    }

    @GET
    @Path("counters")
    public Html getCounters() {
        return new Html("main.html")
                .with("view", "counters.html")
                .with("colors", COLORS)
                .with("title", "Counters")
                .with("counters", new TreeSet<>(database.getCounters().keySet()));
    }

    @GET
    @Path("counter")
    public Html getCounter(@QueryParam("counter") final String name) {
        final InMemoryDatabase<Long> db = database.getCounters().get(name);
        return new Html("main.html")
                .with("view", "counter.html")
                .with("colors", COLORS)
                .with("title", "Counters")
                .with("name", name)
                .with("unit", db == null ? null : db.getUnit())
                .with("message", db == null ? "No matching counter for name '" + name + "'" : null)
                .with("points", db == null ? null : db.snapshot().stream().map(Point::new).collect(toList()));
    }

    @GET
    @Path("gauges")
    public Html getGauges() {
        return new Html("main.html")
                .with("view", "gauges.html")
                .with("colors", COLORS)
                .with("title", "Gauges")
                .with("gauges", new TreeSet<>(database.getGauges().keySet()));
    }

    @GET
    @Path("gauge")
    public Html getGauge(@QueryParam("gauge") final String name) {
        final InMemoryDatabase<Double> db = database.getGauges().get(name);
        return new Html("main.html")
                .with("view", "gauge.html")
                .with("colors", COLORS)
                .with("title", "Gauges")
                .with("name", name)
                .with("unit", db == null ? null : db.getUnit())
                .with("message", db == null ? "No matching gauge for name '" + name + "'" : null)
                .with("points", db == null ? null : db.snapshot().stream().map(Point::new).collect(toList()));
    }

    @GET
    @Path("histograms")
    public Html getHistograms() {
        return new Html("main.html")
                .with("view", "histograms.html")
                .with("colors", COLORS)
                .with("title", "Histograms")
                .with("histograms", new TreeSet<>(database.getHistograms().keySet()));
    }

    @GET
    @Path("histogram")
    public Html getHistogram(@QueryParam("histogram") final String name) {
        final InMemoryDatabase<Snapshot> db = database.getHistograms().get(name);
        return new Html("main.html")
                .with("view", "histogram.html")
                .with("colors", COLORS)
                .with("title", "Histogram")
                .with("name", name)
                .with("unit", db == null ? null : db.getUnit())
                .with("message", db == null ? "No matching histogram for name '" + name + "'" : null)
                .with("points", db == null ? null : db.snapshot().stream().map(Point::new).collect(toList()));
    }

    @GET
    @Path("meters")
    public Html getMeters() {
        return new Html("main.html")
                .with("view", "meters.html")
                .with("colors", COLORS)
                .with("title", "Meters")
                .with("meters", new TreeSet<>(database.getMeters().keySet()));
    }

    @GET
    @Path("meter")
    public Html getMeter(@QueryParam("meter") final String name) {
        final InMemoryDatabase<MicroprofileDatabase.MeterSnapshot> db = database.getMeters().get(name);
        return new Html("main.html")
                .with("view", "meter.html")
                .with("colors", COLORS)
                .with("title", "Meter")
                .with("name", name)
                .with("unit", db == null ? null : db.getUnit())
                .with("message", db == null ? "No matching meter for name '" + name + "'" : null)
                .with("points", db == null ? null : db.snapshot().stream().map(Point::new).collect(toList()));
    }

    @GET
    @Path("timers")
    public Html getTimers() {
        return new Html("main.html")
                .with("view", "timers.html")
                .with("colors", COLORS)
                .with("title", "Timers")
                .with("timers", new TreeSet<>(database.getTimers().keySet()));
    }

    @GET
    @Path("timer")
    public Html getTimer(@QueryParam("timer") final String name) {
        final InMemoryDatabase<MicroprofileDatabase.TimerSnapshot> db = database.getTimers().get(name);
        return new Html("main.html")
                .with("view", "timer.html")
                .with("colors", COLORS)
                .with("title", "Timer")
                .with("name", name)
                .with("unit", db == null ? null : db.getUnit())
                .with("message", db == null ? "No matching timer for name '" + name + "'" : null)
                .with("points", db == null ? null : db.snapshot().stream().map(Point::new).collect(toList()));
    }

    @GET
    @Path("spans")
    public Html getSpans() {
        final InMemoryDatabase<Span> db = database.getSpans();
        return new Html("main.html")
                .with("view", "spans.html")
                .with("colors", COLORS)
                .with("title", "Spans")
                .with("spans", db == null ?
                        null :
                        db.snapshot().stream()
                            .map(it -> new Point<>(it.getTimestamp(), spanMapper.map(it.getValue())))
                            .collect(toList()));
    }

    @GET
    @Path("span")
    public Html getSpan(@QueryParam("spanId") final String id) {
        final SpanMapper.SpanEntry value = database.getSpans().snapshot().stream()
               .map(it -> spanMapper.map(it.getValue()))
               .filter(it -> it.getSpanId().equals(id))
               .findFirst()
               .orElseThrow(() -> new BadRequestException("No matching span"));
        return new Html("main.html")
                .with("view", "span.html")
                .with("colors", COLORS)
                .with("title", "Span")
                .with("span", value);
    }

    public static class Point<T> {
        private final long timestamp;
        private final T value;

        private Point(final InMemoryDatabase.Value<T> value) {
            this(value.getTimestamp(), value.getValue());
        }

        private Point(final long timestamp, final T value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    private static class Colors {
        private final String main;
        private final String hover;

        private Colors(final String main, final String hover) {
            this.main = main;
            this.hover = hover;
        }
    }
}
