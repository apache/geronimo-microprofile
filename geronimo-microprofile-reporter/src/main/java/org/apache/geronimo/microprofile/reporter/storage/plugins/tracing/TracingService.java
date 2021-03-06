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

import javax.enterprise.inject.Vetoed;

import org.apache.geronimo.microprofile.opentracing.common.impl.FinishedSpan;
import org.apache.geronimo.microprofile.reporter.storage.data.MicroprofileDatabase;

import io.opentracing.Span;

@Vetoed
class TracingService {
    private final MicroprofileDatabase database;
    private final SpanMapper mapper;

    TracingService(final MicroprofileDatabase database, final SpanMapper mapper) {
        this.database = database;
        this.mapper = mapper;
    }

    void onSpan(final Object event) {
        final FinishedSpan finishedSpan = FinishedSpan.class.cast(event);
        final Span value = finishedSpan.getSpan();
        if (!value.getClass().getName().equals("org.apache.geronimo.microprofile.opentracing.common.impl.SpanImpl")) {
            return;
        }
        final SpanEntry mapped = mapper.map(value);
        if (mapped == null) {
            return;
        }
        database.getSpans().add(mapped);
    }
}
