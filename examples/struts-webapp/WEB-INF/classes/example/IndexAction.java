package example;

import java.util.*;
import javax.servlet.http.*;
import org.apache.struts.action.*;

public class IndexAction extends GuestbookAction {
    public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        List snapShot;
        
        synchronized (getGuestbook()) {
            snapShot = (List) getGuestbook().clone();
        }
        req.setAttribute("guestbook", snapShot);
        
        return mapping.findForward("success");
    }
}
