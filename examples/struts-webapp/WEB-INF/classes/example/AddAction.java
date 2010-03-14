package example;

import java.util.List;
import javax.servlet.http.*;
import org.apache.struts.action.*;


public class AddAction extends GuestbookAction {

    public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        GuestbookEntryForm f = (GuestbookEntryForm) form;
        List guestbook = getGuestbook();
        synchronized (guestbook) {
            guestbook.add(0, new GuestbookEntry(
                    f.getName(), f.getEmail(), f.getMessage()));
        }
        
        return mapping.findForward("success");
    }

}
