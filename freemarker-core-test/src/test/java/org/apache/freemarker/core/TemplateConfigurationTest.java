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

import static org.junit.Assert.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.collections.ListUtils;
import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.ConservativeArithmeticEngine;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileExtensionMatcher;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.userpkg.BaseNTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisDivTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.HexTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.LocAndTZSensitiveTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.LocaleSensitiveTemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.apache.freemarker.test.MonitoredTemplateLoader;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("boxing")
public class TemplateConfigurationTest {

    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");

    private final class DummyArithmeticEngine extends ArithmeticEngine {

        @Override
        public int compareNumbers(Number first, Number second) throws TemplateException {
            return 0;
        }

        @Override
        public Number add(Number first, Number second) throws TemplateException {
            return 22;
        }

        @Override
        public Number subtract(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number multiply(Number first, Number second) throws TemplateException {
            return 33;
        }

        @Override
        public Number divide(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number modulus(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number toNumber(String s) {
            return 11;
        }
    }

    private static final Configuration DEFAULT_CFG;
    static {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder();
        StringTemplateLoader stl = new StringTemplateLoader();
        stl.putTemplate("t1.ftl", "<#global loaded = (loaded!) + 't1;'>In t1;");
        stl.putTemplate("t2.ftl", "<#global loaded = (loaded!) + 't2;'>In t2;");
        stl.putTemplate("t3.ftl", "<#global loaded = (loaded!) + 't3;'>In t3;");
        try {
            DEFAULT_CFG = cfgB.templateLoader(stl).build();
        } catch (ConfigurationException e) {
            throw new IllegalStateException("Faild to create default configuration", e);
        }
    }

    private static final TimeZone NON_DEFAULT_TZ;
    static {
        TimeZone defaultTZ = DEFAULT_CFG.getTimeZone();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        if (tz.equals(defaultTZ)) {
            tz = TimeZone.getTimeZone("GMT+01");
            if (tz.equals(defaultTZ)) {
                throw new AssertionError("Couldn't chose a non-default time zone");
            }
        }
        NON_DEFAULT_TZ = tz;
    }

    private static final Locale NON_DEFAULT_LOCALE =
            DEFAULT_CFG.getLocale().equals(Locale.US) ? Locale.GERMAN : Locale.US;

    private static final Charset NON_DEFAULT_ENCODING =
            DEFAULT_CFG.getSourceEncoding().equals(StandardCharsets.UTF_8) ? StandardCharsets.UTF_16LE
                    : StandardCharsets.UTF_8;

    private static final Map<String, Object> SETTING_ASSIGNMENTS;

    static {
        SETTING_ASSIGNMENTS = new HashMap<>();

        // "MutableProcessingConfiguration" settings:
        SETTING_ASSIGNMENTS.put("APIBuiltinEnabled", true);
        SETTING_ASSIGNMENTS.put("SQLDateAndTimeTimeZone", NON_DEFAULT_TZ);
        SETTING_ASSIGNMENTS.put("URLEscapingCharset", StandardCharsets.UTF_16);
        SETTING_ASSIGNMENTS.put("autoFlush", false);
        SETTING_ASSIGNMENTS.put("booleanFormat", "J,N");
        SETTING_ASSIGNMENTS.put("dateFormat", "yyyy-#DDD");
        SETTING_ASSIGNMENTS.put("dateTimeFormat", "yyyy-#DDD-@HH:mm");
        SETTING_ASSIGNMENTS.put("locale", NON_DEFAULT_LOCALE);
        SETTING_ASSIGNMENTS.put("logTemplateExceptions", true);
        SETTING_ASSIGNMENTS.put("newBuiltinClassResolver", TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        SETTING_ASSIGNMENTS.put("numberFormat", "0.0000");
        SETTING_ASSIGNMENTS.put("objectWrapper",
                new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
        SETTING_ASSIGNMENTS.put("outputEncoding", StandardCharsets.UTF_16);
        SETTING_ASSIGNMENTS.put("showErrorTips", false);
        SETTING_ASSIGNMENTS.put("templateExceptionHandler", TemplateExceptionHandler.IGNORE_HANDLER);
        SETTING_ASSIGNMENTS.put("timeFormat", "@HH:mm");
        SETTING_ASSIGNMENTS.put("timeZone", NON_DEFAULT_TZ);
        SETTING_ASSIGNMENTS.put("arithmeticEngine", ConservativeArithmeticEngine.INSTANCE);
        SETTING_ASSIGNMENTS.put("customNumberFormats",
                ImmutableMap.of("dummy", HexTemplateNumberFormatFactory.INSTANCE));
        SETTING_ASSIGNMENTS.put("customDateFormats",
                ImmutableMap.of("dummy", EpochMillisTemplateDateFormatFactory.INSTANCE));
        SETTING_ASSIGNMENTS.put("customAttributes", ImmutableMap.of("dummy", 123));

        // Parser-only settings:
        SETTING_ASSIGNMENTS.put("templateLanguage", TemplateLanguage.STATIC_TEXT);
        SETTING_ASSIGNMENTS.put("tagSyntax", TagSyntax.SQUARE_BRACKET);
        SETTING_ASSIGNMENTS.put("namingConvention", NamingConvention.LEGACY);
        SETTING_ASSIGNMENTS.put("whitespaceStripping", false);
        SETTING_ASSIGNMENTS.put("strictSyntaxMode", false);
        SETTING_ASSIGNMENTS.put("autoEscapingPolicy", AutoEscapingPolicy.DISABLE);
        SETTING_ASSIGNMENTS.put("outputFormat", HTMLOutputFormat.INSTANCE);
        SETTING_ASSIGNMENTS.put("recognizeStandardFileExtensions", false);
        SETTING_ASSIGNMENTS.put("tabSize", 1);
        SETTING_ASSIGNMENTS.put("lazyImports", Boolean.TRUE);
        SETTING_ASSIGNMENTS.put("lazyAutoImports", Boolean.FALSE);
        SETTING_ASSIGNMENTS.put("autoImports", ImmutableMap.of("a", "/lib/a.ftl"));
        SETTING_ASSIGNMENTS.put("autoIncludes", ImmutableList.of("/lib/b.ftl"));
        
        // Special settings:
        SETTING_ASSIGNMENTS.put("sourceEncoding", NON_DEFAULT_ENCODING);
    }
    
    public static String getIsSetMethodName(String readMethodName) {
        return (readMethodName.startsWith("get") ? "is" + readMethodName.substring(3)
                : readMethodName)
                + "Set";
    }

    public static List<PropertyDescriptor> getTemplateConfigurationSettingPropDescs(
            Class<? extends ProcessingConfiguration> confClass, boolean includeCompilerSettings)
            throws IntrospectionException {
        List<PropertyDescriptor> settingPropDescs = new ArrayList<>();

        BeanInfo beanInfo = Introspector.getBeanInfo(confClass);
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            String name = pd.getName();
            if (pd.getWriteMethod() != null && !IGNORED_PROP_NAMES.contains(name)
                    && (includeCompilerSettings
                            || (CONFIGURABLE_PROP_NAMES.contains(name) || !PARSER_PROP_NAMES.contains(name)))) {
                if (pd.getReadMethod() == null) {
                    throw new AssertionError("Property has no read method: " + pd);
                }
                settingPropDescs.add(pd);
            }
        }

        Collections.sort(settingPropDescs, new Comparator<PropertyDescriptor>() {
            @Override
            public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return settingPropDescs;
    }

    private static final Set<String> IGNORED_PROP_NAMES;

    static {
        IGNORED_PROP_NAMES = new HashSet();
        IGNORED_PROP_NAMES.add("class");
        IGNORED_PROP_NAMES.add("strictBeanModels");
        IGNORED_PROP_NAMES.add("parentConfiguration");
        IGNORED_PROP_NAMES.add("settings");
    }

    private static final Set<String> CONFIGURABLE_PROP_NAMES;
    static {
        CONFIGURABLE_PROP_NAMES = new HashSet<>();
        try {
            for (PropertyDescriptor propDesc : Introspector.getBeanInfo(MutableProcessingConfiguration.class).getPropertyDescriptors()) {
                String propName = propDesc.getName();
                if (!IGNORED_PROP_NAMES.contains(propName)) {
                    CONFIGURABLE_PROP_NAMES.add(propName);
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Failed to init static field", e);
        }
    }
    
    private static final Set<String> PARSER_PROP_NAMES;
    static {
        PARSER_PROP_NAMES = new HashSet<>();
        // It's an interface; can't use standard Inrospector
        for (Method m : ParsingConfiguration.class.getMethods()) {
            String propertyName;
            String name = m.getName();
            if (name.startsWith("get")) {
                propertyName = name.substring(3);
            } else if (name.startsWith("is") && !name.endsWith("Set")) {
                propertyName = name.substring(2);
            } else {
                propertyName = null;
            }
            if (propertyName != null) {
                if (!Character.isUpperCase(propertyName.charAt(1))) {
                    propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
                }
                PARSER_PROP_NAMES.add(propertyName);
            }
        }
    }

    private static final Object CA1 = new Object();
    private static final String CA2 = "ca2";
    private static final String CA3 = "ca3";
    private static final String CA4 = "ca4";

    @Test
    public void testMergeBasicFunctionality() throws Exception {
        for (PropertyDescriptor propDesc1 : getTemplateConfigurationSettingPropDescs(
                TemplateConfiguration.Builder.class, true)) {
            for (PropertyDescriptor propDesc2 : getTemplateConfigurationSettingPropDescs(
                    TemplateConfiguration.Builder.class, true)) {
                TemplateConfiguration.Builder tcb1 = new TemplateConfiguration.Builder();
                TemplateConfiguration.Builder tcb2 = new TemplateConfiguration.Builder();

                Object value1 = SETTING_ASSIGNMENTS.get(propDesc1.getName());
                propDesc1.getWriteMethod().invoke(tcb1, value1);
                Object value2 = SETTING_ASSIGNMENTS.get(propDesc2.getName());
                propDesc2.getWriteMethod().invoke(tcb2, value2);

                tcb1.merge(tcb2.build());
                if (propDesc1.getName().equals(propDesc2.getName()) && value1 instanceof List
                        && !propDesc1.getName().equals("autoIncludes")) {
                    assertEquals("For " + propDesc1.getName(),
                            ListUtils.union((List) value1, (List) value1), propDesc1.getReadMethod().invoke(tcb1));
                } else { // Values of the same setting merged
                    assertEquals("For " + propDesc1.getName(), value1, propDesc1.getReadMethod().invoke(tcb1));
                    assertEquals("For " + propDesc2.getName(), value2, propDesc2.getReadMethod().invoke(tcb1));
                }
            }
        }
    }
    
    @Test
    public void testMergeMapSettings() throws Exception {
        TemplateConfiguration.Builder tcb1 = new TemplateConfiguration.Builder();
        tcb1.setCustomDateFormats(ImmutableMap.of(
                "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                "x", LocAndTZSensitiveTemplateDateFormatFactory.INSTANCE));
        tcb1.setCustomNumberFormats(ImmutableMap.of(
                "hex", HexTemplateNumberFormatFactory.INSTANCE,
                "x", LocaleSensitiveTemplateNumberFormatFactory.INSTANCE));
        tcb1.setAutoImports(ImmutableMap.of("a", "a1.ftl", "b", "b1.ftl"));
        
        TemplateConfiguration.Builder tcb2 = new TemplateConfiguration.Builder();
        tcb2.setCustomDateFormats(ImmutableMap.of(
                "loc", LocAndTZSensitiveTemplateDateFormatFactory.INSTANCE,
                "x", EpochMillisDivTemplateDateFormatFactory.INSTANCE));
        tcb2.setCustomNumberFormats(ImmutableMap.of(
                "loc", LocaleSensitiveTemplateNumberFormatFactory.INSTANCE,
                "x", BaseNTemplateNumberFormatFactory.INSTANCE));
        tcb2.setAutoImports(ImmutableMap.of("b", "b2.ftl", "c", "c2.ftl"));
        
        tcb1.merge(tcb2.build());
        
        Map<String, ? extends TemplateDateFormatFactory> mergedCustomDateFormats = tcb1.getCustomDateFormats();
        assertEquals(EpochMillisTemplateDateFormatFactory.INSTANCE, mergedCustomDateFormats.get("epoch"));
        assertEquals(LocAndTZSensitiveTemplateDateFormatFactory.INSTANCE, mergedCustomDateFormats.get("loc"));
        assertEquals(EpochMillisDivTemplateDateFormatFactory.INSTANCE, mergedCustomDateFormats.get("x"));
        
        Map<String, ? extends TemplateNumberFormatFactory> mergedCustomNumberFormats = tcb1.getCustomNumberFormats();
        assertEquals(HexTemplateNumberFormatFactory.INSTANCE, mergedCustomNumberFormats.get("hex"));
        assertEquals(LocaleSensitiveTemplateNumberFormatFactory.INSTANCE, mergedCustomNumberFormats.get("loc"));
        assertEquals(BaseNTemplateNumberFormatFactory.INSTANCE, mergedCustomNumberFormats.get("x"));

        Map<String, String> mergedAutoImports = tcb1.getAutoImports();
        assertEquals("a1.ftl", mergedAutoImports.get("a"));
        assertEquals("b2.ftl", mergedAutoImports.get("b"));
        assertEquals("c2.ftl", mergedAutoImports.get("c"));
        
        // Empty map merging optimization:
        tcb1.merge(new TemplateConfiguration.Builder().build());
        assertSame(mergedCustomDateFormats, tcb1.getCustomDateFormats());
        assertSame(mergedCustomNumberFormats, tcb1.getCustomNumberFormats());
        
        // Empty map merging optimization:
        TemplateConfiguration.Builder tcb3 = new TemplateConfiguration.Builder();
        tcb3.merge(tcb1.build());
        assertSame(mergedCustomDateFormats, tcb3.getCustomDateFormats());
        assertSame(mergedCustomNumberFormats, tcb3.getCustomNumberFormats());
    }
    
    @Test
    public void testMergeListSettings() throws Exception {
        TemplateConfiguration.Builder tcb1 = new TemplateConfiguration.Builder();
        tcb1.setAutoIncludes(ImmutableList.of("a.ftl", "x.ftl", "b.ftl"));
        
        TemplateConfiguration.Builder tcb2 = new TemplateConfiguration.Builder();
        tcb2.setAutoIncludes(ImmutableList.of("c.ftl", "x.ftl", "d.ftl"));
        
        tcb1.merge(tcb2.build());
        
        assertEquals(ImmutableList.of("a.ftl", "b.ftl", "c.ftl", "x.ftl", "d.ftl"), tcb1.getAutoIncludes());
    }
    
    @Test
    public void testMergePriority() throws Exception {
        TemplateConfiguration.Builder tcb1 = new TemplateConfiguration.Builder();
        tcb1.setDateFormat("1");
        tcb1.setTimeFormat("1");
        tcb1.setDateTimeFormat("1");

        TemplateConfiguration.Builder tcb2 = new TemplateConfiguration.Builder();
        tcb2.setDateFormat("2");
        tcb2.setTimeFormat("2");

        TemplateConfiguration.Builder tcb3 = new TemplateConfiguration.Builder();
        tcb3.setDateFormat("3");

        tcb1.merge(tcb2.build());
        tcb1.merge(tcb3.build());

        assertEquals("3", tcb1.getDateFormat());
        assertEquals("2", tcb1.getTimeFormat());
        assertEquals("1", tcb1.getDateTimeFormat());
    }
    
    @Test
    public void testMergeCustomAttributes() throws Exception {
        TemplateConfiguration.Builder tc1 = new TemplateConfiguration.Builder();
        tc1.setCustomAttribute("k1", "v1");
        tc1.setCustomAttribute("k2", "v1");
        tc1.setCustomAttribute("k3", "v1");
        tc1.setCustomAttribute(CA1, "V1");
        tc1.setCustomAttribute(CA2, "V1");
        tc1.setCustomAttribute(CA3, "V1");

        TemplateConfiguration.Builder tcb2 = new TemplateConfiguration.Builder();
        tcb2.setCustomAttribute("k1", "v2");
        tcb2.setCustomAttribute("k2", "v2");
        tcb2.setCustomAttribute(CA1, "V2");
        tcb2.setCustomAttribute(CA2, "V2");

        TemplateConfiguration.Builder tcb3 = new TemplateConfiguration.Builder();
        tcb3.setCustomAttribute("k1", "v3");
        tcb3.setCustomAttribute(CA1, "V3");

        tc1.merge(tcb2.build());
        tc1.merge(tcb3.build());

        assertEquals("v3", tc1.getCustomAttribute("k1"));
        assertEquals("v2", tc1.getCustomAttribute("k2"));
        assertEquals("v1", tc1.getCustomAttribute("k3"));
        assertEquals("V3", tc1.getCustomAttribute(CA1));
        assertEquals("V2", tc1.getCustomAttribute(CA2));
        assertEquals("V1", tc1.getCustomAttribute(CA3));
    }

    @Test
    public void testMergeNullCustomAttributes() throws Exception {
        TemplateConfiguration.Builder tcb1 = new TemplateConfiguration.Builder();
        tcb1.setCustomAttribute("k1", "v1");
        tcb1.setCustomAttribute("k2", "v1");
        tcb1.setCustomAttribute(CA1, "V1");
        tcb1.setCustomAttribute(CA2,"V1");

        assertEquals("v1", tcb1.getCustomAttribute("k1"));
        assertEquals("v1", tcb1.getCustomAttribute("k2"));
        assertNull("v1", tcb1.getCustomAttribute("k3"));
        assertEquals("V1", tcb1.getCustomAttribute(CA1));
        assertEquals("V1", tcb1.getCustomAttribute(CA2));
        assertNull(tcb1.getCustomAttribute(CA3));

        TemplateConfiguration.Builder tcb2 = new TemplateConfiguration.Builder();
        tcb2.setCustomAttribute("k1", "v2");
        tcb2.setCustomAttribute("k2", null);
        tcb2.setCustomAttribute(CA1, "V2");
        tcb2.setCustomAttribute(CA2, null);

        TemplateConfiguration.Builder tcb3 = new TemplateConfiguration.Builder();
        tcb3.setCustomAttribute("k1", null);
        tcb2.setCustomAttribute(CA1, null);

        tcb1.merge(tcb2.build());
        tcb1.merge(tcb3.build());

        assertNull(tcb1.getCustomAttribute("k1"));
        assertNull(tcb1.getCustomAttribute("k2"));
        assertNull(tcb1.getCustomAttribute("k3"));
        assertNull(tcb1.getCustomAttribute(CA1));
        assertNull(tcb1.getCustomAttribute(CA2));
        assertNull(tcb1.getCustomAttribute(CA3));

        TemplateConfiguration.Builder tcb4 = new TemplateConfiguration.Builder();
        tcb4.setCustomAttribute("k1", "v4");
        tcb4.setCustomAttribute(CA1, "V4");

        tcb1.merge(tcb4.build());

        assertEquals("v4", tcb1.getCustomAttribute("k1"));
        assertNull(tcb1.getCustomAttribute("k2"));
        assertNull(tcb1.getCustomAttribute("k3"));
        assertEquals("V4", tcb1.getCustomAttribute(CA1));
        assertNull(tcb1.getCustomAttribute(CA2));
        assertNull(tcb1.getCustomAttribute(CA3));
    }

    @Test
    public void testConfigureNonParserConfig() throws Exception {
        for (PropertyDescriptor pd : getTemplateConfigurationSettingPropDescs(
                TemplateConfiguration.Builder.class, false)) {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();

            Object newValue = SETTING_ASSIGNMENTS.get(pd.getName());
            pd.getWriteMethod().invoke(tcb, newValue);
            
            TemplateConfiguration tc = tcb.build();

            Method tReaderMethod = Template.class.getMethod(pd.getReadMethod().getName());

            // Without TC
            assertNotEquals("For \"" + pd.getName() + "\"",
                    tReaderMethod.invoke(new Template(null, "", DEFAULT_CFG)));
            // With TC
            assertEquals("For \"" + pd.getName() + "\"", newValue,
                    tReaderMethod.invoke(new Template(null, "", DEFAULT_CFG, tc)));
        }
    }
    
    @Test
    public void testConfigureCustomAttributes() throws Exception {
        Configuration cfg = new TestConfigurationBuilder()
                .customAttribute("k1", "c")
                .customAttribute("k2", "c")
                .customAttribute("k3", "c")
                .build();

        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setCustomAttribute("k2", "tc");
        tcb.setCustomAttribute("k3", null);
        tcb.setCustomAttribute("k4", "tc");
        tcb.setCustomAttribute("k5", "tc");
        tcb.setCustomAttribute("k6", "tc");
        tcb.setCustomAttribute(CA1, "tc");
        tcb.setCustomAttribute(CA2,"tc");
        tcb.setCustomAttribute(CA3,"tc");

        TemplateConfiguration tc = tcb.build();
        Template t = new Template(null, "", cfg, tc);
        t.setCustomAttribute("k5", "t");
        t.setCustomAttribute("k6", null);
        t.setCustomAttribute("k7", "t");
        t.setCustomAttribute(CA2, "t");
        t.setCustomAttribute(CA3, null);
        t.setCustomAttribute(CA4, "t");

        assertEquals("c", t.getCustomAttribute("k1"));
        assertEquals("tc", t.getCustomAttribute("k2"));
        assertNull(t.getCustomAttribute("k3"));
        assertEquals("tc", t.getCustomAttribute("k4"));
        assertEquals("t", t.getCustomAttribute("k5"));
        assertNull(t.getCustomAttribute("k6"));
        assertEquals("t", t.getCustomAttribute("k7"));
        assertEquals("tc", t.getCustomAttribute(CA1));
        assertEquals("t", t.getCustomAttribute(CA2));
        assertNull(t.getCustomAttribute(CA3));
        assertEquals("t", t.getCustomAttribute(CA4));
    }
    
    @Test
    public void testConfigureParser() throws Exception {
        Set<String> testedProps = new HashSet<>();
        
        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setTagSyntax(TagSyntax.SQUARE_BRACKET);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "[#if true]y[/#if]", "[#if true]y[/#if]", "y");
            testedProps.add(Configuration.ExtendableBuilder.TAG_SYNTAX_KEY_CAMEL_CASE);
        }
        
        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setNamingConvention(NamingConvention.CAMEL_CASE);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "<#if true>y<#elseif false>n</#if>", "y", null);
            testedProps.add(Configuration.ExtendableBuilder.NAMING_CONVENTION_KEY_CAMEL_CASE);
        }
        
        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setWhitespaceStripping(false);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "<#if true>\nx\n</#if>\n", "x\n", "\nx\n\n");
            testedProps.add(Configuration.ExtendableBuilder.WHITESPACE_STRIPPING_KEY_CAMEL_CASE);
        }

        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setArithmeticEngine(new DummyArithmeticEngine());
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "${1} ${1+1}", "1 2", "11 22");
            testedProps.add(Configuration.ExtendableBuilder.ARITHMETIC_ENGINE_KEY_CAMEL_CASE);
        }

        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setOutputFormat(XMLOutputFormat.INSTANCE);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "${.outputFormat} ${\"a'b\"}",
                    UndefinedOutputFormat.INSTANCE.getName() + " a'b",
                    XMLOutputFormat.INSTANCE.getName() + " a&apos;b");
            testedProps.add(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY_CAMEL_CASE);
        }

        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setOutputFormat(XMLOutputFormat.INSTANCE);
            tcb.setAutoEscapingPolicy(AutoEscapingPolicy.DISABLE);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "${'a&b'}", "a&b", "a&b");
            testedProps.add(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE);
        }
        
        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            /* Can't test this now, as the only valid value is 3.0.0. [FM3.0.1]
            TemplateConfiguration tc = tcb.build();
            tc.setParentConfiguration(new Configuration(new Version(2, 3, 0)));
            assertOutputWithoutAndWithTC(tc, "<#foo>", null, "<#foo>");
            */
            testedProps.add(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE);
        }

        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setRecognizeStandardFileExtensions(false);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc, "adhoc.ftlh", "${.outputFormat}",
                    HTMLOutputFormat.INSTANCE.getName(), UndefinedOutputFormat.INSTANCE.getName());
            testedProps.add(Configuration.ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE);
        }

        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setLogTemplateExceptions(false);
            tcb.setTabSize(3);
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc,
                    "<#attempt><@'\\t$\\{1+}'?interpret/><#recover>"
                    + "${.error?replace('(?s).*?column ([0-9]+).*', '$1', 'r')}"
                    + "</#attempt>",
                    "13", "8");
            testedProps.add(Configuration.ExtendableBuilder.TAB_SIZE_KEY_CAMEL_CASE);
        }

        {
            // As the TemplateLanguage-based parser selection happens in the TemplateResolver, we can't use
            // assertOutput here, as that hard-coded to create an FTL Template.

            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setTemplateLanguage(TemplateLanguage.STATIC_TEXT);

            TestConfigurationBuilder cfgB = new TestConfigurationBuilder();
            cfgB.setTemplateConfigurations(
                    new ConditionalTemplateConfigurationFactory(new FileExtensionMatcher("txt"), tcb.build()));

            StringTemplateLoader templateLoader = new StringTemplateLoader();
            templateLoader.putTemplate("adhoc.ftl", "${1+1}");
            templateLoader.putTemplate("adhoc.txt", "${1+1}");
            cfgB.setTemplateLoader(templateLoader);

            Configuration cfg = cfgB.build();
            
            {
                StringWriter out = new StringWriter();
                cfg.getTemplate("adhoc.ftl").process(null, out);
                assertEquals("2", out.toString());
            }
            {
                StringWriter out = new StringWriter();
                cfg.getTemplate("adhoc.txt").process(null, out);
                assertEquals("${1+1}", out.toString());
            }

            testedProps.add(Configuration.ExtendableBuilder.TEMPLATE_LANGUAGE_KEY_CAMEL_CASE);
        }

        {
            // As the TemplateLanguage-based parser selection happens in the TemplateResolver, we can't use
            // assertOutput here, as that hard-coded to create an FTL Template.

            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setSourceEncoding(StandardCharsets.ISO_8859_1);

            TestConfigurationBuilder cfgB = new TestConfigurationBuilder();
            cfgB.setSourceEncoding(StandardCharsets.UTF_8);
            cfgB.setTemplateConfigurations(new ConditionalTemplateConfigurationFactory(
                    new FileNameGlobMatcher("latin1.ftl"), tcb.build()));

            MonitoredTemplateLoader templateLoader = new MonitoredTemplateLoader();
            templateLoader.putBinaryTemplate("utf8.ftl", "próba", StandardCharsets.UTF_8, 1);
            templateLoader.putBinaryTemplate("latin1.ftl", "próba", StandardCharsets.ISO_8859_1, 1);
            cfgB.setTemplateLoader(templateLoader);

            Configuration cfg = cfgB.build();
            
            {
                StringWriter out = new StringWriter();
                cfg.getTemplate("utf8.ftl").process(null, out);
                assertEquals("próba", out.toString());
            }
            {
                StringWriter out = new StringWriter();
                cfg.getTemplate("latin1.ftl").process(null, out);
                assertEquals("próba", out.toString());
            }

            testedProps.add(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY_CAMEL_CASE);
        }

        if (!PARSER_PROP_NAMES.equals(testedProps)) {
            Set<String> diff = new HashSet<>(PARSER_PROP_NAMES);
            diff.removeAll(testedProps);
            fail("Some settings weren't checked: " + diff);
        }
    }
    
    @Test
    public void testArithmeticEngine() throws TemplateException, IOException {
        TemplateConfiguration tc = new TemplateConfiguration.Builder()
                .arithmeticEngine(new DummyArithmeticEngine())
                .build();
        assertOutputWithoutAndWithTC(tc,
                "<#setting locale='en_US'>${1} ${1+1} ${1*3} <#assign x = 1>${x + x} ${x * 3}",
                "1 2 3 2 3", "11 22 33 22 33");
        
        // Does affect template.arithmeticEngine (unlike in FM2)
        Template t = new Template(null, null, new StringReader(""), DEFAULT_CFG, tc, null);
        assertEquals(tc.getArithmeticEngine(), t.getArithmeticEngine());
    }

    @Test
    public void testAutoImport() throws TemplateException, IOException {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setAutoImports(ImmutableMap.of("t1", "t1.ftl", "t2", "t2.ftl"));
        TemplateConfiguration tc = tcb.build();
        assertOutputWithoutAndWithTC(tc, "<#import 't3.ftl' as t3>${loaded}", "t3;", "t1;t2;t3;");
    }

    @Test
    public void testAutoIncludes() throws TemplateException, IOException {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setAutoIncludes(ImmutableList.of("t1.ftl", "t2.ftl"));
        TemplateConfiguration tc = tcb.build();
        assertOutputWithoutAndWithTC(tc, "<#include 't3.ftl'>", "In t3;", "In t1;In t2;In t3;");
    }
    
    @Test
    public void testStringInterpolate() throws TemplateException, IOException {
        TemplateConfiguration tc = new TemplateConfiguration.Builder()
                .arithmeticEngine(new DummyArithmeticEngine())
                .build();
        assertOutputWithoutAndWithTC(tc,
                "<#setting locale='en_US'>${'${1} ${1+1} ${1*3}'} <#assign x = 1>${'${x + x} ${x * 3}'}",
                "1 2 3 2 3", "11 22 33 22 33");
        
        // Does affect template.arithmeticEngine (unlike in FM2):
        Template t = new Template(null, null, new StringReader(""), DEFAULT_CFG, tc, null);
        assertEquals(tc.getArithmeticEngine(), t.getArithmeticEngine());
    }
    
    @Test
    public void testInterpret() throws TemplateException, IOException {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setArithmeticEngine(new DummyArithmeticEngine());
        {
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc,
                    "<#setting locale='en_US'><#assign src = r'${1} <#assign x = 1>${x + x}'><@src?interpret />",
                    "1 2", "11 22");
        }
        tcb.setWhitespaceStripping(false);
        {
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc,
                    "<#if true>\nX</#if><#assign src = r'<#if true>\nY</#if>'><@src?interpret />",
                    "XY", "\nX\nY");
        }
    }

    @Test
    public void testEval() throws TemplateException, IOException {
        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            tcb.setArithmeticEngine(new DummyArithmeticEngine());
            TemplateConfiguration tc = tcb.build();
            assertOutputWithoutAndWithTC(tc,
                    "<#assign x = 1>${r'1 + x'?eval?c}",
                    "2", "22");
            assertOutputWithoutAndWithTC(tc,
                    "${r'1?c'?eval}",
                    "1", "11");
        }
        
        {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            Charset outputEncoding = ISO_8859_2;
            tcb.setOutputEncoding(outputEncoding);

            String legacyNCFtl = "${r'.output_encoding!\"null\"'?eval}";
            String camelCaseNCFtl = "${r'.outputEncoding!\"null\"'?eval}";

            {
                TemplateConfiguration tc = tcb.build();

                // Default is re-auto-detecting in ?eval:
                assertOutputWithoutAndWithTC(tc, legacyNCFtl, "null", outputEncoding.name());
                assertOutputWithoutAndWithTC(tc, camelCaseNCFtl, "null", outputEncoding.name());
            }

            {
                // Force camelCase:
                tcb.setNamingConvention(NamingConvention.CAMEL_CASE);

                TemplateConfiguration tc = tcb.build();

                assertOutputWithoutAndWithTC(tc, legacyNCFtl, "null", null);
                assertOutputWithoutAndWithTC(tc, camelCaseNCFtl, "null", outputEncoding.name());
            }

            {
                // Force legacy:
                tcb.setNamingConvention(NamingConvention.LEGACY);

                TemplateConfiguration tc = tcb.build();

                assertOutputWithoutAndWithTC(tc, legacyNCFtl, "null", outputEncoding.name());
                assertOutputWithoutAndWithTC(tc, camelCaseNCFtl, "null", null);
            }
        }
    }
    
    private void assertOutputWithoutAndWithTC(
            TemplateConfiguration tc, String ftl, String expectedDefaultOutput,
            String expectedConfiguredOutput) throws TemplateException, IOException {
        assertOutputWithoutAndWithTC(tc, null, ftl, expectedDefaultOutput, expectedConfiguredOutput);
    }
    
    private void assertOutputWithoutAndWithTC(
            TemplateConfiguration tc, String templateName, String ftl, String expectedDefaultOutput,
            String expectedConfiguredOutput) throws TemplateException, IOException {
        if (templateName == null) {
            templateName = "adhoc.ftl";
        }
        assertOutput(null, templateName, ftl, expectedDefaultOutput);
        assertOutput(tc, templateName, ftl, expectedConfiguredOutput);
    }

    private void assertOutput(TemplateConfiguration tc, String templateName, String ftl, String
            expectedConfiguredOutput)
            throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        try {
            Template t = new Template(templateName, null, new StringReader(ftl), DEFAULT_CFG, tc, null);
            t.process(null, sw);
            if (expectedConfiguredOutput == null) {
                fail("Template should have fail.");
            }
        } catch (TemplateException|ParseException e) {
            if (expectedConfiguredOutput != null) {
                throw e;
            }
        }
        if (expectedConfiguredOutput != null) {
            assertEquals(expectedConfiguredOutput, sw.toString());
        }
    }

    @Test
    public void testIsSet() throws Exception {
        for (PropertyDescriptor pd : getTemplateConfigurationSettingPropDescs(
                TemplateConfiguration.Builder.class, true)) {
            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            checkAllIsSetFalseExcept(tcb.build(), null);
            pd.getWriteMethod().invoke(tcb, SETTING_ASSIGNMENTS.get(pd.getName()));
            checkAllIsSetFalseExcept(tcb.build(), pd.getName());
        }
    }

    private void checkAllIsSetFalseExcept(TemplateConfiguration tc, String setSetting)
            throws SecurityException, IntrospectionException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        for (PropertyDescriptor pd : getTemplateConfigurationSettingPropDescs(TemplateConfiguration.class, true)) {
            String isSetMethodName = getIsSetMethodName(pd.getReadMethod().getName());
            Method isSetMethod;
            try {
                isSetMethod = tc.getClass().getMethod(isSetMethodName);
            } catch (NoSuchMethodException e) {
                fail("Missing " + isSetMethodName + " method for \"" + pd.getName() + "\".");
                return;
            }
            if (pd.getName().equals(setSetting)) {
                assertTrue(isSetMethod + " should return true", (Boolean) (isSetMethod.invoke(tc)));
            } else {
                assertFalse(isSetMethod + " should return false", (Boolean) (isSetMethod.invoke(tc)));
            }
        }
    }

    /**
     * Test case self-check.
     */
    @Test
    public void checkTestAssignments() throws Exception {
        for (PropertyDescriptor pd : getTemplateConfigurationSettingPropDescs(
                TemplateConfiguration.Builder.class, true)) {
            String propName = pd.getName();
            if (!SETTING_ASSIGNMENTS.containsKey(propName)) {
                fail("Test case doesn't cover all settings in SETTING_ASSIGNMENTS. Missing: " + propName);
            }
            Method readMethod = pd.getReadMethod();
            String cfgMethodName = readMethod.getName();
            Method cfgMethod = DEFAULT_CFG.getClass().getMethod(cfgMethodName, readMethod.getParameterTypes());
            Object defaultSettingValue = cfgMethod.invoke(DEFAULT_CFG);
            Object assignedValue = SETTING_ASSIGNMENTS.get(propName);
            assertNotEquals("SETTING_ASSIGNMENTS must contain a non-default value for " + propName,
                    assignedValue, defaultSettingValue);

            TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
            try {
                pd.getWriteMethod().invoke(tcb, assignedValue);
            } catch (Exception e) {
                throw new IllegalStateException("For setting \"" + propName + "\" and assigned value of type "
                        + (assignedValue != null ? assignedValue.getClass().getName() : "Null"),
                        e);
            }
        }
    }
    
}
