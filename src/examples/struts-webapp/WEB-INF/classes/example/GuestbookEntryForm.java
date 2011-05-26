package example;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.*;

public class GuestbookEntryForm extends ActionForm {
    private String name;
    private String email;
    private String message;
    
    public GuestbookEntryForm() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizeString(email);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = normalizeString(message);
    }

    public void setName(String name) {
        this.name = normalizeString(name);
    }

    public String getName() {
        return name;
    }
    
    public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
        ActionErrors errs = new ActionErrors();
        if (name.length() == 0) {
            errs.add("name", new ActionError("errors.required", "name"));
        }
        if (message.length() == 0) {
            errs.add("message", new ActionError("errors.required", "message"));
        }
        return errs.size() == 0 ? null : errs;
    }
    
    private static String normalizeString(String s) {
        if (s == null) return "";
        return s.trim();
    }
}