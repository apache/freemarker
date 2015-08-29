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

import static org.hamcrest.Matchers.*;
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

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import freemarker.template.utility.NullArgumentException;

@SuppressWarnings("boxing")
public class TemplateConfigurerTest {

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

    private static final Version ICI = Configuration.VERSION_2_3_22;

    private static final Configuration DEFAULT_CFG = new Configuration(ICI);

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

    private static final Locale NON_DEFAULT_LOCALE;
    static {
        Locale defaultLocale = DEFAULT_CFG.getLocale();
        Locale locale = Locale.GERMAN;
        if (locale.equals(defaultLocale)) {
            locale = Locale.US;
            if (locale.equals(defaultLocale)) {
                throw new AssertionError("Couldn't chose a non-default locale");
            }
        }
        NON_DEFAULT_LOCALE = locale;
    }

    private static final String NON_DEFAULT_ENCODING;

    static {
        String defaultEncoding = DEFAULT_CFG.getDefaultEncoding();
        String encoding = "UTF-16";
        if (encoding.equals(defaultEncoding)) {
            encoding = "UTF-8";
            if (encoding.equals(defaultEncoding)) {
                throw new AssertionError("Couldn't chose a non-default locale");
            }
        }
        NON_DEFAULT_ENCODING = encoding;
    }
    
    private static final Map<String, Object> SETTING_ASSIGNMENTS;

    static {
        SETTING_ASSIGNMENTS = new HashMap<String, Object>();

        // "Configurable" settings:
        SETTING_ASSIGNMENTS.put("APIBuiltinEnabled", true);
        SETTING_ASSIGNMENTS.put("SQLDateAndTimeTimeZone", NON_DEFAULT_TZ);
        SETTING_ASSIGNMENTS.put("URLEscapingCharset", "utf-16");
        SETTING_ASSIGNMENTS.put("autoFlush", false);
        SETTING_ASSIGNMENTS.put("booleanFormat", "J,N");
        SETTING_ASSIGNMENTS.put("classicCompatibleAsInt", 2);
        SETTING_ASSIGNMENTS.put("dateFormat", "yyyy-#DDD");
        SETTING_ASSIGNMENTS.put("dateTimeFormat", "yyyy-#DDD-@HH:mm");
        SETTING_ASSIGNMENTS.put("locale", NON_DEFAULT_LOCALE);
        SETTING_ASSIGNMENTS.put("logTemplateExceptions", false);
        SETTING_ASSIGNMENTS.put("newBuiltinClassResolver", TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        SETTING_ASSIGNMENTS.put("numberFormat", "0.0000");
        SETTING_ASSIGNMENTS.put("objectWrapper", new SimpleObjectWrapper(ICI));
        SETTING_ASSIGNMENTS.put("outputEncoding", "utf-16");
        SETTING_ASSIGNMENTS.put("showErrorTips", false);
        SETTING_ASSIGNMENTS.put("templateExceptionHandler", TemplateExceptionHandler.IGNORE_HANDLER);
        SETTING_ASSIGNMENTS.put("timeFormat", "@HH:mm");
        SETTING_ASSIGNMENTS.put("timeZone", NON_DEFAULT_TZ);
        SETTING_ASSIGNMENTS.put("arithmeticEngine", ArithmeticEngine.CONSERVATIVE_ENGINE);
        SETTING_ASSIGNMENTS.put("customNumberFormats",
                ImmutableMap.of("dummy", HexTemplateNumberFormatFactory.INSTANCE));
        SETTING_ASSIGNMENTS.put("customDateFormats",
                ImmutableMap.of("dummy", EpochMillisTemplateDateFormatFactory.INSTANCE));

        // Parser-only settings:
        SETTING_ASSIGNMENTS.put("tagSyntax", Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        SETTING_ASSIGNMENTS.put("namingConvention", Configuration.LEGACY_NAMING_CONVENTION);
        SETTING_ASSIGNMENTS.put("whitespaceStripping", false);
        SETTING_ASSIGNMENTS.put("strictSyntaxMode", false);
        SETTING_ASSIGNMENTS.put("autoEscapingPolicy", Configuration.DISABLE_AUTO_ESCAPING_POLICY);
        SETTING_ASSIGNMENTS.put("outputFormat", HTMLOutputFormat.INSTANCE);
        SETTING_ASSIGNMENTS.put("recognizeStandardFileExtensions", true);
        
        // Special settings:
        SETTING_ASSIGNMENTS.put("encoding", NON_DEFAULT_ENCODING);
    }
    
    public static String getIsSetMethodName(String readMethodName) {
        String isSetMethodName = (readMethodName.startsWith("get") ? "is" + readMethodName.substring(3)
                : readMethodName)
                + "Set";
        if (isSetMethodName.equals("isClassicCompatibleAsIntSet")) {
            isSetMethodName = "isClassicCompatibleSet";
        }
        return isSetMethodName;
    }

    public static List<PropertyDescriptor> getTemplateConfigurerSettingPropDescs(
            boolean includeCompilerSettings, boolean includeSpecialSettings)
            throws IntrospectionException {
        List<PropertyDescriptor> settingPropDescs = new ArrayList<PropertyDescriptor>();

        BeanInfo beanInfo = Introspector.getBeanInfo(TemplateConfigurer.class);
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            String name = pd.getName();
            if (pd.getWriteMethod() != null && !IGNORED_PROP_NAMES.contains(name)
                    && (includeCompilerSettings
                            || (CONFIGURABLE_PROP_NAMES.contains(name) || !PARSER_PROP_NAMES.contains(name)))
                    && (includeSpecialSettings
                            || !SPECIAL_PROP_NAMES.contains(name))) {
                if (pd.getReadMethod() == null) {
                    throw new AssertionError("Property has no read method: " + pd);
                }
                settingPropDescs.add(pd);
            }
        }

        Collections.sort(settingPropDescs, new Comparator<PropertyDescriptor>() {

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
        IGNORED_PROP_NAMES.add("classicCompatible");
    }

    private static final Set<String> CONFIGURABLE_PROP_NAMES;
    static {
        CONFIGURABLE_PROP_NAMES = new HashSet<String>();
        try {
            for (PropertyDescriptor propDesc : Introspector.getBeanInfo(Configurable.class).getPropertyDescriptors()) {
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
        PARSER_PROP_NAMES = new HashSet<String>();
        // It's an interface; can't use standard Inrospector
        for (Method m : ParserConfiguration.class.getMethods()) {
            String propertyName;
            if (m.getName().startsWith("get")) {
                propertyName = m.getName().substring(3);
            } else if (m.getName().startsWith("is")) {
                propertyName = m.getName().substring(2);
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

    private static final Set<String> SPECIAL_PROP_NAMES;
    static {
        SPECIAL_PROP_NAMES = new HashSet<String>();
        SPECIAL_PROP_NAMES.add("encoding");
    }
    
    private static final CustomAttribute CA1 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE); 
    private static final CustomAttribute CA2 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE); 
    private static final CustomAttribute CA3 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE); 
    private static final CustomAttribute CA4 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE); 

    @Test
    public void testMergeBasicFunctionality() throws Exception {
        for (PropertyDescriptor propDesc1 : getTemplateConfigurerSettingPropDescs(true, true)) {
            for (PropertyDescriptor propDesc2 : getTemplateConfigurerSettingPropDescs(true, true)) {
                TemplateConfigurer tc1 = new TemplateConfigurer();
                TemplateConfigurer tc2 = new TemplateConfigurer();

                Object value1 = SETTING_ASSIGNMENTS.get(propDesc1.getName());
                propDesc1.getWriteMethod().invoke(tc1, value1);
                Object value2 = SETTING_ASSIGNMENTS.get(propDesc2.getName());
                propDesc2.getWriteMethod().invoke(tc2, value2);

                tc1.merge(tc2);
                Object mValue1 = propDesc1.getReadMethod().invoke(tc1);
                Object mValue2 = propDesc2.getReadMethod().invoke(tc1);

                assertEquals("For " + propDesc1.getName(), value1, mValue1);
                assertEquals("For " + propDesc2.getName(), value2, mValue2);
            }
        }
    }
    
    @Test
    public void testMergePriority() throws Exception {
        TemplateConfigurer tc1 = new TemplateConfigurer();
        tc1.setDateFormat("1");
        tc1.setTimeFormat("1");
        tc1.setDateTimeFormat("1");

        TemplateConfigurer tc2 = new TemplateConfigurer();
        tc2.setDateFormat("2");
        tc2.setTimeFormat("2");

        TemplateConfigurer tc3 = new TemplateConfigurer();
        tc3.setDateFormat("3");

        tc1.merge(tc2);
        tc1.merge(tc3);

        assertEquals("3", tc1.getDateFormat());
        assertEquals("2", tc1.getTimeFormat());
        assertEquals("1", tc1.getDateTimeFormat());
    }
    
    @Test
    public void testMergeCustomAttributes() throws Exception {
        TemplateConfigurer tc1 = new TemplateConfigurer();
        tc1.setCustomAttribute("k1", "v1");
        tc1.setCustomAttribute("k2", "v1");
        tc1.setCustomAttribute("k3", "v1");
        CA1.set("V1", tc1);
        CA2.set("V1", tc1);
        CA3.set("V1", tc1);

        TemplateConfigurer tc2 = new TemplateConfigurer();
        tc2.setCustomAttribute("k1", "v2");
        tc2.setCustomAttribute("k2", "v2");
        CA1.set("V2", tc2);
        CA2.set("V2", tc2);

        TemplateConfigurer tc3 = new TemplateConfigurer();
        tc3.setCustomAttribute("k1", "v3");
        CA1.set("V3", tc2);

        tc1.merge(tc2);
        tc1.merge(tc3);

        assertEquals("v3", tc1.getCustomAttribute("k1"));
        assertEquals("v2", tc1.getCustomAttribute("k2"));
        assertEquals("v1", tc1.getCustomAttribute("k3"));
        assertEquals("V3", CA1.get(tc1));
        assertEquals("V2", CA2.get(tc1));
        assertEquals("V1", CA3.get(tc1));
    }
    
    @Test
    public void testMergeNullCustomAttributes() throws Exception {
        TemplateConfigurer tc1 = new TemplateConfigurer();
        tc1.setCustomAttribute("k1", "v1");
        tc1.setCustomAttribute("k2", "v1");
        tc1.setCustomAttribute(null, "v1");
        CA1.set("V1", tc1);
        CA2.set("V1", tc1);
        CA3.set(null, tc1);
        
        assertEquals("v1", tc1.getCustomAttribute("k1"));
        assertEquals("v1", tc1.getCustomAttribute("k2"));
        assertNull("v1", tc1.getCustomAttribute("k3"));
        assertEquals("V1", CA1.get(tc1));
        assertEquals("V1", CA2.get(tc1));
        assertNull(CA3.get(tc1));

        TemplateConfigurer tc2 = new TemplateConfigurer();
        tc2.setCustomAttribute("k1", "v2");
        tc2.setCustomAttribute("k2", null);
        CA1.set("V2", tc2);
        CA2.set(null, tc2);

        TemplateConfigurer tc3 = new TemplateConfigurer();
        tc3.setCustomAttribute("k1", null);
        CA1.set(null, tc2);

        tc1.merge(tc2);
        tc1.merge(tc3);

        assertNull(tc1.getCustomAttribute("k1"));
        assertNull(tc1.getCustomAttribute("k2"));
        assertNull(tc1.getCustomAttribute("k3"));
        assertNull(CA1.get(tc1));
        assertNull(CA2.get(tc1));
        assertNull(CA3.get(tc1));
        
        TemplateConfigurer tc4 = new TemplateConfigurer();
        tc4.setCustomAttribute("k1", "v4");
        CA1.set("V4", tc4);
        
        tc1.merge(tc4);
        
        assertEquals("v4", tc1.getCustomAttribute("k1"));
        assertNull(tc1.getCustomAttribute("k2"));
        assertNull(tc1.getCustomAttribute("k3"));
        assertEquals("V4", CA1.get(tc1));
        assertNull(CA2.get(tc1));
        assertNull(CA3.get(tc1));
    }

    @Test
    public void testConfigureNonParserConfig() throws Exception {
        for (PropertyDescriptor pd : getTemplateConfigurerSettingPropDescs(false, true)) {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
    
            Object newValue = SETTING_ASSIGNMENTS.get(pd.getName());
            pd.getWriteMethod().invoke(tc, newValue);
            
            Template t = new Template(null, "", DEFAULT_CFG);
            Method tReaderMethod = t.getClass().getMethod(pd.getReadMethod().getName());
            
            assertNotEquals("For \"" + pd.getName() + "\"", newValue, tReaderMethod.invoke(t));
            tc.configure(t);
            assertEquals("For \"" + pd.getName() + "\"", newValue, tReaderMethod.invoke(t));
        }
    }
    
    @Test
    public void testConfigureCustomAttributes() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setCustomAttribute("k1", "c");
        cfg.setCustomAttribute("k2", "c");
        cfg.setCustomAttribute("k3", "c");

        TemplateConfigurer tc = new TemplateConfigurer();
        tc.setCustomAttribute("k2", "tc");
        tc.setCustomAttribute("k3", null);
        tc.setCustomAttribute("k4", "tc");
        tc.setCustomAttribute("k5", "tc");
        tc.setCustomAttribute("k6", "tc");
        CA1.set("tc", tc);
        CA2.set("tc", tc);
        CA3.set("tc", tc);

        Template t = new Template(null, "", cfg);
        t.setCustomAttribute("k5", "t");
        t.setCustomAttribute("k6", null);
        t.setCustomAttribute("k7", "t");
        CA2.set("t", t);
        CA3.set(null, t);
        CA4.set("t", t);
        
        tc.setParentConfiguration(cfg);
        tc.configure(t);
        
        assertEquals("c", t.getCustomAttribute("k1"));
        assertEquals("tc", t.getCustomAttribute("k2"));
        assertNull(t.getCustomAttribute("k3"));
        assertEquals("tc", t.getCustomAttribute("k4"));
        assertEquals("t", t.getCustomAttribute("k5"));
        assertNull(t.getCustomAttribute("k6"));
        assertEquals("t", t.getCustomAttribute("k7"));
        assertEquals("tc", CA1.get(t));
        assertEquals("t", CA2.get(t));
        assertNull(CA3.get(t));
        assertEquals("t", CA4.get(t));
    }
    
    @Test
    public void testConfigureParser() throws Exception {
        Set<String> testedProps = new HashSet<String>();
        
        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
            assertOutputWithoutAndWithTC(tc, "[#if true]y[/#if]", "[#if true]y[/#if]", "y");
            testedProps.add(Configuration.TAG_SYNTAX_KEY_CAMEL_CASE);
        }
        
        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
            assertOutputWithoutAndWithTC(tc, "<#if true>y<#elseif false>n</#if>", "y", null);
            testedProps.add(Configuration.NAMING_CONVENTION_KEY_CAMEL_CASE);
        }
        
        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setWhitespaceStripping(false);
            assertOutputWithoutAndWithTC(tc, "<#if true>\nx\n</#if>\n", "x\n", "\nx\n\n");
            testedProps.add(Configuration.WHITESPACE_STRIPPING_KEY_CAMEL_CASE);
        }

        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setArithmeticEngine(new DummyArithmeticEngine());
            assertOutputWithoutAndWithTC(tc, "${1} ${1+1}", "1 2", "11 22");
            testedProps.add(Configuration.ARITHMETIC_ENGINE_KEY_CAMEL_CASE);
        }

        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setOutputFormat(XMLOutputFormat.INSTANCE);
            assertOutputWithoutAndWithTC(tc, "${.outputFormat} ${\"a'b\"}",
                    UndefinedOutputFormat.INSTANCE.getName() + " a'b",
                    XMLOutputFormat.INSTANCE.getName() + " a&apos;b");
            testedProps.add(Configuration.OUTPUT_FORMAT_KEY_CAMEL_CASE);
        }

        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setOutputFormat(XMLOutputFormat.INSTANCE);
            tc.setAutoEscapingPolicy(Configuration.DISABLE_AUTO_ESCAPING_POLICY);
            assertOutputWithoutAndWithTC(tc, "${'a&b'}", "a&b", "a&b");
            testedProps.add(Configuration.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE);
        }
        
        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setStrictSyntaxMode(false);
            assertOutputWithoutAndWithTC(tc, "<if true>y</if>", "<if true>y</if>", "y");
            testedProps.add("strictSyntaxMode");
        }

        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(new Configuration(new Version(2, 3, 0)));
            assertOutputWithoutAndWithTC(tc, "<#foo>", null, "<#foo>");
            testedProps.add(Configuration.INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE);
        }


        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(new Configuration(new Version(2, 3, 0)));
            tc.setRecognizeStandardFileExtensions(true);
            assertOutputWithoutAndWithTC(tc, "${.outputFormat}",
                    UndefinedOutputFormat.INSTANCE.getName(), HTMLOutputFormat.INSTANCE.getName());
            testedProps.add(Configuration.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE);
        }
        
        assertEquals("Check that you have tested all parser settings; ", PARSER_PROP_NAMES, testedProps);
    }
    
    @Test
    public void testConfigureParserTooLowIcI() throws Exception {
        Configuration cfgWithTooLowIcI = new Configuration(Configuration.VERSION_2_3_21);
        for (PropertyDescriptor propDesc : getTemplateConfigurerSettingPropDescs(true, false)) {
            TemplateConfigurer tc = new TemplateConfigurer();

            String propName = propDesc.getName();
            Object value = SETTING_ASSIGNMENTS.get(propName);
            propDesc.getWriteMethod().invoke(tc, value);
            
            boolean shouldFail;
            if (CONFIGURABLE_PROP_NAMES.contains(propName)) {
                shouldFail = true;
            } else if (PARSER_PROP_NAMES.contains(propName)) {
                shouldFail = false;
            } else {
                fail("Uncategorized property: " + propName);
                return;
            }
            
            try {
                tc.setParentConfiguration(cfgWithTooLowIcI);
                if (shouldFail) {
                    fail("Should fail with property: " + propName);
                }
            } catch (IllegalStateException e) {
                if (!shouldFail) {
                    throw e;
                }
                assertThat(e.getMessage(), containsString("2.3.22"));
            }
        }
    }
    
    @Test
    public void testArithmeticEngine() throws TemplateException, IOException {
        TemplateConfigurer tc = new TemplateConfigurer();
        tc.setParentConfiguration(DEFAULT_CFG);
        tc.setArithmeticEngine(new DummyArithmeticEngine());
        assertOutputWithoutAndWithTC(tc,
                "<#setting locale='en_US'>${1} ${1+1} ${1*3} <#assign x = 1>${x + x} ${x * 3}",
                "1 2 3 2 3", "11 22 33 22 33");
        
        // Doesn't affect template.arithmeticEngine, only affects the parsing:
        Template t = new Template(null, null, new StringReader(""), DEFAULT_CFG, tc, null);
        assertEquals(DEFAULT_CFG.getArithmeticEngine(), t.getArithmeticEngine());
    }

    @Test
    public void testStringInterpolate() throws TemplateException, IOException {
        TemplateConfigurer tc = new TemplateConfigurer();
        tc.setParentConfiguration(DEFAULT_CFG);
        tc.setArithmeticEngine(new DummyArithmeticEngine());
        assertOutputWithoutAndWithTC(tc,
                "<#setting locale='en_US'>${'${1} ${1+1} ${1*3}'} <#assign x = 1>${'${x + x} ${x * 3}'}",
                "1 2 3 2 3", "11 22 33 22 33");
        
        // Doesn't affect template.arithmeticEngine, only affects the parsing:
        Template t = new Template(null, null, new StringReader(""), DEFAULT_CFG, tc, null);
        assertEquals(DEFAULT_CFG.getArithmeticEngine(), t.getArithmeticEngine());
    }
    
    @Test
    public void testInterpret() throws TemplateException, IOException {
        TemplateConfigurer tc = new TemplateConfigurer();
        tc.setParentConfiguration(DEFAULT_CFG);
        tc.setArithmeticEngine(new DummyArithmeticEngine());
        assertOutputWithoutAndWithTC(tc,
                "<#setting locale='en_US'><#assign src = r'${1} <#assign x = 1>${x + x}'><@src?interpret />",
                "1 2", "11 22");
        
        tc.setWhitespaceStripping(false);
        assertOutputWithoutAndWithTC(tc,
                "<#if true>\nX</#if><#assign src = r'<#if true>\nY</#if>'><@src?interpret />",
                "XY", "\nX\nY");
    }

    @Test
    public void testEval() throws TemplateException, IOException {
        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            tc.setArithmeticEngine(new DummyArithmeticEngine());
            assertOutputWithoutAndWithTC(tc,
                    "<#assign x = 1>${r'1 + x'?eval?c}",
                    "2", "22");
            assertOutputWithoutAndWithTC(tc,
                    "${r'1?c'?eval}",
                    "1", "11");
        }
        
        {
            TemplateConfigurer tc = new TemplateConfigurer();
            tc.setParentConfiguration(DEFAULT_CFG);
            String outputEncoding = "ISO-8859-2";
            tc.setOutputEncoding(outputEncoding);

            String legacyNCFtl = "${r'.output_encoding!\"null\"'?eval}";
            String camelCaseNCFtl = "${r'.outputEncoding!\"null\"'?eval}";

            // Default is re-auto-detecting in ?eval:
            assertOutputWithoutAndWithTC(tc, legacyNCFtl, "null", outputEncoding);
            assertOutputWithoutAndWithTC(tc, camelCaseNCFtl, "null", outputEncoding);
            
            // Force camelCase:
            tc.setNamingConvention(Configuration.CAMEL_CASE_NAMING_CONVENTION);
            assertOutputWithoutAndWithTC(tc, legacyNCFtl, "null", null);
            assertOutputWithoutAndWithTC(tc, camelCaseNCFtl, "null", outputEncoding);
            
            // Force legacy:
            tc.setNamingConvention(Configuration.LEGACY_NAMING_CONVENTION);
            assertOutputWithoutAndWithTC(tc, legacyNCFtl, "null", outputEncoding);
            assertOutputWithoutAndWithTC(tc, camelCaseNCFtl, "null", null);
        }
    }
    
    @Test
    public void testSetParentConfiguration() throws IOException {
        TemplateConfigurer tc = new TemplateConfigurer();
        
        Template t = new Template(null, "", DEFAULT_CFG);
        try {
            tc.configure(t);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Configuration"));
        }
        
        tc.setParent(DEFAULT_CFG);
        
        try {
            tc.setParentConfiguration(new Configuration());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Configuration"));
        }

        try {
            // Same as setParentConfiguration
            tc.setParent(new Configuration());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Configuration"));
        }
        
        try {
            tc.setParentConfiguration(null);
            fail();
        } catch (NullArgumentException e) {
            // exected
        }
        
        tc.setParent(DEFAULT_CFG);
        
        tc.configure(t);
    }
    
    private void assertOutputWithoutAndWithTC(TemplateConfigurer tc, String ftl, String expectedDefaultOutput,
            String expectedConfiguredOutput) throws TemplateException, IOException {
        assertOutput(tc, ftl, expectedConfiguredOutput);
        assertOutput(null, ftl, expectedDefaultOutput);
    }

    private void assertOutput(TemplateConfigurer tc, String ftl, String expectedConfiguredOutput)
            throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        try {
            Configuration cfg = tc != null ? tc.getParentConfiguration() : DEFAULT_CFG;
            Template t = new Template("adhoc.ftlh", null, new StringReader(ftl), cfg, tc, null);
            if (tc != null) {
                tc.configure(t);
            }
            t.process(null, sw);
            if (expectedConfiguredOutput == null) {
                fail("Template should have fail.");
            }
        } catch (TemplateException e) {
            if (expectedConfiguredOutput != null) {
                throw e;
            }
        } catch (ParseException e) {
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
        for (PropertyDescriptor pd : getTemplateConfigurerSettingPropDescs(true, true)) {
            TemplateConfigurer tc = new TemplateConfigurer();
            checkAllIsSetFalseExcept(tc, null);
            pd.getWriteMethod().invoke(tc, SETTING_ASSIGNMENTS.get(pd.getName()));
            checkAllIsSetFalseExcept(tc, pd.getName());
        }
    }

    private void checkAllIsSetFalseExcept(TemplateConfigurer tc, String setSetting)
            throws SecurityException, IntrospectionException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        for (PropertyDescriptor pd : getTemplateConfigurerSettingPropDescs(true, true)) {
            String isSetMethodName = getIsSetMethodName(pd.getReadMethod().getName());
            Method isSetMethod;
            try {
                isSetMethod = TemplateConfigurer.class.getMethod(isSetMethodName);
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
        for (PropertyDescriptor pd : getTemplateConfigurerSettingPropDescs(true, true)) {
            String propName = pd.getName();
            if (!SETTING_ASSIGNMENTS.containsKey(propName)) {
                fail("Test case doesn't cover all settings in SETTING_ASSIGNMENTS. Missing: " + propName);
            }
            Method readMethod = pd.getReadMethod();
            String cfgMethodName = readMethod.getName();
            if (cfgMethodName.equals("getEncoding")) {
                // Because Configuration has local-to-encoding map too, this has a different name there.
                cfgMethodName = "getDefaultEncoding";
            }
            Method cfgMethod = DEFAULT_CFG.getClass().getMethod(cfgMethodName, readMethod.getParameterTypes());
            Object defaultSettingValue = cfgMethod.invoke(DEFAULT_CFG);
            Object assignedValue = SETTING_ASSIGNMENTS.get(propName);
            assertNotEquals("SETTING_ASSIGNMENTS must contain a non-default value for " + propName,
                    assignedValue, defaultSettingValue);

            TemplateConfigurer tc = new TemplateConfigurer();
            try {
                pd.getWriteMethod().invoke(tc, assignedValue);
            } catch (Exception e) {
                throw new IllegalStateException("For setting \"" + propName + "\" and assigned value of type "
                        + (assignedValue != null ? assignedValue.getClass().getName() : "Null"),
                        e);
            }
        }
    }
    
}
