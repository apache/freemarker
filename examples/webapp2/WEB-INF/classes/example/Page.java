package example;

import java.util.*;

public class Page {
    private String template;
    private String forward;
    private Map root = new HashMap();

    public String getTemplate() {
        return template;
    }
    
    public void setTemplate(String template) {
        forward = null;
        this.template = template;
    }

    public void put(String name, Object value) {
        root.put(name, value);
    }
    
    public void put(String name, int value) {
        root.put(name, new Integer(value));
    }
    
    public void put(String name, double value) {
        root.put(name, new Double(value));
    }

    public void put(String name, boolean value) {
        root.put(name, new Boolean(value));
    }
    
    public Map getRoot() {
        return root;
    }
    
    public String getForward() {
        return forward;
    }

    public void setForward(String forward) {
        template = null;
        this.forward = forward;
    }

}
