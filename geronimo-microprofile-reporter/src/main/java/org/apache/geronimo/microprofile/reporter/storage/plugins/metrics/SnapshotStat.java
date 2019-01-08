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

public class SnapshotStat {

    private final int size;

    private final double median;

    private final double mean;

    private final double min;

    private final double max;

    private final double stdDev;

    private final double pc75;

    private final double pc95;

    private final double pc98;

    private final double pc99;

    private final double pc999;

    SnapshotStat(final int size, final double median, final double mean,
                        final double min, final double max, final double stdDev,
                        final double pc75, final double pc95, final double pc98,
                        final double pc99, final double pc999) {
        this.size = size;
        this.median = median;
        this.mean = mean;
        this.min = min;
        this.max = max;
        this.stdDev = stdDev;
        this.pc75 = pc75;
        this.pc95 = pc95;
        this.pc98 = pc98;
        this.pc99 = pc99;
        this.pc999 = pc999;
    }

    public double get75thPercentile() {
        return pc75;
    }

    public double get95thPercentile() {
        return pc95;
    }

    public double get98thPercentile() {
        return pc98;
    }

    public double get99thPercentile() {
        return pc99;
    }

    public double get999thPercentile() {
        return pc999;
    }
}
