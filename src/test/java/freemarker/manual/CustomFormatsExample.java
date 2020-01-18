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
package freemarker.manual;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.core.AliasTemplateDateFormatFactory;
import freemarker.core.AliasTemplateNumberFormatFactory;
import freemarker.core.BaseNTemplateNumberFormatFactory;
import freemarker.core.TemplateDateFormatFactory;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@SuppressWarnings("boxing")
public class CustomFormatsExample extends ExamplesTest {

    @Test
    public void aliases1() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();

        Map<String, TemplateNumberFormatFactory> customNumberFormats
                = new HashMap<>();
        customNumberFormats.put("price", new AliasTemplateNumberFormatFactory(",000.00"));
        customNumberFormats.put("weight", new AliasTemplateNumberFormatFactory("0.##;; roundingMode=halfUp"));
        cfg.setCustomNumberFormats(customNumberFormats);

        Map<String, TemplateDateFormatFactory> customDateFormats
                = new HashMap<>();
        customDateFormats.put("fileDate", new AliasTemplateDateFormatFactory("dd/MMM/yy hh:mm a"));
        customDateFormats.put("logEventTime", new AliasTemplateDateFormatFactory("iso ms u"));
        cfg.setCustomDateFormats(customDateFormats);

        addToDataModel("p", 10000);
        addToDataModel("w", new BigDecimal("10.305"));
        addToDataModel("fd", new Date(1450904944213L));
        addToDataModel("let", new Date(1450904944213L));
        
        assertOutputForNamed("CustomFormatsExample-alias1.ftlh");
    }

    @Test
    public void aliases2() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();

        Map<String, TemplateNumberFormatFactory> customNumberFormats
                = new HashMap<>();
        customNumberFormats.put("base", BaseNTemplateNumberFormatFactory.INSTANCE);
        customNumberFormats.put("oct", new AliasTemplateNumberFormatFactory("@base 8"));
        cfg.setCustomNumberFormats(customNumberFormats);
        
        assertOutputForNamed("CustomFormatsExample-alias2.ftlh");
    }

    @Test
    public void modelAware() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();

        Map<String, TemplateNumberFormatFactory> customNumberFormats
                = new HashMap<>();
        customNumberFormats.put("ua", UnitAwareTemplateNumberFormatFactory.INSTANCE);
        cfg.setCustomNumberFormats(customNumberFormats);
        cfg.setNumberFormat("@ua 0.####;; roundingMode=halfUp");

        addToDataModel("weight", new UnitAwareTemplateNumberModel(1.5, "kg"));
        
        assertOutputForNamed("CustomFormatsExample-modelAware.ftlh");
    }

}
