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

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class TemplateProcessingTracerTest {

    private static final String TEMPLATE_TEXT =
            "<#if 0 == 1>Nope.\n</#if>" +
            "<#if 1 == 1>Yup.\n</#if>" +
            "Always.\n" +
            "<#list [1, 2, 3] as item>\n" +
            "${item}<#else>\n" +
            "Nope.\n" +
            "</#list>\n" +
            "<#list [] as item>\n" +
            "${item}<#else>\n" +
            "Yup.\n" +
            "</#list>\n";

    @Test
    public void test() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftl", TEMPLATE_TEXT, cfg);
        StringWriter sw = new StringWriter();
        Tracer tracer = new Tracer();
        Environment env = t.createProcessingEnvironment(null, sw);
        env.setTracer(tracer);
        env.process();

        List<Integer> expected = Arrays.asList(2, 3, 5, 5, 5, 9);
        assertEquals(expected, tracer.linesVisited);
    }

    private static class Tracer implements TemplateProcessingTracer {
        ArrayList<Integer> linesVisited;

        Tracer() {
            linesVisited = new ArrayList<>();
        }

        public void start() {}
        public void end() {}

        public void trace(Template template, int beginColumn, int beginLine, int endColumn, int endLine,
            boolean isLeafElement) {
            if (isLeafElement) {
                linesVisited.add(beginLine);
            }
        }

    }
}
