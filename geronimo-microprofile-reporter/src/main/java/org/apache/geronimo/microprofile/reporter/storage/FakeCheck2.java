/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.microprofile.reporter.storage;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

@Health
@Dependent
public class FakeCheck2 implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder named = HealthCheckResponse.named("check_2");
        if (System.currentTimeMillis() % 2 == 0) {
            return named.up().withData("foo", "bar").withData("another", "dummy").build();
        }
        return named.down().withData("foo", "bar").withData("another", "dummy").build();
    }
}
