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

package freemarker.test.templatesuite;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.AssertionFailedError;

import org.junit.Ignore;
import org.xml.sax.InputSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import freemarker.core.ASTPrinter;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BooleanModel;
import freemarker.ext.beans.Java7MembersOnlyBeansWrapper;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultNonListCollectionAdapter;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleNumber;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;
import freemarker.test.templatesuite.models.BooleanAndStringTemplateModel;
import freemarker.test.templatesuite.models.BooleanHash1;
import freemarker.test.templatesuite.models.BooleanHash2;
import freemarker.test.templatesuite.models.BooleanList1;
import freemarker.test.templatesuite.models.BooleanList2;
import freemarker.test.templatesuite.models.BooleanVsStringMethods;
import freemarker.test.templatesuite.models.JavaObjectInfo;
import freemarker.test.templatesuite.models.Listables;
import freemarker.test.templatesuite.models.MultiModel1;
import freemarker.test.templatesuite.models.OverloadedMethods;
import freemarker.test.templatesuite.models.OverloadedMethods2;
import freemarker.test.templatesuite.models.VarArgTestModel;
import freemarker.test.utility.AssertDirective;
import freemarker.test.utility.AssertEqualsDirective;
import freemarker.test.utility.AssertFailsDirective;
import freemarker.test.utility.FileTestCase;
import freemarker.test.utility.NoOutputDirective;

/**
 * Instances of this are created and called by {@link TemplateTestSuite}. (It's on "Ignore" so that Eclipse doesn't try
 * to run this alone.) 
 */
@Ignore
public class TemplateTestCase extends FileTestCase {
    
    // Name of variables exposed to all test FTL-s:
    private static final String ICI_INT_VALUE_VAR_NAME = "iciIntValue";
    private static final String TEST_NAME_VAR_NAME = "testName";
    private static final String JAVA_OBJECT_INFO_VAR_NAME = "javaObjectInfo";
    private static final String NO_OUTPUT_VAR_NAME = "noOutput";
    private static final String ASSERT_FAILS_VAR_NAME = "assertFails";
    private static final String ASSERT_EQUALS_VAR_NAME = "assertEquals";
    private static final String ASSERT_VAR_NAME = "assert";
    
    private final String simpleTestName;
    private final String templateName;
    private final String expectedFileName;
    private final boolean noOutput;
    
    private final Configuration conf;
    private final HashMap dataModel = new HashMap();
    
    public TemplateTestCase(String testName, String simpleTestName, String templateName, String expectedFileName, boolean noOutput,
            Version incompatibleImprovements) {
        super(testName);
        NullArgumentException.check("testName", testName);
        
        NullArgumentException.check("simpleTestName", simpleTestName);
        this.simpleTestName = simpleTestName;
        
        NullArgumentException.check("templateName", templateName);
        this.templateName = templateName;
        
        NullArgumentException.check("expectedFileName", expectedFileName);
        this.expectedFileName = expectedFileName;
        
        this.noOutput = noOutput;
        
        conf = new Configuration(incompatibleImprovements);
    }
    
    public void setSetting(String param, String value) throws IOException {
        if ("auto_import".equals(param)) {
            StringTokenizer st = new StringTokenizer(value);
            if (!st.hasMoreTokens()) fail("Expecting libname");
            String libname = st.nextToken();
            if (!st.hasMoreTokens()) fail("Expecting 'as <alias>' in autoimport");
            String as = st.nextToken();
            if (!as.equals("as")) fail("Expecting 'as <alias>' in autoimport");
            if (!st.hasMoreTokens()) fail("Expecting alias after 'as' in autoimport");
            String alias = st.nextToken();
            conf.addAutoImport(alias, libname);
        } else if ("clear_encoding_map".equals(param)) {
            if (StringUtil.getYesNo(value)) {
                conf.clearEncodingMap();
            }
        } else if ("input_encoding".equals(param)) {
            conf.setDefaultEncoding(value);
        // INCOMPATIBLE_IMPROVEMENTS is a list here, and was already set in the constructor.
        } else if (!Configuration.INCOMPATIBLE_IMPROVEMENTS.equals(param)) {
            try {
                conf.setSetting(param, value);
            } catch (TemplateException e) {
                throw new RuntimeException(
                        "Failed to set setting " +
                        StringUtil.jQuote(param) + " to " +
                        StringUtil.jQuote(value) + "; see cause exception.",
                        e);
            }
        }
    }
    
    /*
     * This method just contains all the code to seed the data model 
     * ported over from the individual classes. This seems ugly and unnecessary.
     * We really might as well just expose pretty much 
     * the same tree to all our tests. (JR)
     */
    @SuppressWarnings("boxing")
    public void setUp() throws Exception {
        conf.setDirectoryForTemplateLoading(new File(getTestClassDirectory(), "templates"));
        
        dataModel.put(ASSERT_VAR_NAME, AssertDirective.INSTANCE);
        dataModel.put(ASSERT_EQUALS_VAR_NAME, AssertEqualsDirective.INSTANCE);
        dataModel.put(ASSERT_FAILS_VAR_NAME, AssertFailsDirective.INSTANCE);
        dataModel.put(NO_OUTPUT_VAR_NAME, NoOutputDirective.INSTANCE);

        dataModel.put(JAVA_OBJECT_INFO_VAR_NAME, JavaObjectInfo.INSTANCE);
        dataModel.put(TEST_NAME_VAR_NAME, simpleTestName);
        dataModel.put(ICI_INT_VALUE_VAR_NAME, conf.getIncompatibleImprovements().intValue());
        
        dataModel.put("message", "Hello, world!");

        if (simpleTestName.startsWith("api-builtin")) {
            dataModel.put("map", ImmutableMap.of(1, "a", 2, "b", 3, "c"));
            dataModel.put("list", ImmutableList.of(1, 2, 3));
            dataModel.put("set", ImmutableSet.of("a", "b", "c"));
            dataModel.put("s", "test");
        } else if (simpleTestName.equals("bean-maps")) {
            BeansWrapper w1 = new Java7MembersOnlyBeansWrapper();
            BeansWrapper w2 = new Java7MembersOnlyBeansWrapper();
            BeansWrapper w3 = new Java7MembersOnlyBeansWrapper();
            BeansWrapper w4 = new Java7MembersOnlyBeansWrapper();
            BeansWrapper w5 = new Java7MembersOnlyBeansWrapper();
            BeansWrapper w6 = new Java7MembersOnlyBeansWrapper();
            BeansWrapper w7 = new Java7MembersOnlyBeansWrapper();
    
            w1.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            w2.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            w3.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
            w4.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
            w5.setExposureLevel(BeansWrapper.EXPOSE_ALL);
            w6.setExposureLevel(BeansWrapper.EXPOSE_ALL);
    
            w1.setMethodsShadowItems(true);
            w2.setMethodsShadowItems(false);
            w3.setMethodsShadowItems(true);
            w4.setMethodsShadowItems(false);
            w5.setMethodsShadowItems(true);
            w6.setMethodsShadowItems(false);
    
            w7.setSimpleMapWrapper(true);
    
            Object test = getTestMapBean();
    
            dataModel.put("m1", w1.wrap(test));
            dataModel.put("m2", w2.wrap(test));
            dataModel.put("m3", w3.wrap(test));
            dataModel.put("m4", w4.wrap(test));
            dataModel.put("m5", w5.wrap(test));
            dataModel.put("m6", w6.wrap(test));
            dataModel.put("m7", w7.wrap(test));
    
            dataModel.put("s1", w1.wrap("hello"));
            dataModel.put("s2", w1.wrap("world"));
            dataModel.put("s3", w5.wrap("hello"));
            dataModel.put("s4", w5.wrap("world"));
        }
        
        else if (simpleTestName.equals("beans")) {
            dataModel.put("array", new String[] { "array-0", "array-1"});
            dataModel.put("list", Arrays.asList(new String[] { "list-0", "list-1", "list-2"}));
            Map tmap = new HashMap();
            tmap.put("key", "value");
            Object objKey = new Object();
            tmap.put(objKey, "objValue");
            dataModel.put("map", tmap);
            dataModel.put("objKey", objKey);
            dataModel.put("obj", new freemarker.test.templatesuite.models.BeanTestClass());
            dataModel.put("resourceBundle", new ResourceBundleModel(ResourceBundle.getBundle("freemarker.test.templatesuite.models.BeansTestResources"), BeansWrapper.getDefaultInstance()));
            dataModel.put("date", new GregorianCalendar(1974, 10, 14).getTime());
            dataModel.put("statics", BeansWrapper.getDefaultInstance().getStaticModels());
            dataModel.put("enums", BeansWrapper.getDefaultInstance().getEnumModels());
        }
        
        else if (simpleTestName.equals("boolean")) {
            dataModel.put( "boolean1", TemplateBooleanModel.FALSE);
            dataModel.put( "boolean2", TemplateBooleanModel.TRUE);
            dataModel.put( "boolean3", TemplateBooleanModel.TRUE);
            dataModel.put( "boolean4", TemplateBooleanModel.TRUE);
            dataModel.put( "boolean5", TemplateBooleanModel.FALSE);
            
            dataModel.put( "list1", new BooleanList1() );
            dataModel.put( "list2", new BooleanList2() );
    
            dataModel.put( "hash1", new BooleanHash1() );
            dataModel.put( "hash2", new BooleanHash2() );
        }
        
        else if (simpleTestName.startsWith("dateformat")) {
            GregorianCalendar cal = new GregorianCalendar(2002, 10, 15, 14, 54, 13);
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            dataModel.put("date", new SimpleDate(cal.getTime(), TemplateDateModel.DATETIME));
            dataModel.put("unknownDate", new SimpleDate(cal.getTime(), TemplateDateModel.UNKNOWN));
            dataModel.put("javaGMT02", TimeZone.getTimeZone("GMT+02"));
            dataModel.put("javaUTC", TimeZone.getTimeZone("UTC"));
            dataModel.put("adaptedToStringScalar", new Object() {
                public String toString() {
                    return "GMT+02";
                }
            });
            dataModel.put("sqlDate", new java.sql.Date(1273955885023L));
            dataModel.put("sqlTime", new java.sql.Time(74285023L));
        }

        else if (templateName.equals("list.ftl")
                || templateName.equals("list2.ftl") || templateName.equals("list3.ftl")) {
            dataModel.put("listables", new Listables());
        }
        
        else if (simpleTestName.startsWith("number-format")) {
            dataModel.put("int", new SimpleNumber(new Integer(1)));
            dataModel.put("double", new SimpleNumber(new Double(1.0)));
            dataModel.put("double2", new SimpleNumber(new Double(1 + 1e-15)));
            dataModel.put("double3", new SimpleNumber(new Double(1e-16)));
            dataModel.put("double4", new SimpleNumber(new Double(-1e-16)));
            dataModel.put("bigDecimal", new SimpleNumber(java.math.BigDecimal.valueOf(1)));
            dataModel.put("bigDecimal2", new SimpleNumber(java.math.BigDecimal.valueOf(1, 16)));
        }
        
        else if (simpleTestName.equals("simplehash-char-key")) {
            HashMap mStringC = new HashMap();
            mStringC.put("c", "string");
            dataModel.put("mStringC", mStringC);
            
            HashMap mStringCNull = new HashMap();
            mStringCNull.put("c", null);
            dataModel.put("mStringCNull", mStringCNull);
            
            HashMap mCharC = new HashMap();
            mCharC.put(Character.valueOf('c'), "char");
            dataModel.put("mCharC", mCharC);
            
            HashMap mCharCNull = new HashMap();
            mCharCNull.put("c", null);
            dataModel.put("mCharCNull", mCharCNull);
            
            HashMap mMixed = new HashMap();
            mMixed.put(Character.valueOf('c'), "char");
            mMixed.put("s", "string");
            mMixed.put("s2", "string2");
            mMixed.put("s2n", null);
            dataModel.put("mMixed", mMixed);
        }
    
        else if (simpleTestName.equals("default-xmlns")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/defaultxmlns1.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (simpleTestName.equals("multimodels")) {
            dataModel.put("test", "selftest");
            dataModel.put("self", "self");
            dataModel.put("zero", new Integer(0));
            dataModel.put("data", new MultiModel1());
        }
        
        else if (simpleTestName.equals("stringbimethods")) {
            dataModel.put("multi", new TestBoolean());
        }
        
        else if (simpleTestName.startsWith("type-builtins")) {
            dataModel.put("testmethod", new TestMethod());
            dataModel.put("testnode", new TestNode());
            dataModel.put("testcollection", new SimpleCollection(new ArrayList()));
            dataModel.put("testcollectionEx", DefaultNonListCollectionAdapter.adapt(new HashSet(), null));
            dataModel.put("bean", new TestBean());
        }

        else if (simpleTestName.equals("date-type-builtins")) {
            GregorianCalendar cal = new GregorianCalendar(2003, 4 - 1, 5, 6, 7, 8);
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = cal.getTime();
            dataModel.put("unknown", d);
            dataModel.put("timeOnly", new java.sql.Time(d.getTime()));
            dataModel.put("dateOnly", new java.sql.Date(d.getTime()));
            dataModel.put("dateTime", new java.sql.Timestamp(d.getTime()));
        }
        
        else if (simpleTestName.equals("var-layers")) {
            dataModel.put("x", new Integer(4));
            dataModel.put("z", new Integer(4));
            conf.setSharedVariable("y", new Integer(7));
        }
        
        else if (simpleTestName.equals("xml-fragment")) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            DocumentBuilder db = f.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new InputSource(getClass().getResourceAsStream("models/xmlfragment.xml")));
            dataModel.put("node", NodeModel.wrap(doc.getDocumentElement().getFirstChild().getFirstChild()));
        }
        
        else if (simpleTestName.equals("xmlns1")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (simpleTestName.equals("xmlns2")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns2.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (simpleTestName.equals("xmlns3") || simpleTestName.equals("xmlns4")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns3.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        else if (simpleTestName.equals("xmlns5")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/defaultxmlns1.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        else if (simpleTestName.equals("xml-ns_prefix-scope")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xml-ns_prefix-scope.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (simpleTestName.startsWith("sequence-builtins")) {
            Set abcSet = new TreeSet();
            abcSet.add("a");
            abcSet.add("b");
            abcSet.add("c");
            dataModel.put("abcSet", abcSet);
            
            List listWithNull = new ArrayList();
            listWithNull.add("a");
            listWithNull.add(null);
            listWithNull.add("c");
            dataModel.put("listWithNull", listWithNull);
            
            List listWithNullsOnly = new ArrayList();
            listWithNull.add(null);
            listWithNull.add(null);
            listWithNull.add(null);
            dataModel.put("listWithNullsOnly", listWithNullsOnly);
            
            dataModel.put("abcCollection", new SimpleCollection(abcSet));
            
            Set set = new HashSet();
            set.add("a");
            set.add("b");
            set.add("c");
            dataModel.put("set", set);
        }
        
        else if (simpleTestName.equals("number-to-date")) {
          dataModel.put("bigInteger", new BigInteger("1305575275540"));
          dataModel.put("bigDecimal", new BigDecimal("1305575275539.5"));
        }
        
        else if (simpleTestName.equals("varargs")) {
          dataModel.put("m", new VarArgTestModel());
        }
        
        else if (simpleTestName.startsWith("overloaded-methods-") && !simpleTestName.startsWith("overloaded-methods-2-")) {
          dataModel.put("obj", new OverloadedMethods());
        }
        
        else if (simpleTestName.startsWith("boolean-formatting")) {
          dataModel.put("beansBoolean", new BooleanModel(Boolean.TRUE, (BeansWrapper) conf.getObjectWrapper()));
          dataModel.put("booleanAndString", new BooleanAndStringTemplateModel());
          dataModel.put("booleanVsStringMethods", new BooleanVsStringMethods());
        }
        
        else if (simpleTestName.startsWith("number-math-builtins")) {
            dataModel.put("fNan", Float.valueOf(Float.NaN));
            dataModel.put("dNan", Double.valueOf(Double.NaN));
            dataModel.put("fNinf", Float.valueOf(Float.NEGATIVE_INFINITY));
            dataModel.put("dPinf", Double.valueOf(Double.POSITIVE_INFINITY));
            
            dataModel.put("fn", Float.valueOf(-0.05f));
            dataModel.put("dn", Double.valueOf(-0.05));
            dataModel.put("ineg", Integer.valueOf(-5));
            dataModel.put("ln", Long.valueOf(-5));
            dataModel.put("sn", Short.valueOf((short) -5));
            dataModel.put("bn", Byte.valueOf((byte) -5));
            dataModel.put("bin", BigInteger.valueOf(5));
            dataModel.put("bdn", BigDecimal.valueOf(-0.05));
            
            dataModel.put("fp", Float.valueOf(0.05f));
            dataModel.put("dp", Double.valueOf(0.05));
            dataModel.put("ip", Integer.valueOf(5));
            dataModel.put("lp", Long.valueOf(5));
            dataModel.put("sp", Short.valueOf((short) 5));
            dataModel.put("bp", Byte.valueOf((byte) 5));
            dataModel.put("bip", BigInteger.valueOf(5));
            dataModel.put("bdp", BigDecimal.valueOf(0.05));
          }
          
        else if (simpleTestName.startsWith("classic-compatible")) {
            dataModel.put("array", new String[] { "a", "b", "c" });
            dataModel.put("beansArray", new BeansWrapper().wrap(new String[] { "a", "b", "c" }));
            dataModel.put("beanTrue", new BeansWrapper().wrap(Boolean.TRUE));
            dataModel.put("beanFalse", new BeansWrapper().wrap(Boolean.FALSE));
        }
        
        else if (simpleTestName.startsWith("overloaded-methods-2-")) {
            dataModel.put("obj", new OverloadedMethods2());
            final boolean dow = conf.getObjectWrapper() instanceof DefaultObjectWrapper;
            dataModel.put("dow", dow);
            dataModel.put("dowPre22", dow
                    && ((DefaultObjectWrapper) conf.getObjectWrapper()).getIncompatibleImprovements()
                            .intValue() < _TemplateAPI.VERSION_INT_2_3_22);
        }
    }
    
    public void runTest() throws IOException {
        Template template;
        try {
            template = conf.getTemplate(templateName);
        } catch (IOException e) {
            throw new AssertionFailedError(
                    "Could not load template " + StringUtil.jQuote(templateName) + ":\n" + getStackTrace(e));
        }
        ASTPrinter.validateAST(template);
        
        StringWriter out = noOutput ? null : new StringWriter();
        try {
            template.process(dataModel, out != null ? out : NullWriter.INSTANCE);
        } catch (TemplateException e) {
            throw new AssertionFailedError("Template " + StringUtil.jQuote(templateName) + " has stopped with error:\n"
                        + getStackTrace(e));
        }
        
        if (out != null) {
            assertExpectedFileEqualsString(getName(), out.toString());
        }
    }

    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Override
    protected File getExpectedFileDirectory() throws IOException {
        return new File(super.getExpectedFileDirectory(), "expected");
    }

    @Override
    protected String getDefaultCharset() {
        return conf.getOutputEncoding() != null ? conf.getOutputEncoding() : "UTF-8";
    }
    
    @Override
    protected File getExpectedFileFor(String testCaseFileName) throws IOException {
        return new File(getExpectedFileDirectory(), expectedFileName);
    }

    static class TestBoolean implements TemplateBooleanModel, TemplateScalarModel {
        public boolean getAsBoolean() {
            return true;
        }
        
        public String getAsString() {
            return "de";
        }
    }
    
    static class TestMethod implements TemplateMethodModel {
      public Object exec(java.util.List arguments) {
          return "x";
      }
    }
    
    static class TestNode implements TemplateNodeModel {
      
      public String getNodeName() {
          return "name";
      }
                    
      public TemplateNodeModel getParentNode() {
          return null;
      }
    
      public String getNodeType() {
          return "element";
      }
    
      public TemplateSequenceModel getChildNodes() {
          return null;
      }
      
      public String getNodeNamespace() {
          return null;
      }
    }

   public Object getTestMapBean()
    {
        Map testBean = new TestMapBean();
        testBean.put("name", "Chris");
        testBean.put("location", "San Francisco");
        testBean.put("age", new Integer(27));
        return testBean;
    }

    public static class TestMapBean extends HashMap {
        public String getName() {
            return "Christopher";
        }
        public int getLuckyNumber() {
            return 7;
        }
    }

    public static class TestBean {

        public int m(int n) {
            return n * 10;
        }

        public int mOverloaded(int n) {
            return n * 10;
        }

        public String mOverloaded(String s) {
            return s.toUpperCase();
        }
        
    }
    
}
