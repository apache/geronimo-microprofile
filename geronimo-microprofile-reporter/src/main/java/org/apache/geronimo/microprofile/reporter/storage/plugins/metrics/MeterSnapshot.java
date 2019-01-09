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
package org.apache.geronimo.microprofile.reporter.storage.plugins.metrics;

public class MeterSnapshot {
    private final long count;
    private final double rateMean;
    private final double rate1;
    private final double rate5;
    private final double rate15;

    MeterSnapshot(final long count, final double rateMean,
                          final double rate1, final double rate5, final double rate15) {
        this.count = count;
        this.rateMean = rateMean;
        this.rate1 = rate1;
        this.rate5 = rate5;
        this.rate15 = rate15;
    }
}
