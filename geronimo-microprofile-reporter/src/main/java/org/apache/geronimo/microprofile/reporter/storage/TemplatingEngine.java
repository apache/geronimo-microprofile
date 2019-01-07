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
package org.apache.geronimo.microprofile.reporter.storage;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TemplatingEngine {
    private final ConcurrentMap<AccessorKey, Function<Object, Object>> accessors = new ConcurrentHashMap<>();

    private final ConcurrentMap<TemplateKey, Collection<Function<Object, String>>> templates = new ConcurrentHashMap<>();

    @Inject
    private TemplateHelper templateHelper;

    // simple passthrough impl with these specificites
    // - @include(template)
    // - @include(template,newModel1=someDataToPassthrough1,newModel=2someDataToPassthrough1)
    // - @each($collectionVar,templatePath)
    // - $var from data with dot notation support
    public Function<Object, String> compileIfNeeded(final String template, final Function<String, String> templateLoader) {
        final Collection<Function<Object, String>> segments = templates.computeIfAbsent(new TemplateKey(templateLoader, template),
                key -> precompile(key.template, key.loader));
        return data -> segments.stream().map(it -> it.apply(data)).collect(joining());
    }

    private Collection<Function<Object, String>> precompile(final String template,
            final Function<String, String> templateLoader) {
        final Collection<Function<Object, String>> segments = new ArrayList<>();

        final StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        final char[] chars = template.toCharArray();
        String substring;
        for (int i = 0; i < chars.length; i++) {
            final char current = chars[i];
            if (escaped) {
                builder.append(current);
                escaped = false;
            } else if (current == '\\') { // escaping
                escaped = true;
            } else if (current == '$') { // variable
                final String value = builder.toString();
                segments.add(ctx -> value);
                builder.setLength(0);

                final StringBuilder variable = new StringBuilder();
                for (int j = i + 1; j < chars.length; j++) {
                    if (!Character.isJavaIdentifierPart(chars[j]) && chars[j] != '.') {
                        break;
                    }
                    variable.append(chars[j]);
                }
                i += variable.length();
                final String varName = variable.toString();
                segments.add(data -> {
                    final Object interpolated = interpolate(varName, data);
                    if (interpolated != null) {
                        return String.valueOf(interpolated);
                    }
                    return "";
                });
            } else if ((substring = template.substring(i)).startsWith("/*")) { // comment
                final int end = template.indexOf("*/", i);
                if (end < 0) {
                    throw new IllegalArgumentException("No comment end at index " + i + " for:\n" + template);
                }
                i = end + "*/".length();
            } else if (substring.startsWith("@include(")) {
                final String value = builder.toString();
                segments.add(data -> value);
                builder.setLength(0);

                final int end = findEndingParenthesis(chars, i + "@include(".length() + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Missing ')' token for @include at position " + i + " for:\n" + template);
                }
                final String tplPath = template.substring(i + "@include(".length(), end);
                i = end;
                segments.add(data -> {
                    final String interpolated = compileIfNeeded(tplPath, templateLoader).apply(data); // todo: compose segments
                    if (interpolated == null) {
                        return "";
                    }

                    final Object includeData;
                    final String templatePath;
                    if (interpolated.contains(",")) {
                        final String[] split = interpolated.split(",");
                        templatePath = split[0];

                        final Map<String, Object> map = new HashMap<>();
                        includeData = map;
                        for (int j = 1; j < split.length; j++) {
                            final String[] config = split[j].split("=");
                            if (config.length != 2) {
                                throw new IllegalArgumentException(
                                        "Passed data during a directive (@include) must set their alias, ex: name=foo.bar");
                            }
                            map.put(config[0], interpolate(config[1], data));
                        }
                    } else {
                        templatePath = interpolated;
                        includeData = data;
                    }

                    return compileIfNeeded(templateLoader.apply(templatePath), templateLoader).apply(includeData);
                });
            } else if (substring.startsWith("@escape(")) {
                final String value = builder.toString();
                segments.add(data -> value);
                builder.setLength(0);

                final int end = findEndingParenthesis(chars, i + "@escape(".length() + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Missing ')' token for @escape at position " + i + " for:\n" + template);
                }
                final String toEscape = template.substring(i + "@escape(".length(), end);
                i = end;
                segments.add(data -> {
                    final String escapableValue = compileIfNeeded(toEscape, templateLoader).apply(data);
                    if (escapableValue == null) {
                        return "";
                    }
                    return templateHelper.escape(escapableValue);
                });
            } else if (substring.startsWith("@lowercase(")) {
                final String value = builder.toString();
                segments.add(data -> value);
                builder.setLength(0);

                final int end = findEndingParenthesis(chars, i + "@lowercase(".length() + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Missing ')' token for @lowercase at position " + i + " for:\n" + template);
                }
                final String toEscape = template.substring(i + "@lowercase(".length(), end);
                i = end;
                segments.add(data -> {
                    final String escapableValue = compileIfNeeded(toEscape, templateLoader).apply(data);
                    if (escapableValue == null) {
                        return "";
                    }
                    return escapableValue.toLowerCase(ROOT);
                });
            } else if (substring.startsWith("@each(")) {
                final String value = builder.toString();
                segments.add(ctx -> value);
                builder.setLength(0);

                final int end = findEndingParenthesis(chars, i + "@each(".length() + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Missing ')' token for @each at position " + i +
                            " for:\n" + template);
                }

                final int startConfig = i + "@each(".length();
                final String config = template.substring(startConfig, end);
                i = end;

                final int sep = config.indexOf(",");
                if (sep < 0) {
                    throw new IllegalArgumentException(
                            "Bad configuration for @each, first parameter is the variable, second the template at index " +
                                    i + ", for:\n" + template);
                }

                final String variableName = config.substring(config.startsWith("$") ? 1 : 0, sep);
                final Function<Object, String> tplProvider;
                final String tpl = config.substring(sep + 1);
                if (tpl.startsWith("inline:")) {
                    final String completeTpl = tpl.substring("inline:".length());
                    tplProvider = data -> completeTpl;
                } else {
                    tplProvider = data -> templateLoader.apply(compileIfNeeded(tpl, templateLoader).apply(data));
                }
                segments.add(data -> {
                    final Object collection = interpolate(variableName, data);
                    if (collection == null) {
                        return "";
                    }
                    final Iterator<?> it;
                    if (Collection.class.isInstance(collection)) {
                        it = ((Collection<Object>) collection).iterator();
                    } else if (Map.class.isInstance(collection)) {
                        it = ((Map<Object, Object>) collection).entrySet().iterator();
                    } else {
                        throw new IllegalArgumentException("Only Collection and Map can be used in @each, got " + collection);
                    }
                    final String compiled = tplProvider.apply(data);
                    final StringBuilder out = new StringBuilder();
                    while (it.hasNext()) {
                        final Object next = it.next();
                        final boolean hasNext = it.hasNext();
                        final Map<String, Object> subData = new HashMap<>();
                        subData.put("$value", next);
                        subData.put("hasNext", hasNext);
                        out.append(compileIfNeeded(compiled, templateLoader).apply(subData));
                    }
                    return out.toString();
                });
            } else if (substring.startsWith("@if(")) {
                final String value = builder.toString();
                segments.add(ctx -> value);
                builder.setLength(0);

                final int end = findEndingParenthesis(chars, i + "@if(".length() + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Missing ')' token for @if at position " + i +
                            " for:\n" + template);
                }

                final String config = template.substring(i + "@if(".length(), end);
                i = end;

                final int sep = config.indexOf(",");
                if (sep < 0) {
                    throw new IllegalArgumentException(
                            "Bad configuration for @if, first parameter is the falsy condition, second the template. At index " +
                                    i + ", for:\n" + template);
                }

                final String variableName = config.substring(config.startsWith("$") ? 1 : 0, sep);
                final String tpl = config.substring(sep + 1);
                segments.add(data -> {
                    final Object condition = interpolate(variableName, data);
                    if (condition == null) {
                        return "";
                    }
                    final String conditionStr = String.valueOf(condition);
                    if ("false".equalsIgnoreCase(conditionStr) || conditionStr.isEmpty()) {
                        return "";
                    }

                    final String compiled = tpl.startsWith("inline:") ?
                            tpl.substring("inline:".length()) :
                            templateLoader.apply(compileIfNeeded(tpl, templateLoader).apply(data));
                    return compileIfNeeded(compiled, templateLoader).apply(data);
                });
            } else {
                builder.append(current);
            }
        }

        if (builder.length() > 0) {
            final String value = builder.toString();
            segments.add(ctx -> value);
        }

        return segments;
    }

    private int findEndingParenthesis(final char[] chars, final int from) {
        int remaining = 1;
        for (int i = from; i < chars.length; i++) {
            if (chars[i] == ')' && --remaining == 0) {
                return i;
            } else if (chars[i] == '(') {
                remaining++;
            }
        }
        return -1;
    }

    private Object getVariable(final Object registry, final String name) {
        if (registry == null) {
            return registry;
        }
        // map handling
        if (Map.class.isInstance(registry)) {
            return Map.class.cast(registry).get(name);
        }

        final Class<?> registryClass = registry.getClass();

        // array handling - foo.1.name syntax
        if (registryClass.isArray()) {
            return Array.get(registry, Integer.parseInt(name));
        }

        return accessors.computeIfAbsent(new AccessorKey(registryClass, name), key -> {
            // try getter
            try {
                final Method method = key.type
                        .getMethod("get" + Character.toUpperCase(key.name.charAt(0)) + key.name.substring(1));
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return o -> {
                    try {
                        return method.invoke(o);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    } catch (final InvocationTargetException e) {
                        throw new IllegalStateException(e.getTargetException());
                    }
                };
            } catch (final NoSuchMethodException e) {
                // no-op
            }
            // try field
            try {
                final Field field = key.type.getDeclaredField(key.name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return o -> {
                    try {
                        return field.get(o);
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                };
            } catch (final Exception e) {
                // no-op
            }
            return o -> null;
        }).apply(registry);
    }

    private Object interpolate(final String string, final Object data) {
        final String[] segments = string.split("\\.");
        Object registry = data;
        for (final String it : segments) {
            final Object variable = getVariable(registry, it);
            if (variable == null) {
                return null;
            }
            registry = variable;
        }
        return registry;
    }

    void clean() {
        templates.clear();
    }

    private static class TemplateContext {

        private final Function<String, String> loader;

        private final Object data;

        private TemplateContext(final Function<String, String> loader, final Object data) {
            this.loader = loader;
            this.data = data;
        }
    }

    private static class AccessorKey {

        private final Class<?> type;

        private final String name;

        private final int hash;

        private AccessorKey(final Class<?> type, final String name) {
            this.type = type;
            this.name = name;
            this.hash = Objects.hash(type, name);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || AccessorKey.class != o.getClass()) {
                return false;
            }
            final AccessorKey that = AccessorKey.class.cast(o);
            return Objects.equals(type, that.type) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class TemplateKey {

        private final Function<String, String> loader;

        private final String template;

        private final int hash;

        private TemplateKey(final Function<String, String> loader, final String value) {
            this.loader = loader;
            this.template = value;
            this.hash = Objects.hash(loader, value);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || TemplateKey.class != o.getClass()) {
                return false;
            }
            final TemplateKey that = TemplateKey.class.cast(o);
            return Objects.equals(template, that.template) && Objects.equals(loader, that.loader);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
