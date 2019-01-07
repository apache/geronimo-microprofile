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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.opentracing.Span;

@ApplicationScoped
public class SpanMapper {
    private boolean active;

    private Method getSpanId;
    private Method getTraceId;
    private Method getParentId;
    private Method getName;
    private Method getTimestamp;
    private Method getDuration;
    private Method getKind;
    private Method getTags;
    private Method getLogs;
    private Method logGetTimestampMicros;
    private Method logGetFields;

    @PostConstruct
    private void init() {
        try {
            final Class<?> spanImpl = Thread.currentThread().getContextClassLoader()
                    .loadClass("org.apache.geronimo.microprofile.opentracing.common.impl.SpanImpl");
            getTraceId = spanImpl.getMethod("getTraceId");
            getSpanId = spanImpl.getMethod("getId");
            getParentId = spanImpl.getMethod("getParentId");
            getName = spanImpl.getMethod("getName");
            getTimestamp = spanImpl.getMethod("getTimestamp");
            getDuration = spanImpl.getMethod("getDuration");
            getKind = spanImpl.getMethod("getKind");
            getTags = spanImpl.getMethod("getTags");
            getLogs = spanImpl.getMethod("getLogs");

            final Class<?> logType = Class.class
                    .cast(ParameterizedType.class.cast(getLogs.getGenericReturnType()).getActualTypeArguments()[0]);
            logGetTimestampMicros = logType.getMethod("getTimestampMicros");
            logGetFields = logType.getMethod("getFields");

            active = true;
        } catch (final Exception e) {
            active = false;
        }
    }

    public SpanEntry map(final Span span) {
        if (!active) {
            return null;
        }
        try {
            final Collection<LogEntry> logs = ofNullable((Collection<?>) getLogs.invoke(span))
                    .map(it -> it.stream().map(log -> {
                        try {
                            return new LogEntry(Long.class.cast(logGetTimestampMicros.invoke(log)), Map.class.cast(logGetFields.invoke(log)));
                        } catch (final IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        } catch (final InvocationTargetException e) {
                            throw new IllegalStateException(e.getTargetException());
                        }
                    }).collect(toList()))
                    .orElseGet(Collections::emptyList);
            return new SpanEntry(
                    stringify(getSpanId.invoke(span)),
                    stringify(getTraceId.invoke(span)),
                    stringify(getParentId.invoke(span)),
                    stringify(getName.invoke(span)),
                    Long.class.cast(getTimestamp.invoke(span)),
                    Long.class.cast(getDuration.invoke(span)),
                    stringify(getKind.invoke(span)),
                    Map.class.cast(getTags.invoke(span)), logs);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private static String stringify(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static class LogEntry {

        private final long timestampMicros;

        private final Map<String, Object> fields;

        private LogEntry(final long timestampMicros, final Map<String, Object> fields) {
            this.timestampMicros = timestampMicros;
            this.fields = fields;
        }

        public long getTimestampMicros() {
            return timestampMicros;
        }

        public Map<String, Object> getFields() {
            return fields;
        }
    }

    public static class SpanEntry {
        private final String spanId;
        private final String traceId;
        private final String parentId;
        private final String name;
        private final long timestamp;
        private final long duration;
        private final String kind;
        private final Map<String, Object> tags;
        private final Collection<LogEntry> getLogs;

        private SpanEntry(final String spanId, final String traceId, final String parentId, final String name,
                final long timestamp, final long duration, final String kind, final Map<String, Object> tags,
                final Collection<LogEntry> getLogs) {
            this.spanId = spanId;
            this.traceId = traceId;
            this.parentId = parentId;
            this.name = name;
            this.timestamp = timestamp;
            this.duration = duration;
            this.kind = kind;
            this.tags = tags;
            this.getLogs = getLogs;
        }

        public String getSpanId() {
            return spanId;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getParentId() {
            return parentId;
        }

        public String getName() {
            return name;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getDuration() {
            return duration;
        }

        public String getKind() {
            return kind;
        }

        public Map<String, Object> getTags() {
            return tags;
        }

        public Collection<LogEntry> getGetLogs() {
            return getLogs;
        }
    }
}
