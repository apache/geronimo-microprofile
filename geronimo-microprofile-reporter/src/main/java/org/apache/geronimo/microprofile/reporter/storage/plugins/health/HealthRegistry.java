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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

public class HealthRegistry implements Extension {
    private static final Annotation[] NO_ANNOTATION = new Annotation[0];

    private final Collection<Bean<?>> beans = new ArrayList<>();
    private final Collection<CreationalContext<?>> contexts = new ArrayList<>();

    private Class<? extends Annotation> annotationMarker;
    private Class<?> apiType;

    void onStart(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            annotationMarker = (Class<? extends Annotation>) loader.loadClass("org.eclipse.microprofile.health.Health");
            apiType = loader.loadClass("org.eclipse.microprofile.health.HealthCheck");
        } catch (final ClassNotFoundException e) {
            // no-op
        }
    }

    public Class<?> getApiType() {
        return apiType;
    }

    void findChecks(@Observes final ProcessBean<?> bean) {
        if (bean.getAnnotated().isAnnotationPresent(annotationMarker) && bean.getBean().getTypes().contains(apiType)) {
            beans.add(bean.getBean());
        }
    }

    void start(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager beanManager) {
        if (beans.isEmpty()) {
            return;
        }
        final HealthService healthService = HealthService.class.cast(
                beanManager.getReference(beanManager.resolve(beanManager.getBeans(HealthService.class)),
                        HealthService.class, beanManager.createCreationalContext(null)));
        beans.stream().map(it -> lookup(it, beanManager)).forEach(healthService::register);
    }

    void stop(@Observes final BeforeShutdown beforeShutdown) {
        final IllegalStateException ise = new IllegalStateException("Something went wrong releasing health checks");
        contexts.forEach(c -> {
            try {
                c.release();
            } catch (final RuntimeException re) {
                ise.addSuppressed(re);
            }
        });
        if (ise.getSuppressed().length > 0) {
            throw ise;
        }
    }

    private Object lookup(final Bean<?> bean, final BeanManager manager) {
        final Class<?> beanClass = bean.getBeanClass();
        final Bean<?> resolvedBean = manager.resolve(manager.getBeans(
                beanClass != null ? beanClass : apiType, bean.getQualifiers().toArray(NO_ANNOTATION)));
        final CreationalContext<Object> creationalContext = manager.createCreationalContext(null);
        if (!manager.isNormalScope(resolvedBean.getScope())) {
            contexts.add(creationalContext);
        }
        return manager.getReference(resolvedBean, apiType, creationalContext);
    }
}
