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
import java.util.Date;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._DateUtils.TrivialDateToISO8601CalendarFactory;

public class AttributeInfoTag extends AttributeAccessorTag {

    @SuppressWarnings("boxing")
    @Override
    public void doTag() throws JspException, IOException {
        JspContext ctx = getJspContext();
        JspWriter out = ctx.getOut();
        final Integer scope = getScopeAsInteger();
        Object attrVal = scope == null ? ctx.getAttribute(name) : ctx.getAttribute(name, scope);
        
        final String formattedVal;
        if (attrVal instanceof Date) {
            formattedVal = _DateUtils.dateToISO8601String((Date) attrVal, true, true, true, _DateUtils.ACCURACY_SECONDS,
                    _DateUtils.UTC,
                    new TrivialDateToISO8601CalendarFactory());
        } else {
            formattedVal = String.valueOf(attrVal);
        }
        
        out.write(formattedVal);
        if (attrVal != null) {
            out.write(" [");
            out.write(attrVal.getClass().getName());
            out.write("]");
        }
    }

}
