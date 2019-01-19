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
package org.apache.freemarker.manual.examples;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.impl.AliasTemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.impl.AliasTemplateNumberFormatFactory;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("boxing")
public class CustomFormatsExample extends TemplateTest {

    @Test
    public void aliases1() throws IOException, TemplateException {
        setConfiguration(newConfigurationBuilder()
            .customNumberFormats(ImmutableMap.<String, TemplateNumberFormatFactory>of(
                    "price", new AliasTemplateNumberFormatFactory(",000.00"),
                    "weight", new AliasTemplateNumberFormatFactory("0.##;; roundingMode=halfUp")))
            .customDateFormats(ImmutableMap.<String, TemplateDateFormatFactory>of(
                    "fileDate", new AliasTemplateDateFormatFactory("dd/MMM/yy hh:mm a"),
                    "logEventTime", new AliasTemplateDateFormatFactory("iso ms u")
                    )));

        addToDataModel("p", 10000);
        addToDataModel("w", new BigDecimal("10.305"));
        addToDataModel("fd", new Date(1450904944213L));
        addToDataModel("let", new Date(1450904944213L));
        
        assertOutputForNamed("CustomFormatsExample-alias1.f3ah");
    }

    @Test
    public void aliases2() throws IOException, TemplateException {
        setConfiguration(newConfigurationBuilder()
                .customNumberFormats(ImmutableMap.of(
                        "base", BaseNTemplateNumberFormatFactory.INSTANCE,
                        "oct", new AliasTemplateNumberFormatFactory("@base 8"))));

        assertOutputForNamed("CustomFormatsExample-alias2.f3ah");
    }

    @Test
    public void modelAware() throws IOException, TemplateException {
        setConfiguration(newConfigurationBuilder()
                .customNumberFormats(ImmutableMap.<String, TemplateNumberFormatFactory>of(
                        "ua", UnitAwareTemplateNumberFormatFactory.INSTANCE))
                .numberFormat("@ua 0.####;; roundingMode=halfUp"));

        addToDataModel("weight", new UnitAwareTemplateNumberModel(1.5, "kg"));
        
        assertOutputForNamed("CustomFormatsExample-modelAware.f3ah");
    }

}
