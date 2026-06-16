/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package freemarker.core;

import static freemarker.core.Configurable.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class DistinctBiTest extends TemplateTest {

    private static final List<TestParam> TEST_DISTINCT_PARAMS = ImmutableList.of(
            new TestParam(ImmutableList.of("a", "b", "c", "a", "c", "d"), "a, b, c, d"),
            new TestParam(ImmutableList.of(1, 2, 3, 1, 5), "1, 2, 3, 5"),
            new TestParam(ImmutableList.of(new Timestamp(0), new Timestamp(1000), new Timestamp(123000),
                    new Timestamp(5000),
                    new Timestamp(123000)),
                    "1970-01-01 01:00:00, 1970-01-01 01:00:01, 1970-01-01 01:02:03, 1970-01-01 01:00:05"),
            new TestParam(ImmutableList.of(), ""),
            new TestParam(ImmutableList.of("x"), "x")
    );

    private static final List<TestParam> TEST_DISTINCT_BY_PARAMS;

    static {
        ImmutableList<TestBean> beans = ImmutableList.of(
                new TestBean(1, "name1", 13, false, new Timestamp(1000)),
                new TestBean(2, "name2", 10, false, new Timestamp(2000)),
                new TestBean(3, "name2", 10, false, new Timestamp(3000)),
                new TestBean(4, "name2", 25, true, new Timestamp(2000))
        );
        TEST_DISTINCT_BY_PARAMS = ImmutableList.of(
                new TestParam(beans, "name", "1, 2", "4, 1"),
                new TestParam(beans, "age", "1, 2, 4", "4, 3, 1"),
                new TestParam(beans, "adult", "1, 4", "4, 3"),
                new TestParam(beans, "birthday", "1, 2, 3", "4, 3, 1"))
        ;
    }

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration configuration = super.createConfiguration();
        configuration.setDateTimeFormat("YYYY-MM-dd HH:mm:ss");
        configuration.setBooleanFormat(BOOLEAN_FORMAT_LEGACY_DEFAULT);
        return configuration;
    }

    @Test
    public void testDistinct() throws TemplateException, IOException {
        for (TestParam testParam : TEST_DISTINCT_PARAMS) {
            addToDataModel("xs", testParam.list);
            assertOutput(
                    "<#list xs?distinct as x>${x}<#sep>, </#list>",
                    testParam.result);
            assertOutput(
                    "<#assign fxs = xs?distinct>" +
                            "${fxs?join(', ')}",
                    testParam.result);
        }
    }

    @Test
    public void testDistinctBy() throws TemplateException, IOException {
        for (TestParam testParam : TEST_DISTINCT_BY_PARAMS) {
            addToDataModel("xs", testParam.list);
            assertOutput(
                    "<#list xs?distinct_by('" + testParam.field + "') as x>${x.id}<#sep>, </#list>",
                    testParam.result);
            assertOutput(
                    "<#assign fxs = xs?distinct_by('" + testParam.field + "')>" +
                            "${fxs?map(i -> i.id)?join(', ')}",
                    testParam.result);

        }
    }

    @Test
    public void testDistinctByReverse() throws TemplateException, IOException {
        for (TestParam testParam : TEST_DISTINCT_BY_PARAMS) {
            addToDataModel("xs", testParam.list);
            assertOutput(
                    "<#list xs?reverse?distinct_by('" + testParam.field + "') as x>${x.id}<#sep>, </#list>",
                    testParam.reverseResult);
            assertOutput(
                    "<#assign fxs = xs?reverse?distinct_by('" + testParam.field + "')>" +
                            "${fxs?map(i -> i.id)?join(', ')}",
                    testParam.reverseResult);

        }
    }

    private static class TestParam {
        private final List<?> list;

        private String field;

        private final String result;

        private String reverseResult;

        public TestParam(List<?> list, String result) {
            this.list = list;
            this.result = result;
        }

        public TestParam(List<?> list, String field, String result, String reverseResult) {
            this.list = list;
            this.field = field;
            this.result = result;
            this.reverseResult = reverseResult;
        }
    }

    public static class TestBean {

        private final int id;

        private final String name;

        private final int age;

        private final boolean adult;

        private final Timestamp birthday;

        public TestBean(int id, String name, int age, boolean adult, Timestamp birthday) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.adult = adult;
            this.birthday = birthday;
        }


        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public boolean isAdult() {
            return adult;
        }

        public Timestamp getBirthday() {
            return birthday;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestBean
                    && name.equals(((TestBean) obj).name)
                    && age == ((TestBean) obj).age
                    && adult == ((TestBean) obj).adult
                    && birthday.equals(((TestBean) obj).birthday);
        }

        @Override
        public String toString() {
            return id + "";
        }
    }

}
