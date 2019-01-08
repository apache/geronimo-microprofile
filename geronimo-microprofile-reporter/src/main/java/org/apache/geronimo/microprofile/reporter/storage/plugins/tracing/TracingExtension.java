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
package org.apache.geronimo.microprofile.reporter.storage.plugins.tracing;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.apache.geronimo.microprofile.reporter.storage.data.MicroprofileDatabase;

public class TracingExtension implements Extension {
    private boolean active;
    private Class<?> finishedSpan;
    private TracingService service;

    public boolean isActive() {
        return active;
    }

    void onStart(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            loader.loadClass("io.opentracing.Span");
            finishedSpan = loader.loadClass("org.apache.geronimo.microprofile.opentracing.common.impl.FinishedSpan");
            active = true;
        } catch (final ClassNotFoundException e) {
            // no-op
        }
    }

    void addObserver(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        if (active) {
            // like that to avoid to require tracing in the app
            afterBeanDiscovery.addObserverMethod()
                              .observedType(finishedSpan)
                              .notifyWith(e -> service.onSpan(e.getEvent()));
        }
    }

    void createService(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager manager) {
        if (active) {
            service = new TracingService(lookup(manager, MicroprofileDatabase.class), lookup(manager, SpanMapper.class));
        }
    }

    // only lookup normal bean so ignore cc
    private <T> T lookup(final BeanManager beanManager, final Class<T> type) {
        final Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(null)));
    }
}
