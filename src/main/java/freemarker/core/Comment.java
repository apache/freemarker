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

import freemarker.template.utility.StringUtil;

/**
 * <b>Internal API - subject to change:</b> A template element where the content is ignored, a Comment.
 * 
 * @deprecated This is an internal FreeMarker API with no backward compatibility guarantees, so you shouldn't depend on
 *             it.
 */
public final class Comment extends TemplateElement {

    private final String text;

    Comment(String text) {
        this.text = text;
    }

    void accept(Environment env) {
        // do nothing, skip the body
    }

    protected String dump(boolean canonical) {
        if (canonical) {
            return "<#--" + text + "-->";
        } else {
            return "comment " + StringUtil.jQuote(text.trim());
        }
    }
    
    String getNodeTypeSymbol() {
        return "#--...--";
    }
    

    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return text;
    }

    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.CONTENT;
    }

    public String getText() {
        return text;
    }

    boolean isOutputCacheable() {
        return true;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
