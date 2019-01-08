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
package org.apache.geronimo.microprofile.reporter.storage.front;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.geronimo.microprofile.reporter.storage.data.InMemoryDatabase;
import org.apache.geronimo.microprofile.reporter.storage.data.MicroprofileDatabase;
import org.apache.geronimo.microprofile.reporter.storage.plugins.health.CheckSnapshot;
import org.apache.geronimo.microprofile.reporter.storage.plugins.health.HealthService;
import org.apache.geronimo.microprofile.reporter.storage.plugins.metrics.MeterSnapshot;
import org.apache.geronimo.microprofile.reporter.storage.plugins.metrics.SnapshotStat;
import org.apache.geronimo.microprofile.reporter.storage.plugins.metrics.TimerSnapshot;
import org.apache.geronimo.microprofile.reporter.storage.plugins.tracing.SpanEntry;
import org.apache.geronimo.microprofile.reporter.storage.plugins.tracing.TracingExtension;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("geronimo/microprofile/reporter")
@ApplicationScoped
@Produces(TEXT_HTML)
public class ReporterEndpoints {
    private static final Colors COLORS = new Colors("#007bff", "#0000CD");

    @Inject
    private MicroprofileDatabase database;

    @Inject
    private HealthService health;

    @Inject
    private TracingExtension tracing;

    @Inject
    @ConfigProperty(name = "geronimo.microprofile.reporter.resources.chartjs",
            defaultValue = "/META-INF/resources/webjars/chart.js/2.7.3/dist/Chart.bundle.min.js")
    private String chartJsResource;

    private String chartJs;
    private List<String> tiles;

    @PostConstruct
    private void init() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        chartJs = (chartJsResource.startsWith("/") ?
                Stream.of(chartJsResource, chartJsResource.substring(1)) : Stream.of(chartJsResource, '/' + chartJsResource))
                .map(it -> {
                    final InputStream stream = loader.getResourceAsStream(it);
                    if (stream == null) {
                        return null;
                    }
                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                            requireNonNull(stream,
                                    "Chart.js bundle not found")))) {
                        return reader.lines().collect(joining("\n"));
                    } catch (final IOException e) {
                        throw new IllegalStateException("Didn't find chart.js bundle");
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No " + chartJsResource + " found, did you add org.webjars.bower:chart.js:2.7.3 to your classpath?"));

        tiles = new ArrayList<>(7);
        if (tracing.isActive()) {
            tiles.add("Spans");
        }
        tiles.addAll(asList("Counters", "Gauges", "Histograms", "Meters", "Timers"));
        if (health.isActive()) {
            tiles.add("Health Checks");
        }
    }

    @GET
    public Html get() {
        return new Html("main.html")
                .with("view", "index.html")
                .with("colors", COLORS)
                .with("title", "Home")
                .with("tiles", tiles);
    }

    @GET
    @Path("index.html") // we are too used to that to not provide it
    public Html getIndex() {
        return get();
    }

    @GET
    @Path("Chart.bundle.min.js")
    @Produces("application/javascript")
    public String getChartJsBundle() {
        return chartJs;
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
        final InMemoryDatabase<SnapshotStat> db = database.getHistograms().get(name);
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
        final InMemoryDatabase<MeterSnapshot> db = database.getMeters().get(name);
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
        final InMemoryDatabase<TimerSnapshot> db = database.getTimers().get(name);
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
        final InMemoryDatabase<SpanEntry> db = database.getSpans();
        return new Html("main.html")
                .with("view", "spans.html")
                .with("colors", COLORS)
                .with("title", "Spans")
                .with("spans", db == null ?
                        null :
                        db.snapshot().stream()
                            .map(it -> new Point<>(it.getTimestamp(), it.getValue()))
                            .collect(toList()));
    }

    @GET
    @Path("span")
    public Html getSpan(@QueryParam("spanId") final String id) {
        final SpanEntry value = database.getSpans().snapshot().stream()
               .map(InMemoryDatabase.Value::getValue)
               .filter(it -> it.getSpanId().equals(id))
               .findFirst()
               .orElseThrow(() -> new BadRequestException("No matching span"));
        return new Html("main.html")
                .with("view", "span.html")
                .with("colors", COLORS)
                .with("title", "Span")
                .with("span", value);
    }

    @GET
    @Path("health-checks")
    public Html getHealths() {
        return new Html("main.html")
                .with("view", "health-checks.html")
                .with("colors", COLORS)
                .with("title", "Health Checks")
                .with("checks", new TreeSet<>(database.getChecks().keySet()));
    }

    @GET
    @Path("check")
    public Html getHealth(@QueryParam("check") final String name) {
        final InMemoryDatabase<CheckSnapshot> db = database.getChecks().get(name);
        return new Html("main.html")
                .with("view", "health.html")
                .with("colors", COLORS)
                .with("title", "Health Check")
                .with("name", name)
                .with("message", db == null ? "No matching check for name '" + name + "'" : null)
                .with("points", db == null ? null : db.snapshot().stream().map(Point::new).collect(toList()));
    }

    @GET
    @Path("health-check-detail")
    public Html getHealthCheckDetail(@QueryParam("check") final String name) {
        final InMemoryDatabase.Value<CheckSnapshot> last = ofNullable(database.getChecks().get(name))
                .map(InMemoryDatabase::snapshot)
                .map(it -> it.isEmpty() ? null : it.getLast())
                .orElse(null); // todo: orElseGet -> call them all and filter per name?
        return new Html("main.html")
                .with("view", "health-check-detail.html")
                .with("colors", COLORS)
                .with("title", "Health Check")
                .with("name", name)
                .with("message", last == null ? "No matching check yet for name '" + name + "'" : null)
                .with("lastCheckTimestamp", last == null ? null : new Date(last.getTimestamp()))
                .with("lastCheck", last == null ? null : last.getValue());
    }

    @GET
    @Path("health-application")
    public Html getApplicationHealth() {
        final List<CheckSnapshot> checks = health.doCheck()
                                                 .sorted(comparing(CheckSnapshot::getName))
                                                 .collect(toList());
        return new Html("main.html")
                .with("view", "health-application.html")
                .with("colors", COLORS)
                .with("title", "Application Health")
                .with("globalState", checks.stream()
                                           .filter(it -> it.getState().equals("DOWN")).findAny()
                                           .map(CheckSnapshot::getState).orElse("UP"))
                .with("checks", checks);
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
