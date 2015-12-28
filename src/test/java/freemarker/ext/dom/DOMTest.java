package freemarker.ext.dom;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class DOMTest extends TemplateTest {

    @Test
    public void xpathDetectionBugfix() throws Exception {
        addDocToDataModel("<root><a>A</a><b>B</b><c>C</c></root>");
        assertOutput("${doc.root.b['following-sibling::c']}", "C");
        assertOutput("${doc.root.b['following-sibling::*']}", "C");
    }

    @Test
    public void namespaceUnaware() throws Exception {
        addNSUnawareDocToDataModel("<root><x:a>A</x:a><:>B</:><xyz::c>C</xyz::c></root>");
        assertOutput("${doc.root['x:a']}", "A");
        assertOutput("${doc.root[':']}", "B");
        try {
            assertOutput("${doc.root['xyz::c']}", "C");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("xyz"));
        }
    }
    
    private void addDocToDataModel(String xml) throws SAXException, IOException, ParserConfigurationException {
        addToDataModel("doc", NodeModel.parse(new InputSource(new StringReader(xml))));
    }

    private void addNSUnawareDocToDataModel(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();
        newFactory.setNamespaceAware(false);
        DocumentBuilder builder = newFactory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        addToDataModel("doc", doc);
    }

}
