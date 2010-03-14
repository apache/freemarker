package example;

import javax.servlet.http.*;
import org.apache.struts.action.*;


public class FormAction extends GuestbookAction {

    public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        return mapping.findForward("success");
    }

}
