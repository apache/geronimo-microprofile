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
package org.apache.geronimo.microprofile.extensions.config.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.StreamSupport;

import org.apache.geronimo.microprofile.extensions.config.docker.DockerConfigConfigSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DockerConfigConfigSourceTest {

    @BeforeEach
    @AfterEach
    void clear() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    @Test
    void testConfigs(@TempDir final Path base) throws IOException {
        Files.createDirectories(base);
        System.setProperty(DockerConfigConfigSource.class.getName() + ".base", base.toAbsolutePath().toString());
        System.setProperty(DockerConfigConfigSource.class.getName() + ".prefixes", "my.");
        try {
            Files
                    .write(base.resolve("my.config.1"), "My first config".getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files
                    .write(base.resolve("my.config.2"), "My second config".getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            final Config config = ConfigProvider.getConfig();
            assertEquals("My first config", config.getValue("my.config.1", String.class));
            assertEquals("My second config", config.getValue("my.config.2", String.class));
            assertEquals(2,
                    StreamSupport
                            .stream(config.getConfigSources().spliterator(), false)
                            .filter(DockerConfigConfigSource.class::isInstance)
                            .findFirst()
                            .orElseThrow(IllegalStateException::new)
                            .getPropertyNames()
                            .size());
        } finally {
            System.clearProperty("geronimo.microprofile.extensions.config.docker.configs.base");
        }
    }
}
