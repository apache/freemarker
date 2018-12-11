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

package org.apache.freemarker.servlet;

import java.io.Serializable;

import javax.servlet.GenericServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * TemplateHashModel wrapper for a HttpSession attributes.
 */
// TODO [FM3] Shouldn't this be a TemplateHashModelEx? The attribute names are known after all.
public final class HttpSessionHashModel implements TemplateHashModel, Serializable {
    private static final long serialVersionUID = 1L;
    private transient HttpSession session;
    private transient final ObjectWrapper wrapper;

    // These are required for lazy initializing session
    private transient final GenericServlet servlet;
    private transient final HttpServletRequest request;
    private transient final HttpServletResponse response;
    
    /**
     * Use this constructor when the session already exists.
     * @param session the session
     * @param wrapper an object wrapper used to wrap session attributes
     */
    public HttpSessionHashModel(HttpSession session, ObjectWrapper wrapper) {
        this.session = session;
        this.wrapper = wrapper;

        servlet = null;
        request = null;
        response = null;
    }

    /**
     * Use this constructor when the session isn't already created. It is passed
     * enough parameters so that the session can be properly initialized after
     * it's detected that it was created.
     * @param servlet the servlet (e.g, FreemarkerServlet) that created this model. If the
     * model is not created through FreemarkerServlet, this argument can be left as null.
     * @param request the actual request
     * @param response the actual response
     * @param wrapper an object wrapper used to wrap session attributes
     */
    public HttpSessionHashModel(GenericServlet servlet, HttpServletRequest request, HttpServletResponse response, ObjectWrapper wrapper) {
        this.wrapper = wrapper;

        this.servlet = servlet;
        this.request = request;
        this.response = response;
    }

    @Override
    public TemplateModel get(String key) throws TemplateException {
        checkSessionExistence();
        return wrapper.wrap(session != null ? session.getAttribute(key) : null);
    }

    private void checkSessionExistence() throws TemplateException {
        if (session == null && request != null) {
            session = request.getSession(false);
            if (session != null && servlet != null) {
                try {
                    session.setAttribute(FreemarkerServlet.ATTR_SESSION_MODEL, this);

                    if (servlet instanceof FreemarkerServlet) {
                        ((FreemarkerServlet) servlet).initializeSession(request, response);
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new TemplateException(e);
                }
            }
        }
    }

    public boolean isOrphaned(HttpSession currentSession) {
        return (session != null && session != currentSession) || 
            (session == null && request == null);
    }
    
}
