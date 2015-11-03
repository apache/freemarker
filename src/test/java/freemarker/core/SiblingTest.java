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
