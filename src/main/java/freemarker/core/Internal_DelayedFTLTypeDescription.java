package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.utility.ClassUtil;

public class Internal_DelayedFTLTypeDescription {

    private final TemplateModel tm;
    private String description;

    public Internal_DelayedFTLTypeDescription(TemplateModel tm) {
        this.tm = tm;
    }

    public String toString() {
        TemplateModel tm = this.tm;
        if (tm != null) {
            String s = ClassUtil.getFTLTypeDescription(tm);
            synchronized (this) {
                description = s;
                tm = null;
            }
        }
        return description;
    }
    
}
