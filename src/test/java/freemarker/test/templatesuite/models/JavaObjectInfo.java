package freemarker.test.templatesuite.models;

import freemarker.template.utility.StringUtil;

public class JavaObjectInfo {
    
    public static final Object INSTANCE = new JavaObjectInfo();

    private JavaObjectInfo() { }
    
    public String info(Object o) {
        if (o == null) return "null";
        return o.getClass().getName() + " " + StringUtil.jQuote(o.toString());
    }

}
