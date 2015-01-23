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
 
package freemarker.ext.dom;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jaxen.BaseXPath;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.FunctionContext;
import org.jaxen.JaxenException;
import org.jaxen.NamespaceContext;
import org.jaxen.Navigator;
import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DocumentNavigator;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import freemarker.core.CustomAttribute;
import freemarker.core.Environment;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.UndeclaredThrowableException;


/**
 */
class JaxenXPathSupport implements XPathSupport {
    
    private static final CustomAttribute cache = 
        new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE) {
            protected Object create() {
                return new HashMap();
            }
        };

    private final static ArrayList EMPTY_ARRAYLIST = new ArrayList();

    public TemplateModel executeQuery(Object context, String xpathQuery) throws TemplateModelException {
        try {
            BaseXPath xpath;
            Map xpathCache = (Map)cache.get();
            synchronized(xpathCache) {
                xpath = (BaseXPath) xpathCache.get(xpathQuery);
                if (xpath == null) {
                    xpath = new BaseXPath(xpathQuery, fmDomNavigator);
                    xpath.setNamespaceContext(customNamespaceContext);
                    xpath.setFunctionContext(fmFunctionContext);
                    xpath.setVariableContext(fmVariableContext);
                    xpathCache.put(xpathQuery, xpath);
                }
            }
            List result = xpath.selectNodes(context != null ? context : EMPTY_ARRAYLIST);
            if (result.size() == 1) {
                return ObjectWrapper.DEFAULT_WRAPPER.wrap(result.get(0));
            }
            NodeListModel nlm = new NodeListModel(result, null);
            nlm.xpathSupport = this;
            return nlm;
        } catch (UndeclaredThrowableException e) {
            Throwable t  = e.getUndeclaredThrowable();
            if(t instanceof TemplateModelException) {
                throw (TemplateModelException)t;
            }
            throw e;
        } catch (JaxenException je) {
            throw new TemplateModelException(je);
        }
    }

    static private final NamespaceContext customNamespaceContext = new NamespaceContext() {
        
        public String translateNamespacePrefixToUri(String prefix) {
            if (prefix.equals(Template.DEFAULT_NAMESPACE_PREFIX)) {
                return Environment.getCurrentEnvironment().getDefaultNS();
            }
            return Environment.getCurrentEnvironment().getNamespaceForPrefix(prefix);
        }
    };

    private static final VariableContext fmVariableContext = new VariableContext() {
        public Object getVariableValue(String namespaceURI, String prefix, String localName)
        throws 
            UnresolvableException
        {
            try {
                TemplateModel model = Environment.getCurrentEnvironment().getVariable(localName);
                if(model == null) {
                    throw new UnresolvableException("Variable " + localName + " not found.");
                }
                if(model instanceof TemplateScalarModel) {
                    return ((TemplateScalarModel)model).getAsString();
                }
                if(model instanceof TemplateNumberModel) {
                    return ((TemplateNumberModel)model).getAsNumber();
                }
                if(model instanceof TemplateDateModel) {
                    return ((TemplateDateModel)model).getAsDate();
                }
                if(model instanceof TemplateBooleanModel) {
                    return Boolean.valueOf(((TemplateBooleanModel)model).getAsBoolean());
                }
            }
            catch(TemplateModelException e) {
                throw new UndeclaredThrowableException(e);
            }
            throw new UnresolvableException("Variable " + localName + " is not a string, number, date, or boolean");
        }
    };
     
    private static final FunctionContext fmFunctionContext = new XPathFunctionContext() {
        public Function getFunction(String namespaceURI, String prefix, String localName)
        throws UnresolvableException {
            try {
                return super.getFunction(namespaceURI, prefix, localName);
            } 
            catch(UnresolvableException e) {
                return super.getFunction(null, null, localName);
            }
        }
    };
    
    private static final CustomAttribute cachedTree = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE);
     
    private static final Navigator fmDomNavigator = new DocumentNavigator() {
        public Object getDocument(String uri) throws FunctionCallException
        {
            try
            {
                Template raw = getTemplate(uri);
                Document doc = (Document)cachedTree.get(raw);
                if(doc == null) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    FmEntityResolver er = new FmEntityResolver();
                    builder.setEntityResolver(er);
                    doc = builder.parse(createInputSource(null, raw));
                    // If the entity resolver got called 0 times, the document
                    // is standalone, so we can safely cache it
                    if(er.getCallCount() == 0) {
                        cachedTree.set(doc, raw);
                    }
                }
                return doc;
            }
            catch (Exception e)
            {
                throw new FunctionCallException("Failed to parse document for URI: " + uri, e);
            }
        }
    };

    static Template getTemplate(String systemId) throws IOException {
        Environment env = Environment.getCurrentEnvironment();
        String encoding = env.getTemplate().getEncoding();
        if (encoding == null) {
            encoding = env.getConfiguration().getEncoding(env.getLocale());
        }
        String templatePath = env.getTemplate().getName();
        int lastSlash = templatePath.lastIndexOf('/');
        templatePath = lastSlash == -1 ? "" : templatePath.substring(0, lastSlash + 1);
        systemId = env.toFullTemplateName(templatePath, systemId);
        Template raw = env.getConfiguration().getTemplate(systemId, env.getLocale(), encoding, false);
        return raw;
    }

    private static InputSource createInputSource(String publicId, Template raw) throws IOException, SAXException {
        StringWriter sw = new StringWriter();
        try {
            raw.process(Collections.EMPTY_MAP, sw);
        }
        catch(TemplateException e) {
            throw new SAXException(e);
        }
        InputSource is = new InputSource();
        is.setPublicId(publicId);
        is.setSystemId(raw.getName());
        is.setCharacterStream(new StringReader(sw.toString()));
        return is;
    }

    private static class FmEntityResolver implements EntityResolver {
        private int callCount = 0;
        
        public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException {
            ++callCount;
            return createInputSource(publicId, getTemplate(systemId));
        }
        
        int getCallCount() {
            return callCount;
        }
    };
}
