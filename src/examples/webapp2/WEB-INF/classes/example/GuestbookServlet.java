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
