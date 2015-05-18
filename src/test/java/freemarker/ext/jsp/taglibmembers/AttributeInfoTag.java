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

import java.io.IOException;
import java.util.Date;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.TrivialDateToISO8601CalendarFactory;

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
            formattedVal = DateUtil.dateToISO8601String((Date) attrVal, true, true, true, DateUtil.ACCURACY_SECONDS,
                    DateUtil.UTC,
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
