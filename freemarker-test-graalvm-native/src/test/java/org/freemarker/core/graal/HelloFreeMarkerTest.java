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

package org.freemarker.core.graal;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.FileExtensionMatcher;
import freemarker.cache.FirstMatchTemplateConfigurationFactory;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.OrMatcher;
import freemarker.cache.PathGlobMatcher;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ArithmeticEngine;
import freemarker.core.CSSOutputFormat;
import freemarker.core.Environment;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.JSONOutputFormat;
import freemarker.core.JavaScriptOutputFormat;
import freemarker.core.PlainTextOutputFormat;
import freemarker.core.RTFOutputFormat;
import freemarker.core.TemplateConfiguration;
import freemarker.core.TemplateDateFormat;
import freemarker.core.TemplateDateFormatFactory;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;
import freemarker.core.XHTMLOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.beans.BlacklistMemberAccessPolicy;
import freemarker.ext.beans.MemberSelectorListMemberAccessPolicy.MemberSelector;
import freemarker.ext.beans.WhitelistMemberAccessPolicy;
import freemarker.ext.dom.NodeModel;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

class HelloFreeMarkerTest {

    private static Configuration newConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setLocale(Locale.US);
        cfg.setTimeZone(TimeZone.getTimeZone("UTC"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return cfg;
    }

    private static String render(Configuration cfg, String templateSource, Object dataModel) throws Exception {
        Template template = new Template("inline", new StringReader(templateSource), cfg);
        try (StringWriter out = new StringWriter()) {
            template.process(dataModel, out);
            return out.toString();
        }
    }

    @Test
    void testMain() throws Exception {
        HelloFreeMarker.main(new String[0]);
        String output = HelloFreeMarker.executeHelloWorld();
        Assertions.assertTrue(output.contains("<title>Hello : FreeMarker GraalVM Native Demo</title>"));
        Assertions.assertTrue(output.contains("<h1>Hello : FreeMarker GraalVM Native Demo</h1>"));
        Assertions.assertTrue(output.contains(
                "Test template for Apache FreeMarker GraalVM native support (" + Configuration.getVersion() + ")"));
    }

    @Test
    void testFeaturesSuiteTemplate() throws Exception {
        Configuration cfg = newConfiguration();
        cfg.setClassForTemplateLoading(HelloDataModel.class, "/templates");

        HelloDataModel model = new HelloDataModel();
        model.setName("NativeSuite");
        model.setVersion(Configuration.getVersion().toString());
        model.setStatus(HelloDataModel.Status.ACTIVE);
        model.setItems(Arrays.asList(
                new HelloDataModel.Item("Keyboard", new BigDecimal("49.50"), true, Arrays.asList("hw", "usb")),
                new HelloDataModel.Item("Monitor", new BigDecimal("199.99"), false, Collections.singletonList("display")),
                new HelloDataModel.Item("Mouse", new BigDecimal("25.25"), true, Arrays.asList("hw", "wireless"))
        ));

        Template template = cfg.getTemplate("features-suite.ftlh");
        String output;
        try (StringWriter writer = new StringWriter()) {
            template.process(model, writer);
            output = writer.toString();
        }

        Assertions.assertTrue(output.contains("<header>[NativeSuite :: ACTIVE]</header>"));
        Assertions.assertTrue(output.contains("<span class=\"badge-primary\">NativeSuite</span>(PRIMARY)"));
        Assertions.assertTrue(output.contains("<li>1.odd:Keyboard=49.50[hw,usb]</li>"));
        Assertions.assertTrue(output.contains("<li>2.even:Monitor=199.99[display]</li>"));
        Assertions.assertTrue(output.contains("<li>3.odd:Mouse=25.25[hw,wireless]</li>"));
        Assertions.assertTrue(output.contains("TotalInStock=74.75"));
        Assertions.assertTrue(output.contains("OverloadInt=Box x3"));
        Assertions.assertTrue(output.contains("OverloadStr=Box (fragile)"));
    }

    @Test
    void testCoreDirectivesAndFlowControl() throws Exception {
        Configuration cfg = newConfiguration();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("score", 85);
        model.put("role", "editor");
        model.put("words", Arrays.asList("alpha", "beta", "gamma"));
        model.put("emptyList", Collections.emptyList());

        String templateSource = ""
                + "<#if (score >= 90)>A<#elseif (score >= 80)>B<#else>C</#if>|"
                + "<#switch role><#case 'admin'>ADM<#break><#case 'editor'>EDT<#break><#default>USR</#switch>|"
                + "<#list words>[<#items as w>${w?index}:${w?is_first?c}:${w?is_last?c}:${w}<#sep>,</#items>]<#else>NONE</#list>|"
                + "<#list emptyList>[<#items as w>${w}</#items>]<#else>EMPTY</#list>|"
                + "<#macro wrap prefix='(' suffix=')'><#list 1..2 as i>${prefix}<#nested i>${suffix}</#list></#macro>"
                + "<@wrap prefix='[' suffix=']'; idx>#${idx}</@wrap>|"
                + "<#function doubleVal n><#return n * 2></#function>${doubleVal(21)}|"
                + "<#assign captured><#compress>  hello   world  </#compress></#assign>${captured}|"
                + "<#attempt>ok<#recover>err</#attempt>:"
                + "<#attempt>${missingVar}<#recover>recovered:${.error?has_content?c}</#attempt>|"
                + "<#noparse>${notEvaluated}</#noparse>|"
                + "${missingVar!'defaultVal'}:${missingVar???c}:${score???c}";

        String actual = render(cfg, templateSource, model);
        Assertions.assertEquals(
                "B|EDT|[0:true:false:alpha,1:false:false:beta,2:false:true:gamma]|EMPTY|[#1][#2]|42|hello world|ok:recovered:true|${notEvaluated}|defaultVal:false:true",
                actual);
    }

    @Test
    void testRangesSlicesConcatenationAndKeyValIteration() throws Exception {
        Configuration cfg = newConfiguration();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("letters", Arrays.asList("a", "b", "c", "d", "e"));
        model.put("word", "FreeMarker");
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("x", 1);
        map1.put("y", 2);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("y", 20);
        map2.put("z", 30);
        model.put("m1", map1);
        model.put("m2", map2);

        String templateSource = ""
                + "inc=${(1..4)?join(',')}|"
                + "dec=${(4..1)?join(',')}|"
                + "excl=${(1..<4)?join(',')}|"
                + "len=${(10..*3)?join(',')}|"
                + "slice=${letters[1..3]?join('')}:${letters[3..]?join('')}:${word[0..<4]}|"
                + "seqConcat=${(['p'] + ['q', 'r'])?join('-')}|"
                + "hashConcat=<#assign m = m1 + m2><#list m as k, v>${k}=${v}<#sep>,</#list>|"
                + "loopCtrl=<#list 1..6 as n><#if n == 2><#continue></#if><#if n == 5><#break></#if>${n}</#list>";

        String actual = render(cfg, templateSource, model);
        Assertions.assertEquals(
                "inc=1,2,3,4|dec=4,3,2,1|excl=1,2,3|len=10,11,12|slice=bcd:de:Free|"
                        + "seqConcat=p-q-r|hashConcat=x=1,y=20,z=30|loopCtrl=134",
                actual);
    }

    @Test
    void testAdvancedMacrosVarargsAndRecursion() throws Exception {
        Configuration cfg = newConfiguration();
        String templateSource = ""
                + "<#macro tag name attrs...>"
                + "<${name}<#list attrs?keys?sort as k> ${k}=\"${attrs[k]}\"</#list>><#nested name?upper_case></${name}>"
                + "</#macro>"
                + "<@tag name='div' id='main' role='banner'; upper>${upper}</@tag>|"
                + "<#function sumAll first rest...>"
                + "<#local total = first>"
                + "<#list rest as r><#local total = total + r></#list>"
                + "<#return total>"
                + "</#function>"
                + "${sumAll(10, 20, 30, 40)}|"
                + "<#function factorial n>"
                + "<#if (n <= 1)><#return 1><#else><#return n * factorial(n - 1)></#if>"
                + "</#function>"
                + "fact5=${factorial(5)}|"
                + "<#macro greet who>Hi ${who}!</#macro>"
                + "<#assign targetMacro = 'greet'>"
                + "<@.vars[targetMacro] who='Native'/>";

        String actual = render(cfg, templateSource, Collections.emptyMap());
        Assertions.assertEquals(
                "<div id=\"main\" role=\"banner\">DIV</div>|100|fact5=120|Hi Native!",
                actual);
    }

    @Test
    void testStringNumberBooleanAndDateBuiltIns() throws Exception {
        Configuration cfg = newConfiguration();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("text", "  hello freeMarker world  ");
        model.put("path", "/api/v1/items?name=a&b");
        model.put("num", new BigDecimal("-42.678"));
        model.put("idx", 3);
        model.put("flag", true);
        model.put("epoch", Date.from(Instant.parse("2025-01-15T16:28:19Z")));
        model.put("sqlTs", Timestamp.from(Instant.parse("2025-01-15T16:28:19Z")));

        String templateSource = ""
                + "${text?trim?cap_first}|"
                + "${text?trim?capitalize}|"
                + "${text?trim?upper_case}|"
                + "${text?trim?replace('freeMarker', 'Native')}|"
                + "${text?trim?keep_before(' ')}:${text?trim?keep_after(' ')}|"
                + "${'7'?left_pad(3, '0')}:${'7'?right_pad(3, '_')}|"
                + "${'prefix'?ensure_starts_with('/')}:"
                + "${text?contains('freeMarker')?c}:${text?trim?starts_with('hello')?c}:${text?trim?ends_with('world')?c}|"
                + "${'a\"b'?json_string}|"
                + "${'a b'?url('UTF-8')}|"
                + "${num?abs?c}:${num?round?c}:${num?floor?c}:${num?ceiling?c}:${num?int?c}:${num?string('0.00')}|"
                + "${idx?lower_abc}:${idx?upper_abc}|"
                + "${flag?c}:${flag?string('YES', 'NO')}:${flag?then('T', 'F')}|"
                + "${epoch?datetime?string('yyyy-MM-dd HH:mm:ss')}|"
                + "${sqlTs?iso_utc}";

        String actual = render(cfg, templateSource, model);
        Assertions.assertEquals(
                "Hello freeMarker world|Hello Freemarker World|HELLO FREEMARKER WORLD|hello Native world|"
                        + "hello:freeMarker world|007:7__|/prefix:true:true:true|a\\\"b|a%20b|"
                        + "42.678:-43:-43:-42:-42:-42.68|c:C|true:YES:T|2025-01-15 16:28:19|2025-01-15T16:28:19Z",
                actual);
    }

    @Test
    void testSequenceHashLambdaAndDynamicEvalBuiltIns() throws Exception {
        Configuration cfg = newConfiguration();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("nums", Arrays.asList(5, 2, 8, 1, 9, 3));
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("alice", 10);
        scores.put("bob", 20);
        model.put("scores", scores);
        model.put("dynamicTpl", "Sum=${scores.alice + scores.bob}");

        String templateSource = ""
                + "size=${nums?size},first=${nums?first},last=${nums?last}|"
                + "sorted=${nums?sort?join(',')}|"
                + "rev=${nums?sort?reverse?join(',')}|"
                + "contains8=${nums?seq_contains(8)?c},idx8=${nums?seq_index_of(8)}|"
                + "filtered=${nums?filter(x -> x >= 5)?sort?join(',')}|"
                + "mapped=${nums?filter(x -> x <= 3)?sort?map(x -> x * 10)?join(',')}|"
                + "takeWhile=${nums?take_while(x -> x < 8)?join(',')}|"
                + "dropWhile=${nums?drop_while(x -> x < 8)?join(',')}|"
                + "chunks=<#list nums?chunk(4) as c>[${c?join(',')}]</#list>|"
                + "keys=${scores?keys?join(',')},vals=${scores?values?join(',')}|"
                + "eval=${'1 + 2 * 3'?eval}|"
                + "json=${'{\"k\": [10, 20]}'?eval_json.k?join('+')}|"
                + "interpret=<@dynamicTpl?interpret/>";

        String actual = render(cfg, templateSource, model);
        Assertions.assertEquals(
                "size=6,first=5,last=3|sorted=1,2,3,5,8,9|rev=9,8,5,3,2,1|contains8=true,idx8=2|"
                        + "filtered=5,8,9|mapped=10,20,30|takeWhile=5,2|dropWhile=8,1,9,3|chunks=[5,2,8,1][9,3]|"
                        + "keys=alice,bob,vals=10,20|eval=7|json=10+20|interpret=Sum=30",
                actual);
    }

    @Test
    void testBeansWrapperRecordsEnumsStaticsFieldsAndMemberAccessPolicies() throws Exception {
        Configuration cfg = newConfiguration();

        DefaultObjectWrapperBuilder owb = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_34);
        owb.setExposeFields(true);
        DefaultObjectWrapper ow = owb.build();
        cfg.setObjectWrapper(ow);

        HelloDataModel bean = new HelloDataModel();
        bean.setName("BeanTest");
        bean.setStatus(HelloDataModel.Status.PENDING);
        bean.setItems(Collections.singletonList(
                new HelloDataModel.Item("Book", new BigDecimal("12.00"), true, Collections.singletonList("edu"))
        ));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("bean", bean);
        root.put("enums", ow.getEnumModels());
        root.put("statics", ow.getStaticModels());

        String templateSource = ""
                + "field=${bean.exposedNote}|"
                + "record=${bean.items[0].title()}:${bean.items[0].price?c}|"
                + "enum=${(bean.status == enums['org.freemarker.core.graal.HelloDataModel$Status'].PENDING)?c}|"
                + "static=${statics['org.freemarker.core.graal.HelloDataModel'].staticGreeting('GraalVM')}|"
                + "overload=${bean.formatItem('Item', 2)}:${bean.formatItem('Item', 'ok')}";

        String actual = render(cfg, templateSource, root);
        Assertions.assertEquals(
                "field=public-field-ok|record=Book:12|enum=true|static=Hello from static, GraalVM!|overload=Item x2:Item (ok)",
                actual);

        // Also test LegacyDefaultMemberAccessPolicy (which loads unsafeMethods.properties)
        Configuration legacyCfg = new Configuration(Configuration.VERSION_2_3_29);
        BeansWrapper legacyWrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_29).build();
        legacyCfg.setObjectWrapper(legacyWrapper);
        Assertions.assertEquals("BeanTest", render(legacyCfg, "${name}", bean));
    }

    @Test
    void testWhitelistAndBlacklistMemberAccessPolicies() throws Exception {
        HelloDataModel bean = new HelloDataModel();
        bean.setName("PolicyUser");

        // 1. WhitelistMemberAccessPolicy: allows @TemplateAccessible methods (getName, whitelistedAction),
        //    blocks unannotated getRestrictedSecret()
        DefaultObjectWrapperBuilder whitelistOwb = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_34);
        whitelistOwb.setMemberAccessPolicy(new WhitelistMemberAccessPolicy(Collections.<MemberSelector>emptyList()));
        Configuration whitelistCfg = newConfiguration();
        whitelistCfg.setObjectWrapper(whitelistOwb.build());

        String whitelistOut = render(
                whitelistCfg,
                "${name}|${whitelistedAction()}|${restrictedSecret!'BLOCKED'}",
                bean);
        Assertions.assertEquals("PolicyUser|allowed:PolicyUser|BLOCKED", whitelistOut);

        // 2. BlacklistMemberAccessPolicy: explicitly blacklist getRestrictedSecret()
        MemberSelector blockedSelector = MemberSelector.parse(
                HelloDataModel.class.getName() + ".getRestrictedSecret()",
                HelloDataModel.class.getClassLoader());
        DefaultObjectWrapperBuilder blacklistOwb = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_34);
        blacklistOwb.setMemberAccessPolicy(new BlacklistMemberAccessPolicy(Collections.singletonList(blockedSelector)));
        Configuration blacklistCfg = newConfiguration();
        blacklistCfg.setObjectWrapper(blacklistOwb.build());

        String blacklistOut = render(
                blacklistCfg,
                "${name}|${formatItem('Bag', 1)}|${restrictedSecret!'DENIED'}",
                bean);
        Assertions.assertEquals("PolicyUser|Bag x1|DENIED", blacklistOut);
    }

    @Test
    void testMultiTemplateLoaderAndOutputFormats() throws Exception {
        Configuration cfg = newConfiguration();
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate("dynamic.ftlh", "<p>${unsafe}&</p><#noautoesc>${raw}</#noautoesc>|${unsafe?no_esc}");
        stringLoader.putTemplate("xml-doc.ftlx", "<node attr=\"${unsafe}\"/>");
        stringLoader.putTemplate(
                "mixed-formats.ftl",
                "<#ftl output_format='HTML'>${unsafe}|<#outputformat 'XML'>${unsafe}</#outputformat>");

        ClassTemplateLoader classLoader = new ClassTemplateLoader(HelloDataModel.class, "/templates");
        cfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[] { stringLoader, classLoader }));
        cfg.setRecognizeStandardFileExtensions(true);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("name", "LoaderTest");
        model.put("status", HelloDataModel.Status.ARCHIVED);
        model.put("unsafe", "<script>&\"'</script>");
        model.put("raw", "<b>bold</b>");

        try (StringWriter htmlOut = new StringWriter()) {
            Template htmlTpl = cfg.getTemplate("dynamic.ftlh");
            Assertions.assertSame(HTMLOutputFormat.INSTANCE, htmlTpl.getOutputFormat());
            htmlTpl.process(model, htmlOut);
            Assertions.assertEquals(
                    "<p>&lt;script&gt;&amp;&quot;&#39;&lt;/script&gt;&</p><b>bold</b>|<script>&\"'</script>",
                    htmlOut.toString());
        }

        try (StringWriter xmlOut = new StringWriter()) {
            Template xmlTpl = cfg.getTemplate("xml-doc.ftlx");
            Assertions.assertSame(XMLOutputFormat.INSTANCE, xmlTpl.getOutputFormat());
            xmlTpl.process(model, xmlOut);
            Assertions.assertEquals("<node attr=\"&lt;script&gt;&amp;&quot;&apos;&lt;/script&gt;\"/>", xmlOut.toString());
        }

        try (StringWriter mixedOut = new StringWriter()) {
            cfg.getTemplate("mixed-formats.ftl").process(model, mixedOut);
            Assertions.assertEquals(
                    "&lt;script&gt;&amp;&quot;&#39;&lt;/script&gt;|&lt;script&gt;&amp;&quot;&apos;&lt;/script&gt;",
                    mixedOut.toString());
        }

        try (StringWriter headerOut = new StringWriter()) {
            cfg.getTemplate("included-header.ftlh").process(model, headerOut);
            Assertions.assertEquals("<header>[LoaderTest :: ARCHIVED]</header>\n", headerOut.toString().replace("\r\n", "\n"));
        }

        Assertions.assertEquals("JSON", JSONOutputFormat.INSTANCE.getName());
        Assertions.assertEquals("XHTML", XHTMLOutputFormat.INSTANCE.getName());
        Assertions.assertEquals("RTF", RTFOutputFormat.INSTANCE.getName());
        Assertions.assertEquals("JavaScript", JavaScriptOutputFormat.INSTANCE.getName());
        Assertions.assertEquals("CSS", CSSOutputFormat.INSTANCE.getName());
        Assertions.assertEquals("plainText", PlainTextOutputFormat.INSTANCE.getName());
    }

    @Test
    void testCustomDirectivesAndMethods() throws Exception {
        Configuration cfg = newConfiguration();

        TemplateMethodModelEx repeatMethod = new TemplateMethodModelEx() {
            @Override
            public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
                String text = ((TemplateScalarModel) arguments.get(0)).getAsString();
                int count = ((TemplateNumberModel) arguments.get(1)).getAsNumber().intValue();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    if (i > 0) {
                        sb.append('-');
                    }
                    sb.append(text);
                }
                return new SimpleScalar(sb.toString());
            }
        };

        TemplateDirectiveModel repeatDirective = new TemplateDirectiveModel() {
            @Override
            public void execute(
                    Environment env,
                    @SuppressWarnings("rawtypes") Map params,
                    TemplateModel[] loopVars,
                    TemplateDirectiveBody body) throws TemplateException, IOException {
                int times = ((TemplateNumberModel) params.get("times")).getAsNumber().intValue();
                Writer out = env.getOut();
                for (int i = 1; i <= times; i++) {
                    if (loopVars.length > 0) {
                        loopVars[0] = new SimpleNumber(i);
                    }
                    body.render(out);
                }
            }
        };

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("repeatFn", repeatMethod);
        root.put("repeatDir", repeatDirective);

        String templateSource = "${repeatFn('FM', 3)}|<@repeatDir times=3; idx>[${idx}]</@repeatDir>";
        String actual = render(cfg, templateSource, root);
        Assertions.assertEquals("FM-FM-FM|[1][2][3]", actual);
    }

    @Test
    void testCustomNumberAndDateFormats() throws Exception {
        Configuration cfg = newConfiguration();

        TemplateNumberFormatFactory hexFormatFactory = new TemplateNumberFormatFactory() {
            @Override
            public TemplateNumberFormat get(String params, Locale locale, Environment env)
                    throws TemplateValueFormatException {
                TemplateFormatUtil.checkHasNoParameters(params);
                return new TemplateNumberFormat() {
                    @Override
                    public String formatToPlainText(TemplateNumberModel numberModel)
                            throws TemplateValueFormatException, TemplateModelException {
                        return "0x" + Integer.toHexString(TemplateFormatUtil.getNonNullNumber(numberModel).intValue()).toUpperCase(Locale.ROOT);
                    }

                    @Override
                    public boolean isLocaleBound() {
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "hex number format";
                    }
                };
            }
        };

        TemplateDateFormatFactory epochMillisFormatFactory = new TemplateDateFormatFactory() {
            @Override
            public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput, Environment env)
                    throws TemplateValueFormatException {
                TemplateFormatUtil.checkHasNoParameters(params);
                return new TemplateDateFormat() {
                    @Override
                    public String formatToPlainText(TemplateDateModel dateModel)
                            throws TemplateValueFormatException, TemplateModelException {
                        return "ms:" + TemplateFormatUtil.getNonNullDate(dateModel).getTime();
                    }

                    @Override
                    public Object parse(String s, int dateType) {
                        return new Date(Long.parseLong(s));
                    }

                    @Override
                    public boolean isLocaleBound() {
                        return false;
                    }

                    @Override
                    public boolean isTimeZoneBound() {
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "epoch millis date format";
                    }
                };
            }
        };

        cfg.setCustomNumberFormats(Collections.singletonMap("hex", hexFormatFactory));
        cfg.setCustomDateFormats(Collections.singletonMap("epochMillis", epochMillisFormatFactory));

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("n", 255);
        model.put("d", new Date(1700000000000L));

        String templateSource = "${n?string.@hex}|${d?datetime?string.@epochMillis}|<#setting number_format='@hex'>${4095}";
        Assertions.assertEquals("0xFF|ms:1700000000000|0xFFF", render(cfg, templateSource, model));
    }

    @Test
    void testTemplateConfigurationsAndMatchers() throws Exception {
        Configuration cfg = newConfiguration();
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("html/page.inc", "<p>${v}</p>");
        loader.putTemplate("reports/data.csv", "${flag}|${num}");
        cfg.setTemplateLoader(loader);

        TemplateConfiguration htmlTc = new TemplateConfiguration();
        htmlTc.setOutputFormat(HTMLOutputFormat.INSTANCE);

        TemplateConfiguration csvTc = new TemplateConfiguration();
        csvTc.setBooleanFormat("1,0");
        csvTc.setNumberFormat("0.000");

        cfg.setTemplateConfigurations(new FirstMatchTemplateConfigurationFactory(
                new ConditionalTemplateConfigurationFactory(
                        new OrMatcher(new FileExtensionMatcher("inc"), new PathGlobMatcher("html/**")),
                        htmlTc),
                new ConditionalTemplateConfigurationFactory(
                        new PathGlobMatcher("reports/*.csv"),
                        csvTc)
        ));

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("v", "<tag>");
        model.put("flag", true);
        model.put("num", 3.5);

        try (StringWriter w1 = new StringWriter(); StringWriter w2 = new StringWriter()) {
            cfg.getTemplate("html/page.inc").process(model, w1);
            cfg.getTemplate("reports/data.csv").process(model, w2);
            Assertions.assertEquals("<p>&lt;tag&gt;</p>", w1.toString());
            Assertions.assertEquals("1|3.500", w2.toString());
        }
    }

    @Test
    void testAutoIncludesAutoImportsAndSpecialVariables() throws Exception {
        Configuration cfg = newConfiguration();
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("common/auto-header.ftl", "[AUTO:${appTitle}]");
        loader.putTemplate("common/auto-util.ftl", "<#function fmt x><#return '<' + x + '>'></#function>");
        loader.putTemplate(
                "main-view.ftl",
                "|${u.fmt('val')}|${.current_template_name}|${.main_template_name}|${.data_model.appTitle}|${.version?has_content?c}");
        cfg.setTemplateLoader(loader);
        cfg.setAutoIncludes(Collections.singletonList("common/auto-header.ftl"));
        cfg.setAutoImports(Collections.singletonMap("u", "common/auto-util.ftl"));
        cfg.setLazyAutoImports(true);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("appTitle", "NativeApp");

        try (StringWriter out = new StringWriter()) {
            cfg.getTemplate("main-view.ftl").process(model, out);
            Assertions.assertEquals(
                    "[AUTO:NativeApp]|<val>|main-view.ftl|main-view.ftl|NativeApp|true",
                    out.toString());
        }
    }

    @Test
    void testArithmeticEnginesAndExceptionHandlers() throws Exception {
        Configuration cfg = newConfiguration();
        cfg.setArithmeticEngine(ArithmeticEngine.CONSERVATIVE_ENGINE);
        Assertions.assertEquals(
                "10.50|12.35",
                render(cfg, "${(10 + 0.5)?string('0.00')}|${12.345?string('0.##;; roundingMode=halfUp')}", Collections.emptyMap()));

        Configuration ignoreCfg = newConfiguration();
        ignoreCfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        Assertions.assertEquals("before||after", render(ignoreCfg, "before|${missingVar}|after", Collections.emptyMap()));

        Configuration customHandlerCfg = newConfiguration();
        customHandlerCfg.setTemplateExceptionHandler(new TemplateExceptionHandler() {
            @Override
            public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
                try {
                    out.write("[ERR:" + te.getBlamedExpressionString() + "]");
                } catch (IOException e) {
                    throw new TemplateException("Failed to write error", e, env);
                }
            }
        });
        Assertions.assertEquals("A|[ERR:badRef]|B", render(customHandlerCfg, "A|${badRef}|B", Collections.emptyMap()));
    }

    @Test
    void testXmlDomNodeModel() throws Exception {
        String xml = "<library name=\"Main\">"
                + "<book id=\"b1\"><title>FreeMarker In Action</title><author>ASF</author></book>"
                + "<book id=\"b2\"><title>GraalVM Native</title><author>Oracle</author></book>"
                + "</library>";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("doc", NodeModel.wrap(doc));

        String templateSource = ""
                + "${doc.library.@name}:"
                + "<#list doc.library.book as b>[${b.@id}:${b.title}(${b.author})]</#list>|"
                + "<#visit doc.library>"
                + "<#macro library><#recurse></#macro>"
                + "<#macro book>${.node.@id}=${.node.title};</#macro>";

        Configuration cfg = newConfiguration();
        String actual = render(cfg, templateSource, root);
        Assertions.assertEquals(
                "Main:[b1:FreeMarker In Action(ASF)][b2:GraalVM Native(Oracle)]|b1=FreeMarker In Action;b2=GraalVM Native;",
                actual);
    }

    @Test
    void testConfigurationSettingsAndLogger() throws Exception {
        Configuration cfg = newConfiguration();
        Properties props = new Properties();
        props.setProperty(Configuration.LOCALE_KEY, "en_US");
        props.setProperty(Configuration.TIME_ZONE_KEY, "UTC");
        props.setProperty(Configuration.NUMBER_FORMAT_KEY, "0.00");
        props.setProperty(Configuration.BOOLEAN_FORMAT_KEY, "yes,no");
        props.setProperty(Configuration.ARITHMETIC_ENGINE_KEY, "bigdecimal");
        props.setProperty(Configuration.TEMPLATE_EXCEPTION_HANDLER_KEY, "rethrow");
        props.setProperty(Configuration.API_BUILTIN_ENABLED_KEY, "true");
        props.setProperty(Configuration.OUTPUT_FORMAT_KEY, "HTML");
        cfg.setSettings(props);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("n", 12.3);
        model.put("b", true);
        model.put("html", "<a>");

        Assertions.assertEquals("12.30|yes|&lt;a&gt;", render(cfg, "${n}|${b}|${html}", model));

        Logger logger = Logger.getLogger(HelloFreeMarkerTest.class.getName());
        Assertions.assertNotNull(logger);
        logger.debug("Testing FreeMarker Logger under GraalVM Native Image");

        Assertions.assertNotNull(Class.forName("freemarker.ext.jython.JythonModel"));
        Assertions.assertNotNull(Class.forName("freemarker.ext.jython.JythonWrapper"));
    }

}
