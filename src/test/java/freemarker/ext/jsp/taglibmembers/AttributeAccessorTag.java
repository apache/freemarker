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

package freemarker.ext.jsp.taglibmembers;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import freemarker.template.utility.StringUtil;

public abstract class AttributeAccessorTag extends SimpleTagSupport {

    protected String name;
    protected String scope;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @SuppressWarnings("boxing")
    protected Integer getScopeAsInteger() {
        if (scope == null) {
            return null;
        }
        if (scope.equals("page")) {
            return PageContext.PAGE_SCOPE;
        }
        if (scope.equals("request")) {
            return PageContext.REQUEST_SCOPE;
        }
        if (scope.equals("session")) {
            return PageContext.SESSION_SCOPE;
        }
        if (scope.equals("application")) {
            return PageContext.APPLICATION_SCOPE;
        }
        throw new IllegalArgumentException("Invalid scope name: " + StringUtil.jQuote(scope));
    }
    
}
