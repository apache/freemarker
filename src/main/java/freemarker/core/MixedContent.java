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

import freemarker.template.TemplateException;

/**
 * Encapsulates an array of <tt>TemplateElement</tt> objects. 
 */
final class MixedContent extends TemplateElement {

    MixedContent() { }
    
    /**
     * @deprecated Use {@link #addChild(TemplateElement)} instead.
     */
    @Deprecated
    void addElement(TemplateElement element) {
        addChild(element);
    }

    /**
     * @deprecated Use {@link #addChild(int, TemplateElement)} instead.
     */
    @Deprecated
    void addElement(int index, TemplateElement element) {
        addChild(index, element);
    }
    
    @Override
    TemplateElement postParseCleanup(boolean stripWhitespace)
        throws ParseException {
        super.postParseCleanup(stripWhitespace);
        return getChildCount() == 1 ? getChild(0) : this;
    }

    /**
     * Processes the contents of the internal <tt>TemplateElement</tt> list,
     * and outputs the resulting text.
     */
    @Override
    TemplateElement[] accept(Environment env)
        throws TemplateException, IOException {
        return getChildBuffer();
    }

    @Override
    protected String dump(boolean canonical) {
        if (canonical) {
            return getChildrenCanonicalForm();
        } else {
            if (getParentElement() == null) {
                return "root";
            }
            return getNodeTypeSymbol(); // MixedContent is uninteresting in a stack trace.
        }
    }

    @Override
    protected boolean isOutputCacheable() {
        int ln = getChildCount();
        for (int i = 0; i < ln; i++) {
            if (!getChild(i).isOutputCacheable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    String getNodeTypeSymbol() {
        return "#mixed_content";
    }
    
    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }
    
    @Override
    boolean isIgnorable(boolean stripWhitespace) {
        return getChildCount() == 0;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
}
