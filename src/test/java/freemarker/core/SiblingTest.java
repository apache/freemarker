package freemarker.core;

import freemarker.ext.dom.NodeModel;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Pmuruge on 10/29/2015.
 */
public class SiblingTest extends TemplateTest {

    @Override
    protected Object getDataModel() {
        Map dataModel = new HashMap();
        String dataModelFileUrl = this.getClass().getResource(".").toString() + "/siblingDataModel.xml";
        try {
            dataModel.put(
                    "doc", NodeModel.parse(new File("build/test-classes/freemarker/core/siblingDataModel.xml")));
        } catch (Exception e) {
            System.out.println("Exception while parsing the dataModel xml");
            e.printStackTrace();
        }
        return dataModel;
    }
    @Test
    public void testPreviousSibling() throws IOException, TemplateException {
        String ftl = "<#assign sibling>${doc.person.name?previousSibling}</#assign>" +
                "${sibling?trim}" ;
        assertOutput(ftl, "");
    }


}
