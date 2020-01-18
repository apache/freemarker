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

package freemarker.ext.jsp;

import javax.servlet.jsp.PageContext;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @Deprecated This is used by the deprecated {@link FreemarkerTag}.
 */
@Deprecated
class JspContextModel
implements
    TemplateHashModel {
    public static final int ANY_SCOPE = -1;
    public static final int PAGE_SCOPE = PageContext.PAGE_SCOPE;
    public static final int REQUEST_SCOPE = PageContext.REQUEST_SCOPE;
    public static final int SESSION_SCOPE = PageContext.SESSION_SCOPE;
    public static final int APPLICATION_SCOPE = PageContext.APPLICATION_SCOPE;

    private final PageContext pageContext;
    private final int scope;

    public JspContextModel(PageContext pageContext, int scope) {
        this.pageContext = pageContext;
        this.scope = scope;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        Object bean = scope == ANY_SCOPE ? pageContext.findAttribute(key) : pageContext.getAttribute(key, scope);
        return BeansWrapper.getDefaultInstance().wrap(bean);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
