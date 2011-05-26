package example;

import java.io.IOException;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

public class GuestbookServlet extends ControllerServlet {
    /**
     * Stores the list of guestbook entries.
     * 
     * <p>Note that for the sake of simplicity, this example
     * does not try to store the guestbook persistenty.
     */
    private ArrayList guestbook = new ArrayList();
    
    public void indexAction(HttpServletRequest req, Page p) {
        List snapShot;
        synchronized (guestbook) {
            snapShot = (List) guestbook.clone();
        }
        p.put("guestbook", snapShot);
        p.setTemplate("index.ftl");
    }

    public void formAction (HttpServletRequest req, Page p)
            throws IOException, ServletException {
                
        p.put("name", noNull(req.getParameter("name")));
        p.put("email", noNull(req.getParameter("email")));
        p.put("message", noNull(req.getParameter("message")));
        List errors = (List) req.getAttribute("errors");
        p.put("errors", errors == null ? new ArrayList() : errors);

        p.setTemplate("form.ftl");
    }

    public void addAction (HttpServletRequest req, Page p)
            throws IOException, ServletException {
        List errors = new ArrayList();
        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String message = req.getParameter("message");
        if (isBlank(name)) {
            errors.add("You must give your name.");
        }
        if (isBlank(message)) {
            errors.add("You must give a message.");
        }

        // Was the sent data was correct?
        if (errors.isEmpty()) {
            if (email == null) email = "";
            // Create and insert the new guestbook entry.
            GuestbookEntry e = new GuestbookEntry(
                    name.trim(), email.trim(), message);
            synchronized (guestbook) {
                guestbook.add(0, e);
            }
            // Show "Entry added" page.
            p.put("entry", e);
            p.setTemplate("add.ftl");
        } else {
            // Go back to the page of the form
            req.setAttribute("errors", errors);
            p.setForward("form.a");
        }
    }

    public static String noNull(String s) {
        return s == null ? "" : s;
    }
    
    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0; 
    }
}
