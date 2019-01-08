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

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@Vetoed
class HealthDataExtractor {
    private final List<HealthCheck> checks = new ArrayList<>();

    Stream<CheckSnapshot> doCheck() {
        return checks.stream().map(check -> {
            try {
                return check.call();
            } catch (final RuntimeException re) {
                return HealthCheckResponse.named(check.getClass().getName())
                                          .down()
                                          .withData("exceptionMessage", re.getMessage())
                                          .build();
            }
        }).map(healthCheckResponse -> new CheckSnapshot(
                healthCheckResponse.getName(),
                ofNullable(healthCheckResponse.getState()).orElse(HealthCheckResponse.State.DOWN).name(),
                healthCheckResponse.getData().map(HashMap::new).orElseGet(HashMap::new)));
    }

    void register(final Object check) {
        checks.add(HealthCheck.class.cast(check));
    }
}
