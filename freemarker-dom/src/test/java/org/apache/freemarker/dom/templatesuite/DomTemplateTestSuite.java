/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.dom.templatesuite;

import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.dom.NodeModel;
import org.apache.freemarker.dom.test.DOMLoader;
import org.apache.freemarker.test.TemplateTestSuite;
import org.w3c.dom.Document;

import junit.framework.TestSuite;

public class DomTemplateTestSuite extends TemplateTestSuite {

    @Override
    protected void setUpTestCase(String simpleTestName, Map<String, Object> dataModel,
            Configuration.ExtendableBuilder<?> confB) throws Exception {
        NodeModel.useJaxenXPathSupport();

        if (simpleTestName.equals("default-xmlns")) {
            dataModel.put("doc", DOMLoader.toModel(getClass(), "models/defaultxmlns1.xml"));
        } else if (simpleTestName.equals("xml-fragment")) {
            Document doc = DOMLoader.toDOM(getClass(), "models/xmlfragment.xml");
            NodeModel.simplify(doc);
            dataModel.put("node", NodeModel.wrap(doc.getDocumentElement().getFirstChild().getFirstChild()));
        } else if (simpleTestName.equals("xmlns1")) {
            dataModel.put("doc", DOMLoader.toModel(getClass(), "models/xmlns.xml"));
        } else if (simpleTestName.equals("xmlns2")) {
            dataModel.put("doc", DOMLoader.toModel(getClass(), "models/xmlns2.xml"));
        } else if (simpleTestName.equals("xmlns3") || simpleTestName.equals("xmlns4")) {
            dataModel.put("doc", DOMLoader.toModel(getClass(), "models/xmlns3.xml"));
        } else if (simpleTestName.equals("xmlns5")) {
            dataModel.put("doc", DOMLoader.toModel(getClass(), "models/defaultxmlns1.xml"));
        } else if (simpleTestName.equals("xml-ns_prefix-scope")) {
            dataModel.put("doc", DOMLoader.toModel(getClass(), "models/xml-ns_prefix-scope.xml"));
        }
    }

    public static TestSuite suite() {
        return new DomTemplateTestSuite();
    }

}
