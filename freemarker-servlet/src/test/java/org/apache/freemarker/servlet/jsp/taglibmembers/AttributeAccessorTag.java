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

package org.apache.freemarker.servlet.jsp.taglibmembers;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.freemarker.core.util._StringUtils;

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
        throw new IllegalArgumentException("Invalid scope name: " + _StringUtils.jQuote(scope));
    }
    
}
