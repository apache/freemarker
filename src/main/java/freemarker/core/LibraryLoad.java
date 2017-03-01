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

package freemarker.core;

import java.io.IOException;

import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * <b>Internal API - subject to change:</b> Represents an import via {@code #import}.
 * 
 * @deprecated This is an internal FreeMarker API with no backward compatibility guarantees, so you shouldn't depend on
 *             it.
 */
@Deprecated
public final class LibraryLoad extends TemplateElement {

    private Expression importedTemplateNameExp;
    private String targetNsVarName;

    /**
     * @param template the template that this <tt>Include</tt> is a part of.
     * @param templateName the name of the template to be included.
     * @param targetNsVarName the name of the  variable to assign this library's namespace to
     */
    LibraryLoad(Template template,
            Expression templateName,
            String targetNsVarName) {
        this.targetNsVarName = targetNsVarName;
        this.importedTemplateNameExp = templateName;
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        final String importedTemplateName = importedTemplateNameExp.evalAndCoerceToPlainText(env);
        final String fullImportedTemplateName;
        try {
            fullImportedTemplateName = env.toFullTemplateName(getTemplate().getName(), importedTemplateName);
        } catch (MalformedTemplateNameException e) {
            throw new _MiscTemplateException(e, env,
                    "Malformed template name ", new _DelayedJQuote(e.getTemplateName()), ":\n",
                    e.getMalformednessDescription());
        }
        
        try {
            env.importLib(fullImportedTemplateName, targetNsVarName);
        } catch (IOException e) {
            throw new _MiscTemplateException(e, env,
                    "Template importing failed (for parameter value ",
                    new _DelayedJQuote(importedTemplateName),
                    "):\n", new _DelayedGetMessage(e));
        }
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(importedTemplateNameExp.getCanonicalForm());
        buf.append(" as ");
        buf.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(targetNsVarName));
        if (canonical) buf.append("/>");
        return buf.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#import";
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return importedTemplateNameExp;
        case 1: return targetNsVarName;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.TEMPLATE_NAME;
        case 1: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }    
    
    public String getTemplateName() {
        return importedTemplateNameExp.toString();
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}
