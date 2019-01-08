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

import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.geronimo.microprofile.reporter.storage.templating.TemplatingEngine;

@Provider
@Dependent
@Produces(TEXT_HTML)
public class HtmlWriter implements MessageBodyWriter<Html> {
    private final boolean development = Boolean.getBoolean("geronimo.microprofile.reporter.dev");

    @Inject
    private TemplatingEngine templatingEngine;

    private final ConcurrentMap<String, String> templates = new ConcurrentHashMap<>();

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return type == Html.class;
    }

    @Override
    public void writeTo(final Html html, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
            throws IOException, WebApplicationException {
        final String template = templates.computeIfAbsent(html.getName(), this::loadTemplate);
        final Function<Object, String> compiled = templatingEngine.compileIfNeeded(template, this::loadTemplate);
        entityStream.write(compiled.apply(html.getData()).getBytes(StandardCharsets.UTF_8));
        if (development) {
            templates.clear();
            templatingEngine.clean();
        }
    }

    private String loadTemplate(final String template) {
        return Stream.of("geronimo/microprofile/reporter/" + template, template)
                .flatMap(it -> Stream.of(it, '/' + it))
                .map(it -> {
                    try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(it)) {
                        if (stream == null) {
                            return null;
                        }
                        return new BufferedReader(new InputStreamReader(stream)).lines().collect(joining("\n"));
                    } catch (final IOException e) {
                        throw new InternalServerErrorException(e);
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException("Missing template: " + template));
    }
}
