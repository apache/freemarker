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

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import freemarker.template.*;

/**
 * This Servlet does not do anything useful, just prints "Hello World!". The
 * intent is to help you to get started if you want to build your own Controller
 * servlet that uses FreeMarker for the View. For more advanced example, see the
 * 2nd Web application example.
 */
public class HelloServlet extends HttpServlet {

    // Volatile so that it's properly published according to JSR 133 (JMM).
    // Although, the servlet container most certainly makes this unnecessarry.
    private volatile Configuration cfg; 
    
    public void init() {
        // Initialize the FreeMarker configuration;
        // - Create a configuration instance with the defaults of FreeMarker 2.3.21
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
        // - Templates are stoted in the WEB-INF/templates directory of the Web app.
        cfg.setServletContextForTemplateLoading(
            getServletContext(), "WEB-INF/templates");
        // - Give the standard error page on template errors (HTTP 500, usually):
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        // You should set various other settings in a real app.
        // See the "webapp2" example for them.
        
        // Finished modifying cfg, so let's publish it to other threads:
        this.cfg = cfg;
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        // Build the data-model
        Map root = new HashMap();
        root.put("message", "Hello World!");
        
        // Get the templat object
        Template t = cfg.getTemplate("test.ftl");
        
        // Prepare the HTTP response:
        // - Use the charset of template for the output
        // - Use text/html MIME-type
        resp.setContentType("text/html; charset=" + t.getEncoding());
        Writer out = resp.getWriter();
        
        // Merge the data-model and the template
        try {
            t.process(root, out);
        } catch (TemplateException e) {
            throw new ServletException(
                    "Error while processing FreeMarker template", e);
        }
    }
}