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
package org.apache.geronimo.microprofile.reporter.storage.html;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.geronimo.microprofile.reporter.storage.TemplatingEngine;
import org.junit.Test;

public class TemplatingEngineTest {

    private final TemplatingEngine engine = new TemplatingEngine();

    /*
     * // simple passthrough impl with these specificites
     * // - @include(template)
     * // - @include(template,newModel1=someDataToPassthrough1,newModel=2someDataToPassthrough1)
     * // - @each($collectionVar,templatePath)
     * // - $var from data with dot notation support
     */
    @Test
    public void passthrough() {
        final String template = "<test>foo</test>";
        assertEquals(template, engine.compileIfNeeded(template, it -> null).apply(null));
    }

    @Test
    public void varSimple() {
        assertEquals("<test>foo yes</test>",
                engine.compileIfNeeded("<test>foo $foo</test>", it -> null).apply(singletonMap("foo", "yes")));
    }

    @Test
    public void includeSimple() {
        assertEquals("<test>foo yes</test>", engine
                .compileIfNeeded("<test>foo @include(tpl.tpl)</test>", it -> "tpl.tpl".equals(it) ? "yes" : null).apply(null));
    }

    @Test
    public void includeRemapping() {
        assertEquals("<test>foo ok</test>",
                engine.compileIfNeeded("<test>foo @include(tpl.tpl,n=foo)</test>", it -> "tpl.tpl".equals(it) ? "$n.bar" : null)
                        .apply(singletonMap("foo", singletonMap("bar", "ok"))));
    }

    @Test
    public void each() {
        assertEquals("<test>foo a b </test>",
                engine.compileIfNeeded("<test>foo @each($col,it.tpl)</test>", it -> "it.tpl".equals(it) ? "$$value " : null)
                        .apply(singletonMap("col", asList("a", "b"))));
    }

    @Test
    public void eachInline() {
        assertEquals("<test>foo a b </test>",
                engine.compileIfNeeded("<test>foo @each($col,inline:$$value )</test>", it -> null)
                        .apply(singletonMap("col", asList("a", "b"))));
    }

    @Test
    public void lowercase() {
        assertEquals("<test>camelcase</test>",
                engine.compileIfNeeded("<test>@lowercase($foo)</test>", it -> null).apply(singletonMap("foo", "CamelCase")));
    }

    @Test
    public void escaping() {
        assertEquals("\"", engine.compileIfNeeded("\\\"", it -> null).apply(null));
    }

    @Test
    public void condition() {
        final Function<Object, String> compiled = engine.compileIfNeeded("$$value@if($hasNext,inline:,)",
                it -> null);

        final Map<String, Object> data = new HashMap<String, Object>() {{
            put("$value", "test");
            put("hasNext", "true");
        }};
        assertEquals("test,", compiled.apply(data));

        data.put("hasNext", "");
        assertEquals("test", compiled.apply(data));
    }
}
