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

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.freemarker.core.util._NullArgumentException;

public class GetAndSetTag extends AttributeAccessorTag {
    
    private Object value;
    
    @SuppressWarnings("boxing")
    @Override
    public void doTag() throws JspException, IOException {
        _NullArgumentException.check("name", name);
        Integer scopeInt = getScopeAsInteger();

        JspContext ctx = getJspContext();
        JspWriter out = ctx.getOut();
        
        out.write(scope == null ? "any:" : scope + ":");
        out.write(name);
        out.write(" was ");
        out.write(String.valueOf((scope == null ? ctx.getAttribute(name) : ctx.getAttribute(name, scopeInt))));
        if (scope == null) {
            ctx.setAttribute(name, value);
        } else {
            ctx.setAttribute(name, value, scopeInt);
        }
        out.write(", set to ");
        out.write(String.valueOf(value));
        out.write("\n");
        
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
