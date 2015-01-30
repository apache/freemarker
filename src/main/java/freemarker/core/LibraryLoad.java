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

import freemarker.template.Template;
import freemarker.template.TemplateException;


/**
 * An instruction that gets another template
 * and processes it within the current template.
 */
public final class LibraryLoad extends TemplateElement {

    private Expression templateName;
    private String namespace;
    private final String templatePath;

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
        String templatePath1 = template.getName();
        if (templatePath1 == null) {
            // This can be the case if the template wasn't created throuh a TemplateLoader. 
            templatePath1 = "";
        }
        int lastSlash = templatePath1.lastIndexOf('/');
        templatePath = lastSlash == -1 ? "" : templatePath1.substring(0, lastSlash + 1);
        this.templateName = templateName;
    }

    void accept(Environment env) throws TemplateException, IOException {
        String templateNameString = templateName.evalAndCoerceToString(env);
        Template importedTemplate;
        try {
            if(!env.isClassicCompatible()) {
                if (templateNameString.indexOf("://") >0) {
                    ;
                }
                else if(templateNameString.length() > 0 && templateNameString.charAt(0) == '/')  {
                    int protIndex = templatePath.indexOf("://");
                    if (protIndex >0) {
                        templateNameString = templatePath.substring(0, protIndex + 2) + templateNameString;
                    } else {
                        templateNameString = templateNameString.substring(1);
                    }
                }
                else {
                    templateNameString = templatePath + templateNameString;
                }
            }
            importedTemplate = env.getTemplateForImporting(templateNameString);
        }
        catch (ParseException e) {
            throw new _MiscTemplateException(e, env, new Object[] {
                    "Error parsing imported template ",
                    new _DelayedJQuote(templateNameString), ":\n",
                    new _DelayedGetMessage(e) });
            }
        catch (IOException ioe) {
            throw new _MiscTemplateException(ioe, env, new Object[] {
                    "Error reading imported template ", templateNameString });
        }
        env.importLib(importedTemplate, namespace);
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(templateName.getCanonicalForm());
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
        case 0: return templateName;
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
        return templateName.toString();
    }
}
