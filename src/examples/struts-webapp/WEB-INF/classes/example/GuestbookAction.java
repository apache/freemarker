package example;

import java.util.*;
import org.apache.struts.action.*;

/**
 * Defines utility methods for this application. 
 */
public class GuestbookAction extends Action {
    
    ArrayList getGuestbook() {
        return (ArrayList) servlet.getServletContext()
                .getAttribute(GuestbookActionServlet.GUESTBOOK_KEY);
    }
}
