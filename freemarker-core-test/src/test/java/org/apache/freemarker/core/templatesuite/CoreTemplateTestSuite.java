package org.apache.freemarker.core.templatesuite;

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
import java.util.TimeZone;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.freemarker.core.ASTPrinter;
import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.impl.DefaultNonListCollectionAdapter;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.ResourceBundleModel;
import org.apache.freemarker.core.model.impl.SimpleCollection;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.templatesuite.models.BooleanAndStringTemplateModel;
import org.apache.freemarker.core.templatesuite.models.BooleanHash1;
import org.apache.freemarker.core.templatesuite.models.BooleanHash2;
import org.apache.freemarker.core.templatesuite.models.BooleanList1;
import org.apache.freemarker.core.templatesuite.models.BooleanList2;
import org.apache.freemarker.core.templatesuite.models.BooleanVsStringMethods;
import org.apache.freemarker.core.templatesuite.models.JavaObjectInfo;
import org.apache.freemarker.core.templatesuite.models.Listables;
import org.apache.freemarker.core.templatesuite.models.MultiModel1;
import org.apache.freemarker.core.templatesuite.models.OverloadedMethods2;
import org.apache.freemarker.core.templatesuite.models.TestBean;
import org.apache.freemarker.core.templatesuite.models.TestBoolean;
import org.apache.freemarker.core.templatesuite.models.TestMethod;
import org.apache.freemarker.core.templatesuite.models.TestNode;
import org.apache.freemarker.core.templatesuite.models.VarArgTestModel;
import org.apache.freemarker.dom.NodeModel;
import org.apache.freemarker.test.TemplateTestSuite;
import org.apache.freemarker.test.XMLLoader;
import org.xml.sax.InputSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestSuite;

public class CoreTemplateTestSuite extends TemplateTestSuite {

    private final DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();

    /**
     * Required for the suite to be detected and run by build tools and IDE-s.
     */
    public static TestSuite suite() {
        return new CoreTemplateTestSuite();
    }

    @Override
    protected void setUpTestCase(String simpleTestName, Map<String, Object> dataModel,
            Configuration.ExtendableBuilder<?> confB) throws Exception {
        dataModel.put("message", "Hello, world!");
        dataModel.put("javaObjectInfo", JavaObjectInfo.INSTANCE);

        if (simpleTestName.startsWith("api-builtin")) {
            dataModel.put("map", ImmutableMap.of(1, "a", 2, "b", 3, "c"));
            dataModel.put("list", ImmutableList.of(1, 2, 3));
            dataModel.put("set", ImmutableSet.of("a", "b", "c"));
            dataModel.put("s", "test");
        } else if (simpleTestName.equals("default-object-wrapper")) {
            dataModel.put("array", new String[] { "array-0", "array-1"});
            dataModel.put("list", Arrays.asList("list-0", "list-1", "list-2"));
            Map<Object, Object> tmap = new HashMap<>();
            tmap.put("key", "value");
            Object objKey = new Object();
            tmap.put(objKey, "objValue");
            dataModel.put("map", tmap);
            dataModel.put("objKey", objKey);
            dataModel.put("obj", new org.apache.freemarker.core.templatesuite.models.BeanTestClass());
            dataModel.put("resourceBundle",
                    new ResourceBundleModel(ResourceBundle.getBundle(
                            "org.apache.freemarker.core.templatesuite.models.BeansTestResources"), dow));
            dataModel.put("date", new GregorianCalendar(1974, 10, 14).getTime());
            dataModel.put("statics", dow.getStaticModels());
            dataModel.put("enums", dow.getEnumModels());
        } else if (simpleTestName.equals("boolean")) {
            dataModel.put( "boolean1", TemplateBooleanModel.FALSE);
            dataModel.put( "boolean2", TemplateBooleanModel.TRUE);
            dataModel.put( "boolean3", TemplateBooleanModel.TRUE);
            dataModel.put( "boolean4", TemplateBooleanModel.TRUE);
            dataModel.put( "boolean5", TemplateBooleanModel.FALSE);

            dataModel.put( "list1", new BooleanList1(dow) );
            dataModel.put( "list2", new BooleanList2(dow) );

            dataModel.put( "hash1", new BooleanHash1() );
            dataModel.put( "hash2", new BooleanHash2() );
        } else if (simpleTestName.startsWith("dateformat")) {
            GregorianCalendar cal = new GregorianCalendar(2002, 10, 15, 14, 54, 13);
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            dataModel.put("date", new SimpleDate(cal.getTime(), TemplateDateModel.DATETIME));
            dataModel.put("unknownDate", new SimpleDate(cal.getTime(), TemplateDateModel.UNKNOWN));
            dataModel.put("javaGMT02", TimeZone.getTimeZone("GMT+02"));
            dataModel.put("javaUTC", TimeZone.getTimeZone("UTC"));
            dataModel.put("adaptedToStringScalar", new Object() {
                @Override
                public String toString() {
                    return "GMT+02";
                }
            });
            dataModel.put("sqlDate", new java.sql.Date(1273955885023L));
            dataModel.put("sqlTime", new java.sql.Time(74285023L));
        } else if (simpleTestName.startsWith("list-") || simpleTestName.startsWith("list[")
                || simpleTestName.startsWith("list2[") || simpleTestName.startsWith("list3[")
                || simpleTestName.equals("listhash")) {
            dataModel.put("listables", new Listables());
        } else if (simpleTestName.startsWith("number-format")) {
            dataModel.put("int", new SimpleNumber(Integer.valueOf(1)));
            dataModel.put("double", new SimpleNumber(Double.valueOf(1.0)));
            dataModel.put("double2", new SimpleNumber(Double.valueOf(1 + 1e-15)));
            dataModel.put("double3", new SimpleNumber(Double.valueOf(1e-16)));
            dataModel.put("double4", new SimpleNumber(Double.valueOf(-1e-16)));
            dataModel.put("bigDecimal", new SimpleNumber(java.math.BigDecimal.valueOf(1)));
            dataModel.put("bigDecimal2", new SimpleNumber(java.math.BigDecimal.valueOf(1, 16)));
        } else if (simpleTestName.equals("simplehash-char-key")) {
            HashMap<String, String> mStringC = new HashMap<>();
            mStringC.put("c", "string");
            dataModel.put("mStringC", mStringC);

            HashMap<String, String> mStringCNull = new HashMap<>();
            mStringCNull.put("c", null);
            dataModel.put("mStringCNull", mStringCNull);

            HashMap<Character, String> mCharC = new HashMap<>();
            mCharC.put(Character.valueOf('c'), "char");
            dataModel.put("mCharC", mCharC);

            HashMap<String, String> mCharCNull = new HashMap<>();
            mCharCNull.put("c", null);
            dataModel.put("mCharCNull", mCharCNull);

            HashMap<Object, String> mMixed = new HashMap<>();
            mMixed.put(Character.valueOf('c'), "char");
            mMixed.put("s", "string");
            mMixed.put("s2", "string2");
            mMixed.put("s2n", null);
            dataModel.put("mMixed", mMixed);
        } else if (simpleTestName.equals("default-xmlns")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/defaultxmlns1.xml"));
            NodeModel nm = XMLLoader.toModel(is);
            dataModel.put("doc", nm);
        } else if (simpleTestName.equals("multimodels")) {
            dataModel.put("test", "selftest");
            dataModel.put("self", "self");
            dataModel.put("zero", Integer.valueOf(0));
            dataModel.put("data", new MultiModel1());
        } else if (simpleTestName.equals("stringbimethods")) {
            dataModel.put("multi", new TestBoolean());
        } else if (simpleTestName.startsWith("type-builtins")) {
            dataModel.put("testmethod", new TestMethod());
            dataModel.put("testnode", new TestNode());
            dataModel.put("testcollection", new SimpleCollection(new ArrayList<>(), dow));
            dataModel.put("testcollectionEx", DefaultNonListCollectionAdapter.adapt(new HashSet<>(), dow));
            dataModel.put("bean", new TestBean());
        } else if (simpleTestName.equals("date-type-builtins")) {
            GregorianCalendar cal = new GregorianCalendar(2003, 4 - 1, 5, 6, 7, 8);
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = cal.getTime();
            dataModel.put("unknown", d);
            dataModel.put("timeOnly", new java.sql.Time(d.getTime()));
            dataModel.put("dateOnly", new java.sql.Date(d.getTime()));
            dataModel.put("dateTime", new java.sql.Timestamp(d.getTime()));
        } else if (simpleTestName.equals("var-layers")) {
            dataModel.put("x", Integer.valueOf(4));
            dataModel.put("z", Integer.valueOf(4));
            confB.setSharedVariable("y", Integer.valueOf(7));
        } else if (simpleTestName.equals("xml-fragment")) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            DocumentBuilder db = f.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new InputSource(getClass().getResourceAsStream("models/xmlfragment.xml")));
            NodeModel.simplify(doc);
            dataModel.put("node", NodeModel.wrap(doc.getDocumentElement().getFirstChild().getFirstChild()));
        } else if (simpleTestName.equals("xmlns1")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns.xml"));
            NodeModel nm = XMLLoader.toModel(is);
            dataModel.put("doc", nm);
        } else if (simpleTestName.equals("xmlns2")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns2.xml"));
            NodeModel nm = XMLLoader.toModel(is);
            dataModel.put("doc", nm);
        } else if (simpleTestName.equals("xmlns3") || simpleTestName.equals("xmlns4")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns3.xml"));
            NodeModel nm = XMLLoader.toModel(is);
            dataModel.put("doc", nm);
        } else if (simpleTestName.equals("xmlns5")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/defaultxmlns1.xml"));
            NodeModel nm = XMLLoader.toModel(is);
            dataModel.put("doc", nm);
        } else if (simpleTestName.equals("xml-ns_prefix-scope")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xml-ns_prefix-scope.xml"));
            NodeModel nm = XMLLoader.toModel(is);
            dataModel.put("doc", nm);
        } else if (simpleTestName.startsWith("sequence-builtins")) {
            Set<String> abcSet = new TreeSet<>();
            abcSet.add("a");
            abcSet.add("b");
            abcSet.add("c");
            dataModel.put("abcSet", abcSet);
            dataModel.put("abcSetNonSeq", DefaultNonListCollectionAdapter.adapt(abcSet, dow));

            List<String> listWithNull = new ArrayList<>();
            listWithNull.add("a");
            listWithNull.add(null);
            listWithNull.add("c");
            dataModel.put("listWithNull", listWithNull);

            List<String> listWithNullsOnly = new ArrayList<>();
            listWithNull.add(null);
            listWithNull.add(null);
            listWithNull.add(null);
            dataModel.put("listWithNullsOnly", listWithNullsOnly);

            dataModel.put("abcCollection", new SimpleCollection(abcSet, dow));

            Set<String> set = new HashSet<>();
            set.add("a");
            set.add("b");
            set.add("c");
            dataModel.put("set", set);
        } else if (simpleTestName.equals("number-to-date")) {
            dataModel.put("bigInteger", new BigInteger("1305575275540"));
            dataModel.put("bigDecimal", new BigDecimal("1305575275539.5"));
        } else if (simpleTestName.equals("varargs")) {
            dataModel.put("m", new VarArgTestModel());
        } else if (simpleTestName.startsWith("boolean-formatting")) {
            dataModel.put("booleanAndString", new BooleanAndStringTemplateModel());
            dataModel.put("booleanVsStringMethods", new BooleanVsStringMethods());
        } else if (simpleTestName.startsWith("number-math-builtins")) {
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
        } else if (simpleTestName.startsWith("overloaded-methods")) {
            dataModel.put("obj", new OverloadedMethods2());
        }
    }

    @Override
    protected void validateTemplate(Template template) {
        ASTPrinter.validateAST(template);
    }

}
