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

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class MapBiTest extends TemplateTest {

    private static class TestParam {
        private final List<?> list;
        private final String result;

        public TestParam(List<?> list, String result) {
            this.list = list;
            this.result = result;
        }
    }

    private static final List<TestParam> TEST_PARAMS = ImmutableList.of(
            new TestParam(ImmutableList.of("a", "b", "c"), "A, B, C"),
            new TestParam(ImmutableList.of("a"), "A"),
            new TestParam(ImmutableList.of(), "")
    );

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();

        cfg.setNumberFormat("0.####");
        cfg.setBooleanFormat("c");

        DefaultObjectWrapper objectWrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);
        objectWrapper.setForceLegacyNonListCollections(false);
        cfg.setObjectWrapper(objectWrapper);

        return cfg;
    }

    @Test
    public void testFilterWithLambda() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            // Lazy:
            assertOutput(
                    "<#list xs?map(it -> it?upperCase) as x>${x}<#sep>, </#list>",
                    testParam.result);
            // Eager:
            assertOutput(
                    "<#assign fxs = xs?map(it -> it?upperCase)>" +
                    "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testFilterWithFunction() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            String functionDef = "<#function toUpper s><#return s?upperCase></#function>";
            // Lazy:
            assertOutput(
                    functionDef +
                    "<#list xs?map(toUpper) as x>${x}<#sep>, </#list>",
                    testParam.result);
            // Eager:
            assertOutput(
                    functionDef +
                    "<#assign fxs = xs?map(toUpper)>" +
                    "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testFilterWithMethod() throws Exception {
        for (TestParam testParam : TEST_PARAMS) {
            addToDataModel("xs", testParam.list);
            addToDataModel("obj", new MapperObject());
            // Lazy:
            assertOutput(
                    "<#list xs?map(obj.toUpper) as x>${x}<#sep>, </#list>",
                    testParam.result);
            // Eager:
            assertOutput(
                    "<#assign fxs = xs?map(obj.toUpper)>" +
                    "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testWithNumberElements() throws Exception {
        addToDataModel("xs", ImmutableList.of(1, 1.55, 3));
        addToDataModel("obj", new MapperObject());
        assertOutput(
                "<#list xs?map(n -> n * 10) as x>${x}<#sep>, </#list>",
                "10, 15.5, 30");
        assertOutput(
                "<#function tenTimes n><#return n * 10></#function>" +
                "<#list xs?map(tenTimes) as x>${x}<#sep>, </#list>",
                "10, 15.5, 30");
        assertOutput(
                "<#list xs?map(obj.tenTimes) as x>${x}<#sep>, </#list>",
                "10, 15.5, 30");
    }

    @Test
    public void testWithBeanElements() throws Exception {
        addToDataModel("xs", ImmutableList.of(new User("a"), new User("b"), new User("c")));
        addToDataModel("obj", new MapperObject());
        assertOutput(
                "<#list xs?map(user -> user.name) as x>${x}<#sep>, </#list>",
                "a, b, c");
        assertOutput(
                "<#function extractName user><#return user.name></#function>" +
                        "<#list xs?map(extractName) as x>${x}<#sep>, </#list>",
                "a, b, c");
        assertOutput(
                "<#list xs?map(obj.extractName) as x>${x}<#sep>, </#list>",
                "a, b, c");
    }

    @Test
    public void testBuiltInsThatAllowLazyEval() throws Exception {
        assertOutput("" +
                "<#assign s = ''>" +
                "<#function tenTimes(x)><#assign s += '${x};'><#return x * 10></#function>" +
                "${(1..3)?map(tenTimes)?first} ${s}", "10 1;");

        assertOutput("" +
                "<#assign s = ''>" +
                "<#function tenTimes(x)><#assign s += '${x};'><#return x * 10></#function>" +
                "${(1..3)?map(tenTimes)?seqContains(20)} ${s}", "true 1;2;");

        assertOutput("" +
                "<#assign s = ''>" +
                "<#function tenTimes(x)><#assign s += '${x};'><#return x * 10></#function>" +
                "${(1..3)?map(tenTimes)?seqIndexOf(20)} ${s}", "1 1;2;");

        assertOutput("" +
                "<#assign s = ''>" +
                "<#function tenTimes(x)><#assign s += '${x};'><#return x * 10></#function>" +
                "${[1, 2, 3, 2, 5]?map(tenTimes)?seqLastIndexOf(20)} ${s}", "3 1;2;3;2;5;");

        // For these this test can't check that there was no sequence built, but at least we know they are working:
        assertOutput("${(1..3)?map(it -> it * 10)?min}", "10");
        assertOutput("${(1..3)?map(it -> it * 10)?max}", "30");
        assertOutput("${(1..3)?map(it -> it * 10)?join(', ')}", "10, 20, 30");
    }

    @Test
    public void testErrorMessages() {
        assertErrorContains("${1?map(it -> it)}", TemplateException.class,
                "sequence or collection", "number");
        assertErrorContains("${[]?map(1)}", TemplateException.class,
                "method or function or lambda", "number");
        assertErrorContains("<#function f></#function>${['x']?map(f)}", TemplateException.class,
                "Function", "0 parameters", "1");
        assertErrorContains("<#function f x y z></#function>${['x']?map(f)}", TemplateException.class,
                "function", "parameter \"y\"");
        assertErrorContains("<#function f x></#function>${['x']?map(f)}", TemplateException.class,
                "null");
        assertErrorContains("${[]?map(() -> 1)}", ParseException.class,
                "lambda", "1 parameter", "declared 0");
        assertErrorContains("${[]?map((i, j) -> 1)}", ParseException.class,
                "lambda", "1 parameter", "declared 2");
    }

    @Test
    public void testNonSequenceInput() throws Exception {
        addToDataModel("coll", ImmutableSet.of("a", "b", "c"));
        assertErrorContains("${coll?map(it -> it?upperCase)[0]}", "sequence", "evaluated to an extended_collection");
        assertErrorContains("[#ftl][#assign t = coll?map(it -> it?upperCase)]",
                "lazy transformation", "?sequence", "[#list");
        assertOutput("${coll?sequence?map(it -> it?upperCase)[0]}", "A");
        assertOutput("${coll?map(it -> it?upperCase)?sequence[0]}", "A");
        assertOutput("<#list coll?map(it -> it?upperCase) as it>${it}</#list>", "ABC");
    }

    public static class MapperObject {
        public String toUpper(String s) {
            return s.toUpperCase();
        }
        public BigDecimal tenTimes(BigDecimal n) {
            return n.movePointRight(1);
        }
        public String extractName(User user) { return user.getName(); }
    }

    public static class User {
        private final String name;

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
