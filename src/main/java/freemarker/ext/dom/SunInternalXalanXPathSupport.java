/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
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
 
package freemarker.ext.dom;

import freemarker.template.*;
import freemarker.core.Environment;

import org.w3c.dom.*;
import org.w3c.dom.traversal.NodeIterator;
import com.sun.org.apache.xpath.internal.*;
import com.sun.org.apache.xpath.internal.objects.*;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import java.util.List;
import javax.xml.transform.TransformerException;

/**
 * This is just the XalanXPathSupport class using the sun internal
 * package names
 */

class SunInternalXalanXPathSupport implements XPathSupport {
    
    private XPathContext xpathContext = new XPathContext();
        
    private static final String ERRMSG_RECOMMEND_JAXEN
            = "(Note that there is no such restriction if you "
                    + "configure FreeMarker to use Jaxen instead of Xalan.)";

    private static final String ERRMSG_EMPTY_NODE_SET
            = "Cannot perform an XPath query against an empty node set." + ERRMSG_RECOMMEND_JAXEN;
    
    synchronized public TemplateModel executeQuery(Object context, String xpathQuery) throws TemplateModelException {
        if (!(context instanceof Node)) {
            if (context != null) {
                if (isNodeList(context)) {
                    int cnt = ((List) context).size();
                    if (cnt != 0) {
                        throw new TemplateModelException(
                                "Cannot perform an XPath query against a node set of " + cnt
                                + " nodes. Expecting a single node." + ERRMSG_RECOMMEND_JAXEN);
                    } else {
                        throw new TemplateModelException(ERRMSG_EMPTY_NODE_SET);
                    }
                } else {
                    throw new TemplateModelException(
                            "Cannot perform an XPath query against a " + context.getClass().getName()
                            + ". Expecting a single org.w3c.dom.Node.");
                }
            } else {
                throw new TemplateModelException(ERRMSG_EMPTY_NODE_SET);
            }
        }
        Node node = (Node) context;
        try {
            XPath xpath = new XPath(xpathQuery, null, customPrefixResolver, XPath.SELECT, null);
            int ctxtNode = xpathContext.getDTMHandleFromNode(node);
            XObject xresult = xpath.execute(xpathContext, ctxtNode, customPrefixResolver);
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
                return new SimpleNumber(new Double(((XNumber) xresult).num()));
            }
            throw new TemplateModelException("Cannot deal with type: " + xresult.getClass().getName());
        } catch (TransformerException te) {
            throw new TemplateModelException(te);
        }
    }
    
    private static PrefixResolver customPrefixResolver = new PrefixResolver() {
        
        public String getNamespaceForPrefix(String prefix, Node node) {
            return getNamespaceForPrefix(prefix);
        }
        
        public String getNamespaceForPrefix(String prefix) {
            if (prefix.equals(Template.DEFAULT_NAMESPACE_PREFIX)) {
                return Environment.getCurrentEnvironment().getDefaultNS();
            }
            return Environment.getCurrentEnvironment().getNamespaceForPrefix(prefix);
        }
        
        public String getBaseIdentifier() {
            return null;
        }
        
        public boolean handlesNullPrefixes() {
            return false;
        }
    };
    
    /**
     * Used for generating more intelligent error messages.
     */
    private static boolean isNodeList(Object context) {
        if (context instanceof List) {
            List ls = (List) context;
            int ln = ls.size();
            for (int i = 0; i < ln; i++) {
                if (!(ls.get(i) instanceof Node)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}