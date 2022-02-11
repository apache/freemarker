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

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;
import freemarker.test.TemplateTest;

public class FilterBiTest extends TemplateTest {

    private static class TestParam {
        private final List<?> list;
        private final String result;

        public TestParam(List<?> list, String result) {
            this.list = list;
            this.result = result;
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
            new TestParam(ImmutableList.of("a", "aX", "bX", "b", "cX", "c"), "a, b, c"),
            new TestParam(ImmutableList.of("a", "b", "c"), "a, b, c"),
            new TestParam(ImmutableList.of("aX", "bX", "a", "b", "c", "cX", "cX"), "a, b, c"),
            new TestParam(ImmutableList.of("aX", "bX", "cX"), ""),
            new TestParam(ImmutableList.of(), "")
    );

    @Test
    public void testFilterWithLambda() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            assertOutput(
                    "<#list xs?filter(it -> !it?contains('X')) as x>${x}<#sep>, </#list>",
                    testParam.result);
            assertOutput(
                    "<#assign fxs = xs?filter(it -> !it?contains('X'))>" +
                    "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testFilterWithFunction() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            String functionDef = "<#function noX s><#return !s?contains('X')></#function>";
            assertOutput(
                    functionDef +
                    "<#list xs?filter(noX) as x>${x}<#sep>, </#list>",
                    testParam.result);
            assertOutput(
                    functionDef +
                    "<#assign fxs = xs?filter(noX)>" +
                    "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testFilterWithMethod() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            addToDataModel("obj", new FilterObject());
            assertOutput(
                    "<#list xs?filter(obj.noX) as x>${x}<#sep>, </#list>",
                    testParam.result);
            assertOutput(
                    "<#assign fxs = xs?filter(obj.noX)>" +
                    "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testWithNumberElements() throws Exception {
        addToDataModel("xs", ImmutableList.of(1, 1.5, 2, 2.3, 3));
        addToDataModel("obj", new FilterObject());
        assertOutput(
                "<#list xs?filter(n -> n == n?int) as x>${x}<#sep>, </#list>",
                "1, 2, 3");
        assertOutput(
                "<#function isInteger n><#return n == n?int></#function>" +
                "<#list xs?filter(isInteger) as x>${x}<#sep>, </#list>",
                "1, 2, 3");
        assertOutput(
                "<#list xs?filter(obj.isInteger) as x>${x}<#sep>, </#list>",
                "1, 2, 3");
    }

    @Test
    public void testErrorMessages() {
        assertErrorContains("${1?filter(it -> true)}", TemplateException.class,
                "sequence or collection", "number");
        assertErrorContains("${[]?filter(1)}", TemplateException.class,
                "method or function or lambda", "number");
        assertErrorContains("${['x']?filter(it -> 1)}", TemplateException.class,
                "boolean", "number");
        assertErrorContains("<#function f></#function>${['x']?filter(f)}", TemplateException.class,
                "Function", "0 parameters", "1");
        assertErrorContains("<#function f x y z></#function>${['x']?filter(f)}", TemplateException.class,
                "function", "parameter \"y\"");
        assertErrorContains("<#function f x></#function>${['x']?filter(f)}", TemplateException.class,
                "boolean", "null");
        assertErrorContains("${[]?filter(() -> true)}", ParseException.class,
                "lambda", "1 parameter", "declared 0");
        assertErrorContains("${[]?filter((i, j) -> true)}", ParseException.class,
                "lambda", "1 parameter", "declared 2");
    }

    @Test
    public void testSequenceAndCollectionTarget() throws Exception {
        addToDataModel("xs", new SequenceAndCollection());
        assertOutput("${xs?filter(x -> x != 'a')?join(', ')}", "b");
        assertOutput("<#assign xs2 = xs?filter(x -> x != 'a')>${xs2?join(', ')}", "b");
    }

    @Test
    public void testNonSequenceInput() throws Exception {
        addToDataModel("coll", ImmutableSet.of("a", "b", "c"));
        assertErrorContains("${coll?filter(it -> it != 'a')[0]}", "sequence", "evaluated to a collection");
        assertErrorContains("[#ftl][#assign t = coll?filter(it -> it != 'a')]",
                "lazy transformation", "?sequence", "[#list");
        assertOutput("${coll?sequence?filter(it -> it != 'a')[0]}", "b");
        assertOutput("${coll?filter(it -> it != 'a')?sequence[0]}", "b");
        assertOutput("<#list coll?filter(it -> it != 'a') as it>${it}</#list>", "bc");
    }

    public static class FilterObject {
        public boolean noX(String s) {
            return !s.contains("X");
        }
        public boolean isInteger(double n) {
            return n == (int) n;
        }
    }

    public class SequenceAndCollection implements TemplateSequenceModel, TemplateCollectionModel {
        public TemplateModelIterator iterator() throws TemplateModelException {
            return new SequenceIterator(this);
        }

        public TemplateModel get(int index) throws TemplateModelException {
            switch (index) {
                case 0: return new SimpleScalar("a");
                case 1: return new SimpleScalar("b");
                default: return null;
            }
        }

        public int size() throws TemplateModelException {
            return 2;
        }
    }

}
