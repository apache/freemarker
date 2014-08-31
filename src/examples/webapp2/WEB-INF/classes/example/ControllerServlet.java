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

package example;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import javax.servlet.*;
import javax.servlet.http.*;
import freemarker.template.*;
import freemarker.ext.beans.BeansWrapper;


/**
 * <p>This is very very primitive MVC Controller servlet base class, based
 * on example 1. The application specific controller servlet should extend
 * this class.
 */
public class ControllerServlet extends HttpServlet {

    // Volatile so that it's properly published according to JSR 133 (JMM).
    // Although, the servlet container most certainly makes this unnecessarry.
    private volatile Configuration cfg; 
    
    public void init() {
        // Initialize the FreeMarker configuration;
        // - Create a configuration instance, with the not-100%-backward-compatible
        //   fixes up until FreeMarker 2.3.21 applied (as far as it starts
        //   with 2.3, these are only minor changes that doesn't affect most apps):
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
        // - Templates are stoted in the WEB-INF/templates directory of the Web app.
        cfg.setServletContextForTemplateLoading(
                getServletContext(), "WEB-INF/templates");
        // - At most how often should FreeMarker check if a template was updated:
        cfg.setTemplateUpdateDelay(isInDevelopmentMode() ? 0 : 60);
        // - When developing, set an error handler that prints errors so they are
        //   readable with a Web browser, otherwise we just let the HTTP 500
        //   handler deal with it.
        cfg.setTemplateExceptionHandler(
                isInDevelopmentMode()
                        ? TemplateExceptionHandler.HTML_DEBUG_HANDLER
                        : TemplateExceptionHandler.RETHROW_HANDLER);
        // - Set the default charset of the template files
        cfg.setDefaultEncoding("ISO-8859-1");
        // - Set the charset of the output. This is actually just a hint, that
        //   templates may require for URL encoding and for generating META
        //   element that uses http-equiv="Content-type".
        cfg.setOutputEncoding("UTF-8");
        // - Set the default locale
        cfg.setLocale(Locale.US);
        
        // Finished modifying cfg, so let's publish it to other threads:
        this.cfg = cfg;
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        // Choose action method
        String action = req.getServletPath();
        if (action == null) action = "index";
        if (action.startsWith("/")) action = action.substring(1);
        if (action.lastIndexOf(".") != -1) {
            action = action.substring(0, action.lastIndexOf("."));
        }
        Method actionMethod;
        try {
            actionMethod =
                    getClass().getMethod(action + "Action",
                    new Class[]{HttpServletRequest.class, Page.class});
        } catch (NoSuchMethodException e) {
            throw new ServletException("Unknown action: " + action);
        }
        
        // Set the request charset to the same as the output charset,
        // because HTML forms normally send parameters encoded with that.
        req.setCharacterEncoding(cfg.getOutputEncoding());
        
        // Call the action method
        Page page = new Page();
        try {
            actionMethod.invoke(this, new Object[]{req, page});
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        } catch (InvocationTargetException e) {
            throw new ServletException(e.getTargetException());
        }
        
        if (page.getTemplate() != null) { // show a page with a template
            // Get the template object
            Template t = cfg.getTemplate(page.getTemplate());
            
            // Prepare the HTTP response:
            // - Set the MIME-type and the charset of the output.
            //   Note that the charset should be in sync with the output_encoding setting.
            resp.setContentType("text/html; charset=" + cfg.getOutputEncoding());
            // - Prevent browser or proxy caching the page.
            //   Note that you should use it only for development and for interactive
            //   pages, as it significantly slows down the Web site.
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, "
                    + "post-check=0, pre-check=0");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "Thu, 01 Dec 1994 00:00:00 GMT");
            Writer out = resp.getWriter();
            
            // Merge the data-model and the template
            try {
                t.process(page.getRoot(), out);
            } catch (TemplateException e) {
                throw new ServletException(
                        "Error while processing FreeMarker template", e);
            }
        } else if (page.getForward() != null) { // forward request
            RequestDispatcher rd = req.getRequestDispatcher(page.getForward());
            rd.forward(req, resp);            
        } else {
            throw new ServletException("The action didn't specified a command.");
        }
    }
    
    private boolean isInDevelopmentMode() {
        // FIXME: Should detect this with a system property for example.
        return true;
    }
    
}