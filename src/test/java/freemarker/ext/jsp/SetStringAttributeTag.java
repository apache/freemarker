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

package freemarker.ext.jsp;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

/**
 */
public class SetStringAttributeTag extends TagSupport {

    private String name;
    private String value;

    public SetStringAttributeTag() {
        super();
    }

    public int doStartTag() throws JspException {
        pageContext.setAttribute(name, value);
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        name = null;
        value = null;
        return SKIP_BODY;
    }
}
