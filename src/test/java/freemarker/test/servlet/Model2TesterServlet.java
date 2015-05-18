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

package freemarker.test.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.utility.StringUtil;

/**
 * MVC controller servlet used for {@link FreemarkerServlet} JUnit tests. It understands these request parameters:
 * <ul>
 *   <li>{@value #VIEW_PARAM_NAME}: The servlet-relative path of the URL to which we forward after the action.
 *       Required unless the action will return with non-{@code null}.
 *   <li>{@value #ACTION_PARAM_NAME}: The full qualified name of the {@link Model2Action} implementation used as the
 *       action. Optional, by default it will be {@link DefaultModel2TesterAction#INSTANCE}. 
 * </ul>
 */
public class Model2TesterServlet extends HttpServlet {
    
    private static final Logger LOG = Logger.getLogger(Model2TesterServlet.class);
    
    public static final String VIEW_PARAM_NAME = "view";
    public static final String ACTION_PARAM_NAME = "action";
    public static final String VIEW_SERVLET_PARAM_NAME = "viewServlet";

    private static final long serialVersionUID = 1L;

    private static final String CHARSET = "UTF-8";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {
        req.setCharacterEncoding(CHARSET);
        resp.setCharacterEncoding(CHARSET);

        final Model2Action action;
        {
            final String actionClassName = req.getParameter(ACTION_PARAM_NAME);
            if (actionClassName != null) {
                try {
                    action = (Model2Action) Class.forName(actionClassName).newInstance();
                } catch (final Throwable e) {
                    final String msg = "Failed to instantiate action: " + actionClassName;
                    resp.sendError(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            msg + ".\nException (see log for trace): " + e);
                    LOG.error(msg, e);
                    return;
                }
            } else {
                action = DefaultModel2TesterAction.INSTANCE;
            }
        }
        
        final String actionViewPath;
        try {
            actionViewPath = action.execute(req, resp);
        } catch (final Throwable e) {
            final String msg = action.getClass().getName() + ".execute has thrown exception";
            resp.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    msg + ".\nException (see log for trace): " + e);
            LOG.error(msg, e);
            return;
        }

        final String paramViewPath = req.getParameter(VIEW_PARAM_NAME);
        final String viewPath = removeStartingSlash(actionViewPath != null ? actionViewPath : paramViewPath);
        
        if (viewPath == null) {
            resp.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No view was specified as parameter or as action return value.");
            return;
        }
        
        final String paramViewServlet = req.getParameter(VIEW_SERVLET_PARAM_NAME);
        if (paramViewServlet == null) {
            req.getRequestDispatcher(viewPath).forward(req, resp);
        } else {
            final RequestDispatcher requestDispatcher = getServletContext().getNamedDispatcher(paramViewServlet);
            if (requestDispatcher == null) {
                throw new ServletException("Can't find request dispatched for servlet name "
                        + StringUtil.jQuote(paramViewServlet) + ".");
            }
            
            final HttpServletRequestWrapper viewReq = new HttpServletRequestWrapper(req) {

                @Override
                public String getPathInfo() {
                    return viewPath;
                }
                
            };
            
            requestDispatcher.forward(viewReq, resp);
        }
    }

    private String removeStartingSlash(final String s) {
        if (s == null) return null;
        return s.startsWith("/") ? s.substring(1) : s;
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {
        super.doGet(req, resp);
    }

}
