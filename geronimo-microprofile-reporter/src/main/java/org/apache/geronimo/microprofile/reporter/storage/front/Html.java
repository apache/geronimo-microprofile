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

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

public class Html {
    private final String name;
    private final Map<String, Object> data = new HashMap<>();

    public Html(final String name) {
        this.name = name;
    }

    public Html with(final String name, final Object data) {
        if (data == null) {
            return this;
        }
        this.data.put(name, data);
        return this;
    }

    String getName() {
        return name;
    }

    Map<String, Object> getData() {
        return unmodifiableMap(data);
    }
}
