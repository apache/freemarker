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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;

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
            "${item}<#else>" +
            "Yup.\n" +
            "</#list>\n" +
            "<#list 1..2 as i>${i}</#list>" +
            "<#list 1..3 as j>${j}<#sep>, </#list>" +
            "<#foreach k in 1..2>k=${k}</#foreach>" +
            "<#attempt>succeed<#recover>not visited</#attempt>" +
            "<#attempt>will fail${fail}<#recover>recover</#attempt>" +
            "<@('x'?interpret) />" +
            "<#if true>t<#else>f</#if>" +
            "<#if false>t<#else>f</#if>" +
            "<#if false>t1<#elseif false>f1<#else>f2</#if>" +
            "<#if false>t1<#elseif true>t2<#else>f2</#if>" +
            "<#switch 2>" +
            "<#case 1>C1<#break>" +
            "<#case 2>C2<#break>" +
            "<#case 3>C3<#break>" +
            "<#default>D" +
            "</#switch>" +
            "<#switch 3>" +
            "<#case 1>C1<#break>" +
            "<#case 2>C3<#break>" +
            "<#default>D" +
            "</#switch>" +
            "<#switch 4>" +
            "<#on 1>O1" +
            "<#on 4>O4" +
            "<#default>D" +
            "</#switch>" +
            "<#switch 5>" +
            "<#on 1>O1" +
            "<#default>OD" +
            "</#switch>" +
            "<#macro m>Hello from m!</#macro>" +
            "Calling macro: <@m />" +
            "<#assign t>captured</#assign>" +
            "\n";

    @Test
    public void test() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftl", TEMPLATE_TEXT, cfg);
        TestTemplateProcessingTracer tracer = new TestTemplateProcessingTracer();
        Environment env = t.createProcessingEnvironment(null, NullWriter.INSTANCE);
        env.setTemplateProcessingTracer(tracer);
        env.process();

        System.out.println();
        for (String it : tracer.leafElementSourceSnippets) {
            System.out.println(StringUtil.jQuote(it) + ",");
        }
        System.out.println();
        for (String it : tracer.indentedElementDescriptions) {
            System.out.println("|" + it);
        }
        System.out.println();

        assertEquals(
                List.of(
                        "Yup.\n",
                        "Always.\n",
                        "${item}",
                        "${item}",
                        "${item}",
                        "Yup.\n",
                        "${i}",
                        "${i}",
                        "${j}",
                        ", ",
                        "${j}",
                        ", ",
                        "${j}",
                        "k=",
                        "${k}",
                        "k=",
                        "${k}",
                        "succeed",
                        "will fail",
                        "${fail}",
                        "recover",
                        "<@('x'?interpret) />",
                        "x",
                        "t",
                        "f",
                        "f2",
                        "t2",
                        "C2",
                        "<#break>",
                        "D",
                        "O4",
                        "OD",
                        "Calling macro: ",
                        "<@m />",
                        "Hello from m!",
                        "captured",
                        "\n"
                ),
                tracer.leafElementSourceSnippets);

        assertEquals(
                List.of(
                        "root",
                        " #if 0 == 1",
                        " #if 1 == 1",
                        "  text \"Yup.\\n\"",
                        " text \"Always.\\n\"",
                        " #list-#else-container",
                        "  #list [1, 2, 3] as item",
                        "   ${item}",
                        "   ${item}",
                        "   ${item}",
                        " #list-#else-container",
                        "  #list [] as item",
                        "  #else",
                        "   text \"Yup.\\n\"",
                        " #list 1..2 as i",
                        "  ${i}",
                        "  ${i}",
                        " #list 1..3 as j",
                        "  ${j}",
                        "  #sep",
                        "   text \", \"",
                        "  ${j}",
                        "  #sep",
                        "   text \", \"",
                        "  ${j}",
                        "  #sep",
                        " #foreach k in 1..2",
                        "  text \"k=\"",
                        "  ${k}",
                        "  text \"k=\"",
                        "  ${k}",
                        " #attempt",
                        "  text \"succeed\"",
                        " #attempt",
                        "  #mixed_content",
                        "   text \"will fail\"",
                        "   ${fail}",
                        "  #recover",
                        "   text \"recover\"",
                        " @(\"x\"?interpret)",
                        "  text \"x\"",
                        " #if-#elseif-#else-container",
                        "  #if true",
                        "   text \"t\"",
                        " #if-#elseif-#else-container",
                        "  #else",
                        "   text \"f\"",
                        " #if-#elseif-#else-container",
                        "  #else",
                        "   text \"f2\"",
                        " #if-#elseif-#else-container",
                        "  #elseif true",
                        "   text \"t2\"",
                        " #switch 2",
                        "  #case 2",
                        "   text \"C2\"",
                        "   #break",
                        " #switch 3",
                        "  #default",
                        "   text \"D\"",
                        " #switch 4",
                        "  #on 4",
                        "   text \"O4\"",
                        " #switch 5",
                        "  #default",
                        "   text \"OD\"",
                        " #macro m",
                        " text \"Calling macro: \"",
                        " @m",
                        "  #macro m",
                        "   text \"Hello from m!\"",
                        " #assign t = .nested_output",
                        "  text \"captured\"",
                        " text \"\\n\""
                ),
                tracer.indentedElementDescriptions);
    }

    private static class TestTemplateProcessingTracer implements TemplateProcessingTracer {
        private final List<String> leafElementSourceSnippets = new ArrayList<>();
        private final List<String> indentedElementDescriptions = new ArrayList<>();
        private String indentation = null;

        public void enterElement(Environment env, TracedElement tracedElement) {
            if (indentation == null) {
                indentation = "";
            } else {
                indentation += " ";
            }

            indentedElementDescriptions.add(indentation + tracedElement.getDescription());

            if (tracedElement.isLeaf()) {
                int beginColumn = tracedElement.getBeginColumn();
                int beginLine = tracedElement.getBeginLine();
                int endLine = tracedElement.getEndLine();
                int endColumn = tracedElement.getEndColumn();

                String suffix;
                if (beginLine != endLine) {
                    endLine = beginLine;
                    endColumn = Integer.MAX_VALUE;
                    suffix = "[...]";
                } else {
                    suffix = "";
                }

                String sourceQuotation = tracedElement.getTemplate()
                        .getSource(beginColumn, beginLine, endColumn, endLine);
                leafElementSourceSnippets.add(sourceQuotation + suffix);
            }
        }

        public void exitElement(Environment env) {
            indentation = indentation.isEmpty() ? null : indentation.substring(0, indentation.length() - 1);
        }
    }
}
