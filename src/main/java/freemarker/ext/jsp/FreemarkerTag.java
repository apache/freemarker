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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * Simple implementation of JSP tag to allow use of FreeMarker templates in
 * JSP. Inspired by similar class in Velocity template engine developed by
 * <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 *
 * @deprecated This feature is not supported anymore, also, it uses the deprecated default {@link Configuration}.
 */
@Deprecated
public class FreemarkerTag implements BodyTag {
    private Tag parent;
    private BodyContent bodyContent;
    private PageContext pageContext;
    private SimpleHash root;
    private Template template;
    private boolean caching = true;
    private String name = "";
    
    public boolean getCaching() {
        return caching;
    }

    public void setCaching(boolean caching) {
        this.caching = caching;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }
    
    @Override
    public Tag getParent() {
        return parent;
    }

    @Override
    public void setParent(Tag parent) {
        this.parent = parent;
    }

    @Override
    public int doStartTag() {
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public void setBodyContent(BodyContent bodyContent) {
        this.bodyContent = bodyContent;
    }

    @Override
    public void setPageContext(PageContext pageContext) {
        this.pageContext = pageContext;
        root = null;
    }

    @Override
    public void doInitBody() {
    }

    @Override
    public int doAfterBody() {
        return SKIP_BODY;
    }

    @Override
    public void release() {
        root = null;
        template = null;
        name = "";
    }

    @Override
    public int doEndTag()
        throws JspException {
        if (bodyContent == null)
            return EVAL_PAGE;

        try {
            if (template == null) {
                template = new Template(name, bodyContent.getReader());
            }

            if (root == null) {
                root = new SimpleHash();
                root.put("page", new JspContextModel(pageContext, JspContextModel.PAGE_SCOPE));
                root.put("request", new JspContextModel(pageContext, JspContextModel.REQUEST_SCOPE));
                root.put("session", new JspContextModel(pageContext, JspContextModel.SESSION_SCOPE));
                root.put("application", new JspContextModel(pageContext, JspContextModel.APPLICATION_SCOPE));
                root.put("any", new JspContextModel(pageContext, JspContextModel.ANY_SCOPE));
            }
            template.process(root, pageContext.getOut());
        } catch (Exception e) {
            try {
                pageContext.handlePageException(e);
            } catch (ServletException | IOException e2) {
                throw new JspException(e2.getMessage());
            }
        } finally {
            if (!caching) {
                template = null;
            }
        }

        return EVAL_PAGE;
    }
}
