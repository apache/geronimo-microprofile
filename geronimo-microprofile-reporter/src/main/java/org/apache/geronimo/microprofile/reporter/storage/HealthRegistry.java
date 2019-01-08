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

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public class HealthRegistry implements Extension {
    private static final Annotation[] NO_ANNOTATION = new Annotation[0];

    private final Collection<Bean<?>> beans = new ArrayList<>();
    private final Collection<CreationalContext<?>> contexts = new ArrayList<>();
    private final List<HealthCheck> checks = new ArrayList<>();

    public Stream<HealthCheckResponse> doCheck() {
        return checks.stream().map(check -> invoke(check));
    }

    private HealthCheckResponse invoke(final HealthCheck check) {
        try {
            return check.call();
        } catch (final RuntimeException re) {
            return HealthCheckResponse.named(check.getClass().getName())
                                      .down()
                                      .withData("exceptionMessage", re.getMessage())
                                      .build();
        }
    }

    void findChecks(@Observes final ProcessBean<?> bean) {
        if (bean.getAnnotated().isAnnotationPresent(Health.class) && bean.getBean().getTypes().contains(HealthCheck.class)) {
            beans.add(bean.getBean());
        }
    }

    void start(@Observes final AfterDeploymentValidation afterDeploymentValidation, final BeanManager beanManager) {
        checks.addAll(beans.stream().map(it -> lookup(it, beanManager)).collect(toList()));
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

    private HealthCheck lookup(final Bean<?> bean, final BeanManager manager) {
        final Class<?> beanClass = bean.getBeanClass();
        final Bean<?> resolvedBean = manager.resolve(manager.getBeans(
                beanClass != null ? beanClass : HealthCheck.class, bean.getQualifiers().toArray(NO_ANNOTATION)));
        final CreationalContext<Object> creationalContext = manager.createCreationalContext(null);
        if (!manager.isNormalScope(resolvedBean.getScope())) {
            contexts.add(creationalContext);
        }
        return HealthCheck.class.cast(manager.getReference(resolvedBean, HealthCheck.class, creationalContext));
    }
}
