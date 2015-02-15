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
import java.util.ArrayList;
import java.util.Enumeration;

import freemarker.template.TemplateException;

/**
 * Encapsulates an array of <tt>TemplateElement</tt> objects. 
 */
final class MixedContent extends TemplateElement {

    MixedContent()
    {
        nestedElements = new ArrayList();
    }

    void addElement(TemplateElement element) {
        nestedElements.add(element);
    }

    void addElement(int index, TemplateElement element) {
        nestedElements.add(index, element);
    }
    
    TemplateElement postParseCleanup(boolean stripWhitespace)
        throws ParseException 
    {
        super.postParseCleanup(stripWhitespace);
        if (nestedElements.size() == 1) {
            return (TemplateElement) nestedElements.get(0);
        }
        return this;
    }

    /**
     * Processes the contents of the internal <tt>TemplateElement</tt> list,
     * and outputs the resulting text.
     */
    void accept(Environment env) 
        throws TemplateException, IOException 
    {
        for (int i=0; i<nestedElements.size(); i++) {
            TemplateElement element = (TemplateElement) nestedElements.get(i);
            env.visit(element);
        }
    }

    protected String dump(boolean canonical) {
        if (canonical) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i<nestedElements.size(); i++) {
                TemplateElement element = (TemplateElement) nestedElements.get(i);
                buf.append(element.getCanonicalForm());
            }
            return buf.toString();
        } else {
            if (parent == null) {
                return "root";
            }
            return getNodeTypeSymbol(); // MixedContent is uninteresting in a stack trace.
        }
    }

    protected boolean isOutputCacheable() {
        for (Enumeration children = children(); children.hasMoreElements();) {
            if (!((TemplateElement) children.nextElement()).isOutputCacheable()) {
                return false;
            }
        }
        return true;
    }

    String getNodeTypeSymbol() {
        return "#mixed_content";
    }
    
    int getParameterCount() {
        return 0;
    }

    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }
    
    boolean isShownInStackTrace() {
        return false;
    }
    
    boolean isIgnorable() {
        return nestedElements == null || nestedElements.size() == 0;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
}
