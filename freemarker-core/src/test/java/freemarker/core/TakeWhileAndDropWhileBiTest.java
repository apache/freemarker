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

package freemarker.core;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.test.TemplateTest;

public class TakeWhileAndDropWhileBiTest extends TemplateTest {

    private static class TestParam {
        private final List<?> list;
        private final String takeWhileResult;
        private final String dropWhileResult;

        public TestParam(List<?> list, String takeWhileResult, String dropWhileResult) {
            this.list = list;
            this.takeWhileResult = takeWhileResult;
            this.dropWhileResult = dropWhileResult;
        }
    }

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();

        DefaultObjectWrapper objectWrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        objectWrapper.setForceLegacyNonListCollections(false);
        cfg.setObjectWrapper(objectWrapper);

        return cfg;
    }

    private static final List<TestParam> TEST_PARAMS = ImmutableList.of(
            new TestParam(ImmutableList.of(),
                    "",
                    ""),
            new TestParam(ImmutableList.of("a"),
                    "a",
                    "a"),
            new TestParam(ImmutableList.of("a", "b", "c"),
                    "a, b, c",
                    "a, b, c"),
            new TestParam(ImmutableList.of("aX"),
                    "",
                    ""),
            new TestParam(ImmutableList.of("aX", "b"),
                    "",
                    "b"),
            new TestParam(ImmutableList.of("aX", "b", "c"),
                    "",
                    "b, c"),
            new TestParam(ImmutableList.of("a", "bX", "c"),
                    "a",
                    "a, bX, c"),
            new TestParam(ImmutableList.of("a", "b", "cX"),
                    "a, b",
                    "a, b, cX"),
            new TestParam(ImmutableList.of("aX", "bX", "c"),
                    "",
                    "c"),
            new TestParam(ImmutableList.of("aX", "bX", "cX"),
                    "",
                    ""),
            new TestParam(ImmutableList.of("aX", "b", "cX"),
                    "",
                    "b, cX")
    );

    @Test
    public void testTakeWhile() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            assertOutput(
                    "<#list xs?takeWhile(it -> !it?contains('X')) as x>${x}<#sep>, </#list>",
                    testParam.takeWhileResult);
            assertOutput(
                    "<#assign fxs = xs?takeWhile(it -> !it?contains('X'))>" +
                            "${fxs?join(', ')}",
                    testParam.takeWhileResult);
        }
    }

    @Test
    public void testDropWhile() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            assertOutput(
                    "<#list xs?dropWhile(it -> it?contains('X')) as x>${x}<#sep>, </#list>",
                    testParam.dropWhileResult);
            assertOutput(
                    "<#assign fxs = xs?dropWhile(it -> it?contains('X'))>" +
                            "${fxs?join(', ')}",
                    testParam.dropWhileResult);
        }
    }

    // Chaining the two built-ins is not a special case, but, in the hope of running into some bugs, we test that too.
    @Test
    public void testBetween() throws Exception {
        String ftl = "<#list xs?dropWhile(it -> it < 0)?takeWhile(it -> it >= 0) as x>${x}<#sep>, </#list>";

        addToDataModel("xs", ImmutableList.of(-1, -2, 3, 4, -5, -6));
        assertOutput(ftl,  "3, 4");

        addToDataModel("xs", ImmutableList.of(-1, -2, -5, -6));
        assertOutput(ftl,  "");

        addToDataModel("xs", ImmutableList.of(1, 2, 3));
        assertOutput(ftl,  "1, 2, 3");

        addToDataModel("xs", Collections.emptyList());
        assertOutput(ftl,  "");
    }

    @Test
    public void testSnakeCaseNames() throws Exception {
        addToDataModel("xs", ImmutableList.of(-1, -2, 3, 4, -5, -6));
        assertOutput(
                "<#list xs?drop_while(it -> it < 0)?take_while(it -> it >= 0) as x>${x}<#sep>, </#list>",
                "3, 4");
    }

}