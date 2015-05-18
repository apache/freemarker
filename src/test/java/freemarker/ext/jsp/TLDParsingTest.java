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

package freemarker.ext.jsp;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;

@RunWith(JUnit4.class)
public class TLDParsingTest {

    private BeansWrapper wrapper;

    @Before
    public void before() throws Exception {
        BeansWrapperBuilder builder = new BeansWrapperBuilder(new Version("2.3"));
        wrapper = builder.build();
    }

    @Test
    public void testTldParser() throws Exception {
        URL url = getClass().getResource("TLDParsingTest.tld");
        TaglibFactory.TldParserForTaglibBuilding tldParser = new TaglibFactory.TldParserForTaglibBuilding(wrapper);
        InputSource is = new InputSource();
        InputStream input = url.openStream();
        is.setByteStream(input);
        is.setSystemId(url.toString());
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setContentHandler(tldParser);
        reader.setErrorHandler(tldParser);
        reader.parse(is);
        input.close();

        assertEquals(1, tldParser.getListeners().size());
        assertTrue(tldParser.getListeners().get(0) instanceof ExampleContextListener);

        Map tagsAndFunctions = tldParser.getTagsAndFunctions();
        assertEquals(4, tagsAndFunctions.size());

        JspTagModelBase tag = (JspTagModelBase) tagsAndFunctions.get("setStringAttributeTag");
        assertNotNull(tag);
        tag = (JspTagModelBase) tagsAndFunctions.get("setStringAttributeTag2");
        assertNotNull(tag);

        TemplateMethodModelEx function = (TemplateMethodModelEx) tagsAndFunctions.get("toUpperCase");
        assertNotNull(function);
        TemplateScalarModel result = (TemplateScalarModel) function.exec(Arrays.asList(new TemplateModel [] { new SimpleScalar("abc") }));
        assertEquals("ABC", result.getAsString());
        function = (TemplateMethodModelEx) tagsAndFunctions.get("toUpperCase2");
        assertNotNull(function);
        result = (TemplateScalarModel) function.exec(Arrays.asList(new TemplateModel [] { new SimpleScalar("abc") }));
        assertEquals("ABC", result.getAsString());
    }

    public static class StringFunctions {

        private StringFunctions() {
        }

        public static String toUpperCase(String source) {
            return source.toUpperCase();
        }
    }
    
    public static class SetStringAttributeTag extends TagSupport {

        private String name;
        private String value;

        public SetStringAttributeTag() {
            super();
        }

        public int doStartTag() throws JspException {
            pageContext.setAttribute(name, value);
            return SKIP_BODY;
        }

        public int doEndTag() throws JspException {
            name = null;
            value = null;
            return SKIP_BODY;
        }
    }
    
    public static class ExampleContextListener implements ServletContextListener {
        public void contextInitialized(ServletContextEvent event) { }
        public void contextDestroyed(ServletContextEvent event) { }
    }    
    
}
