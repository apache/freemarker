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
 
package freemarker.ext.dom;

import java.util.List;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xpath.internal.XPath;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.objects.XBoolean;
import com.sun.org.apache.xpath.internal.objects.XNodeSet;
import com.sun.org.apache.xpath.internal.objects.XNull;
import com.sun.org.apache.xpath.internal.objects.XNumber;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.objects.XString;

import freemarker.core.Environment;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * XPath support implemented on the internal Xalan that is packed into Java under {@code com.sun} packages. This
 * won't be accessible if Java 9 module access rules are enforced (like if the application is started with
 * {@code java --illegal-access=deny}), because then accessing {@code com.sun} packages is banned. In such case
 * {@link XalanXPathSupport} can be used, which however needs the normal Apache Xalan to be present.
 */
class SunInternalXalanXPathSupport implements XPathSupport {

    private XPathContext xpathContext = new XPathContext();
        
    @Override
    synchronized public TemplateModel executeQuery(Object context, String xpathQuery) throws TemplateModelException {
        if (!(context instanceof Node)) {
            if (context == null || isNodeList(context)) {
                int cnt = context != null ? ((List) context).size() : 0;
                throw new TemplateModelException(
                        (cnt != 0
                                ? "Xalan can't perform an XPath query against a Node Set (contains " + cnt
                                        + " node(s)). Expecting a single Node."
                                : "Xalan can't perform an XPath query against an empty Node Set."
                        )
                        + " (There's no such restriction if you configure FreeMarker to use Jaxen for XPath.)");
            } else {
                throw new TemplateModelException(
                        "Can't perform an XPath query against a " + context.getClass().getName()
                                + ". Expecting a single org.w3c.dom.Node.");
            }
        }

        Node node = (Node) context;
        try {
            XPath xpath = new XPath(xpathQuery, null, CUSTOM_PREFIX_RESOLVER, XPath.SELECT, null);
            int ctxtNode = xpathContext.getDTMHandleFromNode(node);
            XObject xresult = xpath.execute(xpathContext, ctxtNode, CUSTOM_PREFIX_RESOLVER);
            if (xresult instanceof XNodeSet) {
                NodeListModel result = new NodeListModel(node);
                result.xpathSupport = this;
                NodeIterator nodeIterator = xresult.nodeset();
                Node n;
                do {
                    n = nodeIterator.nextNode();
                    if (n != null) {
                        result.add(n);
                    }
                } while (n != null);
                return result.size() == 1 ? result.get(0) : result;
            }
            if (xresult instanceof XBoolean) {
                return ((XBoolean) xresult).bool() ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
            if (xresult instanceof XNull) {
                return null;
            }
            if (xresult instanceof XString) {
                return new SimpleScalar(xresult.toString());
            }
            if (xresult instanceof XNumber) {
                return new SimpleNumber(Double.valueOf(((XNumber) xresult).num()));
            }
            throw new TemplateModelException("Cannot deal with type: " + xresult.getClass().getName());
        } catch (TransformerException te) {
            throw new TemplateModelException(te);
        }
    }
    
    private static final PrefixResolver CUSTOM_PREFIX_RESOLVER = new PrefixResolver() {
        
        @Override
        public String getNamespaceForPrefix(String prefix, Node node) {
            return getNamespaceForPrefix(prefix);
        }
        
        @Override
        public String getNamespaceForPrefix(String prefix) {
            if (prefix.equals(Template.DEFAULT_NAMESPACE_PREFIX)) {
                return Environment.getCurrentEnvironment().getDefaultNS();
            }
            return Environment.getCurrentEnvironment().getNamespaceForPrefix(prefix);
        }
        
        @Override
        public String getBaseIdentifier() {
            return null;
        }
        
        @Override
        public boolean handlesNullPrefixes() {
            return false;
        }
    };
    
    /**
     * Used for generating more intelligent error messages.
     */
    private static boolean isNodeList(Object context) {
        if (!(context instanceof List)) {
            return false;
        }

        List ls = (List) context;
        int ln = ls.size();
        for (int i = 0; i < ln; i++) {
            if (!(ls.get(i) instanceof Node)) {
                return false;
            }
        }
        return true;
    }
}