/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.freemarker.spring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Configuration factory bean to support Spring Framework applications.
 */
public class ConfigurationFactoryBean extends ExtendableBuilder<ConfigurationFactoryBean>
        implements FactoryBean<Configuration>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

    private AbstractFactoryBean<Configuration> delegate;

    private Map<String, String> settings = new LinkedHashMap<>();

    public ConfigurationFactoryBean() {
        // By default, set the default version constant.
        // #setIncompatibleImprovements(Version) can be used to change it.
        super(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

        delegate = new AbstractFactoryBean<Configuration>() {

            @Override
            public Class<?> getObjectType() {
                return Configuration.class;
            }

            @Override
            protected Configuration createInstance() throws Exception {
                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    setSetting(entry.getKey(), entry.getValue());
                }

                return build();
            }

        };
    }

    public Map<String, String> getSettings() {
        return Collections.unmodifiableMap(settings);
    }

    public void setSettings(Map<String, String> settings) {
        this.settings.putAll(settings);
    }

    @Override
    public Configuration getObject() throws Exception {
        return delegate.getObject();
    }

    @Override
    public Class<?> getObjectType() {
        return delegate.getObjectType();
    }

    public void setSingleton(boolean singleton) {
        delegate.setSingleton(singleton);
    }

    @Override
    public boolean isSingleton() {
        return delegate.isSingleton();
    }

    @Override
    public void destroy() throws Exception {
        delegate.destroy();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        delegate.afterPropertiesSet();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        delegate.setBeanFactory(beanFactory);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        delegate.setBeanClassLoader(classLoader);
    }

}
