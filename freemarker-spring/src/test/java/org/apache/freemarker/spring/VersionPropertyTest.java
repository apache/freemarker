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

import org.apache.freemarker.core.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.Assert.assertEquals;

public class VersionPropertyTest {

    private GenericApplicationContext appContext;

    @Before
    public void setUp() throws Exception {
        appContext = new GenericApplicationContext();
    }

    @After
    public void tearDown() throws Exception {
        if (appContext.isActive()) {
            appContext.stop();
            appContext.close();
        }
    }

    @Test
    public void testVersionPropertySettingByString() throws Exception {
        BeanDefinition beanDef =
                BeanDefinitionBuilder.genericBeanDefinition(VersionHolder.class.getName())
                .addPropertyValue("version", "3.1.4-beta1592")
                .getBeanDefinition();

        appContext.registerBeanDefinition("versionHolder", beanDef);
        appContext.refresh();
        appContext.start();

        VersionHolder versionHolder = appContext.getBean("versionHolder", VersionHolder.class);
        assertEquals(new Version("3.1.4-beta1592"), versionHolder.getVersion());
    }

    public static class VersionHolder {

        private Version version;

        public Version getVersion() {
            return version;
        }

        public void setVersion(Version version) {
            this.version = version;
        }
    }
}
