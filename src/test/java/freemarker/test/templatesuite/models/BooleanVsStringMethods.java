package freemarker.test.templatesuite.models;

public class BooleanVsStringMethods {
    
    public String expectsString(String s) {
        return s;
    }

    public boolean expectsBoolean(boolean b) {
        return b;
    }
    
    public String overloaded(String s) {
        return "String " + s;
    }
    
    public String overloaded(boolean s) {
        return "boolean " + s;
    }
    
}
