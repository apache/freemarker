/*
 * Copyright (c) 2005 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.test.templatesuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
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
import freemarker.template.utility.StringUtil;
import freemarker.test.templatesuite.models.BooleanHash1;
import freemarker.test.templatesuite.models.BooleanHash2;
import freemarker.test.templatesuite.models.BooleanList1;
import freemarker.test.templatesuite.models.BooleanList2;
import freemarker.test.templatesuite.models.MultiModel1;
import freemarker.test.templatesuite.models.OverloadedMethods;
import freemarker.test.templatesuite.models.VarArgTestModel;
import freemarker.test.utility.AssertEqualsDirective;
import freemarker.test.utility.AssertFailsDirective;


public class TemplateTestCase extends TestCase {
    
    private Template template;
    private HashMap dataModel = new HashMap();
    
    private String filename, testName;
    private File outputDir;
    
    private Configuration conf = new Configuration();
    
    public TemplateTestCase(String name, String filename) {
        super(name);
        this.filename = filename;
        this.testName = name;
    }
    
    public void setTemplateDirectory(String dirname) throws IOException {
        URL url = getClass().getResource("TemplateTestCase.class");
        File parent = new File(url.getFile()).getParentFile();
        File dir = new File(parent, dirname);
        conf.setDirectoryForTemplateLoading(dir);
    }
    
    public void setReferenceDirectory(String dirname) {
        URL url = getClass().getResource("TemplateTestCase.class");
        File parent = new File(url.getFile()).getParentFile();
        this.outputDir = new File(parent, dirname);
    }

    public void setConfigParam(String param, String value) throws IOException {
        if ("templatedir".equals(param)) {
            setTemplateDirectory(value);
        }
        else if ("auto_import".equals(param)) {
            StringTokenizer st = new StringTokenizer(value);
            if (!st.hasMoreTokens()) fail("Expecting libname");
            String libname = st.nextToken();
            if (!st.hasMoreTokens()) fail("Expecting 'as <alias>' in autoimport");
            String as = st.nextToken();
            if (!as.equals("as")) fail("Expecting 'as <alias>' in autoimport");
            if (!st.hasMoreTokens()) fail("Expecting alias after 'as' in autoimport");
            String alias = st.nextToken();
            conf.addAutoImport(alias, libname);
        }
        else if ("clear_encoding_map".equals(param)) {
            if (StringUtil.getYesNo(value)) {
                conf.clearEncodingMap();
            }
        }
        else if ("input_encoding".equals(param)) {
            conf.setDefaultEncoding(value);
        }
        else if ("referencedir".equals(param)) {
            setReferenceDirectory(value);
        }
        else {
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
    
    public void setUp() throws Exception {
        dataModel.put("assertEquals", new AssertEqualsDirective());
        dataModel.put("assertFails", new AssertFailsDirective());
        
        dataModel.put("message", "Hello, world!");
        
        if (testName.equals("bean-maps")) {
            BeansWrapper w1 = new BeansWrapper();
            BeansWrapper w2 = new BeansWrapper();
            BeansWrapper w3 = new BeansWrapper();
            BeansWrapper w4 = new BeansWrapper();
            BeansWrapper w5 = new BeansWrapper();
            BeansWrapper w6 = new BeansWrapper();
            BeansWrapper w7 = new BeansWrapper();
    
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
    
            Object test = getTestBean();
    
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
        
        else if (testName.equals("beans")) {
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
        
        else if (testName.equals("boolean")) {
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
        
        else if (testName.equals("dateformat")) {
            GregorianCalendar cal = new GregorianCalendar(2002, 10, 15, 14, 54, 13);
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            dataModel.put("date", new SimpleDate(cal.getTime(), TemplateDateModel.DATETIME));
            dataModel.put("unknownDate", new SimpleDate(cal.getTime(), TemplateDateModel.UNKNOWN));
        }
    
        else if (testName.equals("number-format")) {
            dataModel.put("int", new SimpleNumber(new Integer(1)));
            dataModel.put("double", new SimpleNumber(new Double(1.0)));
            dataModel.put("double2", new SimpleNumber(new Double(1 + 1e-15)));
            dataModel.put("double3", new SimpleNumber(new Double(1e-16)));
            dataModel.put("double4", new SimpleNumber(new Double(-1e-16)));
            dataModel.put("bigDecimal", new SimpleNumber(java.math.BigDecimal.valueOf(1)));
            dataModel.put("bigDecimal2", new SimpleNumber(java.math.BigDecimal.valueOf(1, 16)));
        }
    
        else if (testName.equals("default-xmlns")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/defaultxmlns1.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (testName.equals("multimodels")) {
            dataModel.put("test", "selftest");
            dataModel.put("self", "self");
            dataModel.put("zero", new Integer(0));
            dataModel.put("data", new MultiModel1());
        }
        
        else if (testName.equals("stringbimethods")) {
            dataModel.put("multi", new TestBoolean());
        }
        
        else if (testName.equals("type-builtins")) {
            dataModel.put("testmethod", new TestMethod());
            dataModel.put("testnode", new TestNode());
            dataModel.put("testcollection", new SimpleCollection(new ArrayList()));
        }
        
        else if (testName.equals("var-layers")) {
            dataModel.put("x", new Integer(4));
            dataModel.put("z", new Integer(4));
            conf.setSharedVariable("y", new Integer(7));
        }
        
        else if (testName.equals("xml-fragment")) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            DocumentBuilder db = f.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new InputSource(getClass().getResourceAsStream("models/xmlfragment.xml")));
            dataModel.put("node", NodeModel.wrap(doc.getDocumentElement().getFirstChild().getFirstChild()));
        }
        
        else if (testName.equals("xmlns1")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (testName.equals("xmlns2")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns2.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (testName.equals("xmlns3") || testName.equals("xmlns4")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/xmlns3.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        else if (testName.equals("xmlns5")) {
            InputSource is = new InputSource(getClass().getResourceAsStream("models/defaultxmlns1.xml"));
            NodeModel nm = NodeModel.parse(is);
            dataModel.put("doc", nm);
        }
        
        else if (testName.startsWith("sequence-builtins-with-")) {
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
        }
        
        else if (testName.equals("iso8601")) {
            dataModel.put("javaGMT02", TimeZone.getTimeZone("GMT+02"));
            dataModel.put("javaUTC", TimeZone.getTimeZone("UTC"));
            dataModel.put("adaptedToStringScalar", new Object() {
                public String toString() {
                    return "GMT+02";
                }
            });
        }
        
        else if (testName.equals("number-to-date")) {
          dataModel.put("bigInteger", new BigInteger("1305575275540"));
          dataModel.put("bigDecimal", new BigDecimal("1305575275539.5"));
        }
        
        else if (testName.equals("varargs")) {
          dataModel.put("m", new VarArgTestModel());
        }
        
        else if (testName.startsWith("overloaded-methods-")) {
          dataModel.put("obj", new OverloadedMethods());
        }
    }
    
    public void runTest() {
        try {
            template = conf.getTemplate(filename);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            fail("Could not load template " + filename + "\n" + sw.toString());
        }
        File refFile = new File (outputDir, filename);
        File outFile = new File (outputDir, filename+".out");
        Writer out = null;
        String encoding = conf.getOutputEncoding();
        if (encoding == null) encoding = "UTF-8";
        try {
            out = new OutputStreamWriter(new FileOutputStream(outFile), 
                    encoding);
        } catch (IOException ioe) {
            fail("Cannot write to file: " + outFile + "\n" + ioe.getMessage());
        }
        try {
            template.process(dataModel, out);
            out.close();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            fail("Could not process template " + filename + "\n" + sw.toString());
        }
        try {
            Reader ref = new InputStreamReader(new FileInputStream(refFile), 
                    encoding);
            Reader output = new InputStreamReader(new FileInputStream(outFile), 
                    encoding);
            System.out.println(outFile);
            compare(ref, output);
            outFile.delete();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            fail("Error comparing files " + refFile + " and " + outFile + "\n" + sw.toString());
        }
    }

    static public void compare(Reader reference, Reader output) throws IOException {
        LineNumberReader ref = new LineNumberReader(reference);
        LineNumberReader out = new LineNumberReader(output);
        String refLine = "", outLine = "";
        while (refLine != null || outLine != null) {
            if (refLine == null) {
                fail("Output text is longer than reference text");
            }
            if (outLine == null) {
                fail("Output text is shorter than reference text");
            }
            refLine = ref.readLine();
            outLine = out.readLine();
            if (refLine != null && outLine != null & !refLine.equals(outLine)) {
                fail("Difference found on line " + ref.getLineNumber() + 
                                            ".\nReference text is: " + refLine +
                                            "\nOutput text is: " + outLine);
            }
        } 
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

   public Object getTestBean()
    {
        Map testBean = new TestBean();
        testBean.put("name", "Chris");
        testBean.put("location", "San Francisco");
        testBean.put("age", new Integer(27));
        return testBean;
    }

    public static class TestBean extends HashMap {
        public String getName() {
            return "Christopher";
        }
        public int getLuckyNumber() {
            return 7;
        }
    }
}
