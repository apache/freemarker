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
import java.util.Collections;
import java.util.Date;

import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.userpkg.HTMLISOTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.PrintfGTemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("boxing")
public class CoercionToTextualTest extends TemplateTest {
    
    /** 2015-09-06T12:00:00Z */
    private static long T = 1441540800000L;
    private static TemplateDateModel TM = new SimpleDate(new Date(T), TemplateDateModel.DATE_TIME);
    
    @Test
    public void testBasicStringBuiltins() throws IOException, TemplateException {
        assertOutput("${s?upperCase}", "ABC");
        assertOutput("${n?string?lowerCase}", "1.50e+03");
        assertErrorContains("${n?lowerCase}", "convert", "string", "markup", "text/html");
        assertOutput("${dt?string?lowerCase}", "2015-09-06t12:00:00z");
        assertErrorContains("${dt?lowerCase}", "convert", "string", "markup", "text/html");
        assertOutput("${b?upperCase}", "Y");
        assertErrorContains("${m?upperCase}", "convertible to string", "HTMLOutputModel");
    }

    @Test
    public void testEscBuiltin() throws IOException, TemplateException {
        setConfiguration(createDefaultConfigurationBuilder()
                .outputFormat(HTMLOutputFormat.INSTANCE)
                .autoEscapingPolicy(AutoEscapingPolicy.DISABLE)
                .booleanFormat("<y>,<n>")
                .build());
        assertOutput("${'a<b'?esc}", "a&lt;b");
        assertOutput("${n?string?esc}", "1.50E+03");
        assertOutput("${n?esc}", "1.50*10<sup>3</sup>");
        assertOutput("${dt?string?esc}", "2015-09-06T12:00:00Z");
        assertOutput("${dt?esc}", "2015-09-06<span class='T'>T</span>12:00:00Z");
        assertOutput("${b?esc}", "&lt;y&gt;");
        assertOutput("${m?esc}", "<p>M</p>");
    }
    
    @Test
    public void testStringOverloadedBuiltIns() throws IOException, TemplateException {
        assertOutput("${s?contains('b')}", "y");
        assertOutput("${n?string?contains('E')}", "y");
        assertErrorContains("${n?contains('E')}", "convert", "string", "markup", "text/html");
        assertErrorContains("${n?indexOf('E')}", "convert", "string", "markup", "text/html");
        assertOutput("${dt?string?contains('0')}", "y");
        assertErrorContains("${dt?contains('0')}", "convert", "string", "markup", "text/html");
        assertErrorContains("${m?contains('0')}", "convertible to string", "HTMLOutputModel");
        assertErrorContains("${m?indexOf('0')}", "convertible to string", "HTMLOutputModel");
    }
    
    @Test
    public void testMarkupStringBuiltIns() throws IOException, TemplateException {
        assertErrorContains("${n?string?markupString}", "Expected", "markup", "string");
        assertErrorContains("${n?markupString}", "Expected", "markup", "number");
        assertErrorContains("${dt?markupString}", "Expected", "markup", "date");
    }
    
    @Test
    public void testSimpleInterpolation() throws IOException, TemplateException {
        assertOutput("${s}", "abc");
        assertOutput("${n?string}", "1.50E+03");
        assertOutput("${n}", "1.50*10<sup>3</sup>");
        assertOutput("${dt?string}", "2015-09-06T12:00:00Z");
        assertOutput("${dt}", "2015-09-06<span class='T'>T</span>12:00:00Z");
        assertOutput("${b}", "y");
        assertOutput("${m}", "<p>M</p>");
    }
    
    @Test
    public void testConcatenation() throws IOException, TemplateException {
        assertOutput("${s + '&'}", "abc&");
        assertOutput("${n?string + '&'}", "1.50E+03&");
        assertOutput("${n + '&'}", "1.50*10<sup>3</sup>&amp;");
        assertOutput("${dt?string + '&'}", "2015-09-06T12:00:00Z&");
        assertOutput("${dt + '&'}", "2015-09-06<span class='T'>T</span>12:00:00Z&amp;");
        assertOutput("${b + '&'}", "y&");
        assertOutput("${m + '&'}", "<p>M</p>&amp;");
    }

    @Test
    public void testConcatenation2() throws IOException, TemplateException {
        assertOutput("${'&' + s}", "&abc");
        assertOutput("${'&' + n?string}", "&1.50E+03");
        assertOutput("${'&' + n}", "&amp;1.50*10<sup>3</sup>");
        assertOutput("${'&' + dt?string}", "&2015-09-06T12:00:00Z");
        assertOutput("${'&' + dt}", "&amp;2015-09-06<span class='T'>T</span>12:00:00Z");
        assertOutput("${'&' + b}", "&y");
        assertOutput("${'&' + m}", "&amp;<p>M</p>");
    }

    @Override
    protected Configuration createDefaultConfiguration() throws Exception {
        return createDefaultConfigurationBuilder().build();
    }

    private TestConfigurationBuilder createDefaultConfigurationBuilder() {
        return new TestConfigurationBuilder()
                .customNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                        "G", PrintfGTemplateNumberFormatFactory.INSTANCE))
                .customDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                        "HI", HTMLISOTemplateDateFormatFactory.INSTANCE))
                .numberFormat("@G 3")
                .dateTimeFormat("@HI")
                .booleanFormat("y,n");
    }

    @Before
    public void setup() throws TemplateException {
        addToDataModel("s", "abc");
        addToDataModel("n", 1500);
        addToDataModel("dt", TM);
        addToDataModel("b", Boolean.TRUE);
        addToDataModel("m", HTMLOutputFormat.INSTANCE.fromMarkup("<p>M</p>"));
    }

}
