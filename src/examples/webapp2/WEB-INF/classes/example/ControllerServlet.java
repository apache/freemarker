package example;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import javax.servlet.*;
import javax.servlet.http.*;
import freemarker.core.Version;
import freemarker.template.*;
import freemarker.ext.beans.BeansWrapper;


/**
 * <p>This is very very primitive MVC Controller servlet base class, based
 * on example 1. The application specific controller servlet should extend
 * this class.
 */
public class ControllerServlet extends HttpServlet {
    private Configuration cfg; 
    
    public void init() {
        // Initialize the FreeMarker configuration;
        // - Create a configuration instance
        cfg = new Configuration();
        // - At least in new projects, specify that you want the fixes that aren't
        //   100% backward compatible too (these are always very low-risk changes):
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        // - Templates are stoted in the WEB-INF/templates directory of the Web app.
        cfg.setServletContextForTemplateLoading(
                getServletContext(), "WEB-INF/templates");
        // - Set update dealy to 0 for now, to ease debugging and testing.
        //   Higher value should be used in production environment.
        cfg.setTemplateUpdateDelay(0);
        // - When developing, set an error handler that prints errors so they are
		//   readable with a HTML browser, otherwise we just let the HTTP 500
		//   handler to deal with it.
        cfg.setTemplateExceptionHandler(
				isInDevelopmentMode()
						? TemplateExceptionHandler.HTML_DEBUG_HANDLER
						: TemplateExceptionHandler.RETHROW_HANDLER);
        // - Use beans wrapper (recommmended for most applications)
		BeansWrapper bw = new BeansWrapper();
		bw.setSimpleMapWrapper(true);
        cfg.setObjectWrapper(bw);
        // - Set the default charset of the template files
        cfg.setDefaultEncoding("ISO-8859-1");
        // - Set the charset of the output. This is actually just a hint, that
        //   templates may require for URL encoding and for generating META element
        //   that uses http-equiv="Content-type".
        cfg.setOutputEncoding("UTF-8");
        // - Set the default locale
        cfg.setLocale(Locale.US);
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
		// This should detect this with a system property for example.
		return true;
	}
	
}