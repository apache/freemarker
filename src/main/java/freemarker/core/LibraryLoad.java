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
public final class LibraryLoad extends TemplateElement {

    private Expression importedTemplateNameExp;
    private String namespace;

    /**
     * @param template the template that this <tt>Include</tt> is a part of.
     * @param templateName the name of the template to be included.
     * @param namespace the namespace to assign this library to
     */
    LibraryLoad(Template template,
            Expression templateName,
            String namespace)
    {
        this.namespace = namespace;
        this.importedTemplateNameExp = templateName;
    }

    void accept(Environment env) throws TemplateException, IOException {
        final String importedTemplateName = importedTemplateNameExp.evalAndCoerceToString(env);
        final String fullImportedTemplateName;
        try {
            fullImportedTemplateName = env.toFullTemplateName(getTemplate().getName(), importedTemplateName);
        } catch (MalformedTemplateNameException e) {
            throw new _MiscTemplateException(e, env, new Object[] {
                    "Malformed template name ", new _DelayedJQuote(e.getTemplateName()), ":\n",
                    e.getMalformednessDescription() });
        }
        
        final Template importedTemplate;
        try {
            importedTemplate = env.getTemplateForImporting(fullImportedTemplateName);
        } catch (IOException e) {
            throw new _MiscTemplateException(e, env, new Object[] {
                    "Template importing failed (for parameter value ",
                    new _DelayedJQuote(importedTemplateName),
                    "):\n", new _DelayedGetMessage(e) });
        }
        env.importLib(importedTemplate, namespace);
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(importedTemplateNameExp.getCanonicalForm());
        buf.append(" as ");
        buf.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(namespace));
        if (canonical) buf.append("/>");
        return buf.toString();
    }

    String getNodeTypeSymbol() {
        return "#import";
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return importedTemplateNameExp;
        case 1: return namespace;
        default: throw new IndexOutOfBoundsException();
        }
    }

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

    boolean isNestedBlockRepeater() {
        return false;
    }
}
