/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.ext.servlet;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * TemplateHashModel wrapper for a HttpSession attributes.
 * @author Attila Szegedi
 */

public final class HttpSessionHashModel implements TemplateHashModel, Serializable
{
    private static final long serialVersionUID = 1L;
    private transient HttpSession session;
    private transient final ObjectWrapper wrapper;

    // These are required for lazy initializing session
    private transient final FreemarkerServlet servlet;
    private transient final HttpServletRequest request;
    private transient final HttpServletResponse response;
    
    /**
     * Use this constructor when the session already exists.
     * @param session the session
     * @param wrapper an object wrapper used to wrap session attributes
     */
    public HttpSessionHashModel(HttpSession session, ObjectWrapper wrapper)
    {
        this.session = session;
        this.wrapper = wrapper;

        this.servlet = null;
        this.request = null;
        this.response = null;
    }

    /**
     * Use this constructor when the session isn't already created. It is passed
     * enough parameters so that the session can be properly initialized after
     * it's detected that it was created.
     * @param servlet the FreemarkerServlet that created this model. If the
     * model is not created through FreemarkerServlet, leave this argument as
     * null.
     * @param request the actual request
     * @param response the actual response
     * @param wrapper an object wrapper used to wrap session attributes
     */
    public HttpSessionHashModel(FreemarkerServlet servlet, HttpServletRequest request, HttpServletResponse response, ObjectWrapper wrapper)
    {
        this.wrapper = wrapper;
        
        this.servlet = servlet;
        this.request = request;
        this.response = response;
    }

    public TemplateModel get(String key) throws TemplateModelException
    {
        checkSessionExistence();
        return wrapper.wrap(session != null ? session.getAttribute(key) : null);
    }

    private void checkSessionExistence() throws TemplateModelException
    {
        if(session == null && request != null) {
            session = request.getSession(false);
            if(session != null && servlet != null) {
                try {
                    servlet.initializeSessionAndInstallModel(request, response, 
                            this, session);
                }
                catch(RuntimeException e) {
                    throw e;
                }
                catch(Exception e) {
                    throw new TemplateModelException(e);
                }
            }
        }
    }

    boolean isOrphaned(HttpSession currentSession)
    {
        return (session != null && session != currentSession) || 
            (session == null && request == null);
    }
    
    public boolean isEmpty()
    throws
        TemplateModelException
    {
        checkSessionExistence();
        return session == null || !session.getAttributeNames().hasMoreElements();
    }
}
