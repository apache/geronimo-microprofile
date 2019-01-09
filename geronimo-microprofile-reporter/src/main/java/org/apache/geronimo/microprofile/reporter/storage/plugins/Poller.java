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
package org.apache.geronimo.microprofile.reporter.storage.plugins;

import static java.lang.Thread.NORM_PRIORITY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Poller {
    private static final Tick TICK = new Tick();

    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> pollFuture;

    @Inject
    private Event<Tick> tickEvent;

    void onStart(@Observes @Initialized(ApplicationScoped.class) final Object start,
                 @ConfigProperty(name = "geronimo.microprofile.reporter.polling.interval", defaultValue = "5000") final Long pollingInterval) {
        if (pollingInterval <= 0) {
            return;
        }

        final ClassLoader appLoader = Thread.currentThread().getContextClassLoader();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, "geronimo-microprofile-reporter-poller-" + name(start));
            thread.setContextClassLoader(appLoader);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != NORM_PRIORITY) {
                thread.setPriority(NORM_PRIORITY);
            }
            return thread;
        });
        pollFuture = scheduler.scheduleAtFixedRate(() -> tickEvent.fire(TICK), pollingInterval, pollingInterval, MILLISECONDS);
    }

    void onStop(@Observes @Destroyed(ApplicationScoped.class) final Object stop) {
        if (pollFuture != null) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(10, SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
    }

    private String name(final Object start) {
        if (ServletContext.class.isInstance(start)) {
            final ServletContext context = ServletContext.class.cast(start);
            try {
                return "[web=" + context.getVirtualServerName() + '/' + context.getContextPath() + "]";
            } catch (final Error | Exception e) { // no getVirtualServerName() for this context
                return "[web=" + context.getContextPath() + "]";
            }
        }
        return start.toString();
    }
}
