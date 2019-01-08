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

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.meecrowave.junit.InjectRule;
import org.apache.meecrowave.junit.MeecrowaveRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class HealthServiceTest {
    @ClassRule
    public static final MeecrowaveRule SERVER = new MeecrowaveRule();

    @Rule
    public final InjectRule injector = new InjectRule(this);

    @Inject
    private HealthService service;

    @Test
    public void ensureHealthChecksWereRegistered() {
        assertEquals(2, service.doCheck().count());
    }
}
