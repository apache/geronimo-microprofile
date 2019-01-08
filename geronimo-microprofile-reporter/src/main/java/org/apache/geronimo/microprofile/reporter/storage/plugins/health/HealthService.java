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
package org.apache.geronimo.microprofile.reporter.storage.plugins.health;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.geronimo.microprofile.reporter.storage.data.InMemoryDatabase;
import org.apache.geronimo.microprofile.reporter.storage.data.MicroprofileDatabase;
import org.apache.geronimo.microprofile.reporter.storage.plugins.Tick;

// cdi indirection to not require health check api and impl to be present
@ApplicationScoped
public class HealthService {
    private final HealthDataExtractor extractor = new HealthDataExtractor();

    @Inject
    private HealthRegistry registry;

    @Inject
    private MicroprofileDatabase database;

    public boolean isActive() {
        return registry.getApiType() != null;
    }

    void register(final Object check) {
        extractor.register(check);
    }

    public Stream<CheckSnapshot> doCheck() {
        return extractor.doCheck();
    }

    public void onTick(@Observes final Tick tick) {
        if (isActive()) {
            doCheck().forEach(this::updateHealthCheck);
        }
    }

    private void updateHealthCheck(final CheckSnapshot healthCheckResponse) {
        final String name = healthCheckResponse.getName();
        InMemoryDatabase<CheckSnapshot> db = database.getChecks().get(name);
        if (db == null) {
            db = new InMemoryDatabase<>(database.getAlpha(), database.getBucketSize(), "check");
            final InMemoryDatabase<CheckSnapshot> existing = database.getChecks().putIfAbsent(name, db);
            if (existing != null) {
                db = existing;
            }
        }
        db.add(healthCheckResponse);
    }
}
