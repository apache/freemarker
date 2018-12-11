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
 
package org.apache.freemarker.dom;

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

import org.apache.freemarker.core.CustomStateKey;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.UndeclaredThrowableException;
import org.apache.freemarker.core.util._ObjectHolder;
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


/**
 */
class JaxenXPathSupport implements XPathSupport {

    private static final CustomStateKey<Map<String, BaseXPath>> XPATH_CACHE_ATTR
            = new CustomStateKey<Map<String, BaseXPath>>() {
        @Override
        protected Map<String, BaseXPath> create() {
            return new HashMap<String, BaseXPath>();
        }
    };

        // [2.4] Can't we just use Collections.emptyList()? 
    private final static ArrayList EMPTY_ARRAYLIST = new ArrayList();

    @Override
    public TemplateModel executeQuery(Object context, String xpathQuery) throws TemplateException {
        try {
            BaseXPath xpath;
            Map<String, BaseXPath> xpathCache = Environment.getCurrentEnvironmentNotNull().getCurrentTemplate()
                    .getCustomState(XPATH_CACHE_ATTR);
            synchronized (xpathCache) {
                xpath = xpathCache.get(xpathQuery);
                if (xpath == null) {
                    xpath = new BaseXPath(xpathQuery, FM_DOM_NAVIGATOR);
                    xpath.setNamespaceContext(customNamespaceContext);
                    xpath.setFunctionContext(FM_FUNCTION_CONTEXT);
                    xpath.setVariableContext(FM_VARIABLE_CONTEXT);
                    xpathCache.put(xpathQuery, xpath);
                }
            }
            List result = xpath.selectNodes(context != null ? context : EMPTY_ARRAYLIST);
            if (result.size() == 1) {
                return NodeQueryResultItemObjectWrapper.INSTANCE.wrap(result.get(0));
            }
            NodeListModel nlm = new NodeListModel(result, null);
            nlm.xpathSupport = this;
            return nlm;
        } catch (UndeclaredThrowableException e) {
            Throwable t  = e.getUndeclaredThrowable();
            if (t instanceof TemplateException) {
                throw (TemplateException) t;
            }
            throw e;
        } catch (JaxenException e) {
            throw new TemplateException(e);
        }
    }

    static private final NamespaceContext customNamespaceContext = new NamespaceContext() {
        
        @Override
        public String translateNamespacePrefixToUri(String prefix) {
            if (prefix.equals(Template.DEFAULT_NAMESPACE_PREFIX)) {
                return Environment.getCurrentEnvironment().getDefaultNS();
            }
            return Environment.getCurrentEnvironment().getNamespaceForPrefix(prefix);
        }
    };

    private static final VariableContext FM_VARIABLE_CONTEXT = new VariableContext() {
        @Override
        public Object getVariableValue(String namespaceURI, String prefix, String localName)
        throws UnresolvableException {
            try {
                TemplateModel model = Environment.getCurrentEnvironment().getVariable(localName);
                if (model == null) {
                    throw new UnresolvableException("Variable \"" + localName + "\" not found.");
                }
                if (model instanceof TemplateStringModel) {
                    return ((TemplateStringModel) model).getAsString();
                }
                if (model instanceof TemplateNumberModel) {
                    return ((TemplateNumberModel) model).getAsNumber();
                }
                if (model instanceof TemplateDateModel) {
                    return ((TemplateDateModel) model).getAsDate();
                }
                if (model instanceof TemplateBooleanModel) {
                    return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
                }
            } catch (TemplateException e) {
                throw new UndeclaredThrowableException(e);
            }
            throw new UnresolvableException(
                    "Variable \"" + localName + "\" exists, but it's not a string, number, date, or boolean");
        }
    };
     
    private static final FunctionContext FM_FUNCTION_CONTEXT = new XPathFunctionContext() {
        @Override
        public Function getFunction(String namespaceURI, String prefix, String localName)
        throws UnresolvableException {
            try {
                return super.getFunction(namespaceURI, prefix, localName);
            } catch (UnresolvableException e) {
                return super.getFunction(null, null, localName);
            }
        }
    };
    
    /**
     * Stores the the template parsed as {@link Document} in the template itself.
     */
    private static final CustomStateKey<_ObjectHolder<Document>> FM_DOM_NAVIAGOTOR_CACHED_DOM
            = new CustomStateKey<_ObjectHolder<Document>>() {
        @Override
        protected _ObjectHolder<Document> create() {
            return new _ObjectHolder<>(null);
        }
    };
     
    private static final Navigator FM_DOM_NAVIGATOR = new DocumentNavigator() {
        @Override
        public Object getDocument(String uri) throws FunctionCallException {
            try {
                Template raw = getTemplate(uri);
                _ObjectHolder<Document> docHolder = Environment.getCurrentEnvironmentNotNull()
                        .getCurrentTemplate().getCustomState(FM_DOM_NAVIAGOTOR_CACHED_DOM);
                synchronized (docHolder) {
                    Document doc = docHolder.get();
                    if (doc == null) {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        FmEntityResolver er = new FmEntityResolver();
                        builder.setEntityResolver(er);
                        doc = builder.parse(createInputSource(null, raw));
                        // If the entity resolver got called 0 times, the document
                        // is standalone, so we can safely cache it
                        if (er.getCallCount() == 0) {
                            docHolder.set(doc);
                        }
                    }
                    return doc;
                }
            } catch (Exception e) {
                throw new FunctionCallException("Failed to parse document for URI: " + uri, e);
            }
        }
    };

    // [FM3] Look into this "hidden" feature
    static Template getTemplate(String systemId) throws IOException {
        Environment env = Environment.getCurrentEnvironment();
        String templatePath = env.getCurrentTemplate().getLookupName();
        int lastSlash = templatePath.lastIndexOf('/');
        templatePath = lastSlash == -1 ? "" : templatePath.substring(0, lastSlash + 1);
        systemId = env.toFullTemplateName(templatePath, systemId);
        return env.getConfiguration().getTemplate(systemId, env.getLocale());
    }

    private static InputSource createInputSource(String publicId, Template raw) throws IOException, SAXException {
        StringWriter sw = new StringWriter();
        try {
            raw.process(Collections.EMPTY_MAP, sw);
        } catch (TemplateException e) {
            throw new SAXException(e);
        }
        InputSource is = new InputSource();
        is.setPublicId(publicId);
        is.setSystemId(raw.getLookupName());
        is.setCharacterStream(new StringReader(sw.toString()));
        return is;
    }

    private static class FmEntityResolver implements EntityResolver {
        private int callCount = 0;
        
        @Override
        public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException {
            ++callCount;
            return createInputSource(publicId, getTemplate(systemId));
        }
        
        int getCallCount() {
            return callCount;
        }
    }
}
