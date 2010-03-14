package example;

import java.util.*;
import javax.servlet.*;

import org.apache.struts.action.ActionServlet;

public class GuestbookActionServlet extends ActionServlet {
    
    static final String GUESTBOOK_KEY = "guestbook";
    
    public void init(ServletConfig scfg) throws ServletException {
        super.init(scfg);
        
        ServletContext sctx = scfg.getServletContext();
        
        // Add application specific global objects
        sctx.setAttribute(GUESTBOOK_KEY, new ArrayList()); 
    }
}
