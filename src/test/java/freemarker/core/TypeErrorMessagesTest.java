/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import freemarker.test.TemplateTest;

public class TypeErrorMessagesTest extends TemplateTest {

    static final Document doc;
    static {
        try {
            DocumentBuilder docBuilder;
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.parse(new InputSource(new StringReader(
                    "<a><b>123</b><c a='true'>1</c><c a='false'>2</c></a>")));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build data-model", e);
        }
    }

    @Test
    public void testNumericalBinaryOperator() {
        assertErrorContains("${n - s}", "\"-\"", "right-hand", "number", "string");
        assertErrorContains("${s - n}", "\"-\"", "left-hand", "number", "string");
    }

    @Test
    public void testGetterMistake() {
        assertErrorContains("${bean.getX}", "${...}",
                "number", "string", "method", "obj.getSomething", "obj.something");
        assertErrorContains("${1 * bean.getX}", "right-hand",
                "number", "\\!string", "method", "obj.getSomething", "obj.something");
        assertErrorContains("<#if bean.isB></#if>", "condition",
                "boolean", "method", "obj.isSomething", "obj.something");
        assertErrorContains("<#if bean.isB></#if>", "condition",
                "boolean", "method", "obj.isSomething", "obj.something");
        assertErrorContains("${bean.voidM}",
                "string", "method", "\\!()");
        assertErrorContains("${bean.intM}",
                "string", "method", "obj.something()");
        assertErrorContains("${bean.intMP}",
                "string", "method", "obj.something(params)");
    }

    @Test
    public void testXMLTypeMismarches() throws Exception {
        assertErrorContains("${doc.a.c}",
                "used as string", "query result", "2", "multiple matches");
        assertErrorContains("${doc.a.c?boolean}",
                "used as string", "query result", "2", "multiple matches");
        assertErrorContains("${doc.a.d}",
                "used as string", "query result", "0", "no matches");
        assertErrorContains("${doc.a.d?boolean}",
                "used as string", "query result", "0", "no matches");
        
        assertErrorContains("${doc.a.c.@a}",
                "used as string", "query result", "2", "multiple matches");
        assertErrorContains("${doc.a.d.@b}",
                "used as string", "query result", "x", "no matches");
        
        assertErrorContains("${doc.a.b * 2}",
                "used as number", "text", "explicit conversion");
        assertErrorContains("<#if doc.a.b></#if>",
                "used as number", "text", "explicit conversion");

        assertErrorContains("${doc.a.d?nodeName}",
                "used as node", "query result", "0", "no matches");
        assertErrorContains("${doc.a.c?nodeName}",
                "used as node", "query result", "2", "multiple matches");
    }

    @Override
    protected Object createDataModel() {
        Map<String, Object> dataModel = createCommonTestValuesDataModel();
        dataModel.put("doc", doc);
        return dataModel;
    }

}
