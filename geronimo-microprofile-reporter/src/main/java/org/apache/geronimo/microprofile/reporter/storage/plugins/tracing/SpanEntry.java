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
package org.apache.geronimo.microprofile.reporter.storage.plugins.tracing;

import java.util.Collection;
import java.util.Map;

public class SpanEntry {
    private final String spanId;
    private final String traceId;
    private final String parentId;
    private final String name;
    private final long timestamp;
    private final long duration;
    private final String kind;
    private final Map<String, Object> tags;
    private final Collection<LogEntry> getLogs;

    SpanEntry(final String spanId, final String traceId, final String parentId, final String name,
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

    public static class LogEntry {

        private final long timestampMicros;

        private final Map<String, Object> fields;

        LogEntry(final long timestampMicros, final Map<String, Object> fields) {
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
}
