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

package org.apache.freemarker.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.model.impl.SimpleSequence;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class NewBiObjectWrapperRestrictionTest extends TemplateTest {

    @Override
    protected Configuration createDefaultConfiguration() throws Exception {
        return new TestConfigurationBuilder().objectWrapper(new EntirelyCustomObjectWrapper()).build();
    }

    @Test
    public void testPositive() throws IOException, TemplateException {
        assertOutput(
                "${'org.apache.freemarker.test.templatesuite.models.NewTestModel'?new()}",
                "default constructor");
    }

    @Test
    public void testNegative() {
        assertErrorContains(
                "${'org.apache.freemarker.test.templatesuite.models.NewTestModel'?new('s')}",
                "only supports 0 argument");
    }

    /**
     * An object wrapper that doesn't extend {@link DefaultObjectWrapper}.
     */
    public static class EntirelyCustomObjectWrapper implements ObjectWrapper {

        @Override
        public TemplateModel wrap(Object obj) throws TemplateModelException {
            if (obj == null) {
                return null;
            }

            if (obj instanceof TemplateModel) {
                return (TemplateModel) obj;
            }
            if (obj instanceof TemplateModelAdapter) {
                return ((TemplateModelAdapter) obj).getTemplateModel();
            }

            if (obj instanceof String) {
                return new SimpleScalar((String) obj);
            }
            if (obj instanceof Number) {
                return new SimpleNumber((Number) obj);
            }
            if (obj instanceof Boolean) {
                return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
            if (obj instanceof java.util.Date) {
                if (obj instanceof java.sql.Date) {
                    return new SimpleDate((java.sql.Date) obj);
                }
                if (obj instanceof java.sql.Time) {
                    return new SimpleDate((java.sql.Time) obj);
                }
                if (obj instanceof java.sql.Timestamp) {
                    return new SimpleDate((java.sql.Timestamp) obj);
                }
                return new SimpleDate((java.util.Date) obj, TemplateDateModel.UNKNOWN);
            }

            if (obj.getClass().isArray()) {
                obj = Arrays.asList((Object[]) obj);
            }
            if (obj instanceof Collection) {
                return new SimpleSequence((Collection<?>) obj, this);
            }
            if (obj instanceof Map) {
                return new SimpleHash((Map<?, ?>) obj, this);
            }

            return null;
        }
    }
}
