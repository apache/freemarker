package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.utility.ClassUtil;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class Internal_DelayedFTLTypeDescription extends Internal_DelayedConversionToString {
    
    public Internal_DelayedFTLTypeDescription(TemplateModel tm) {
        super(tm);
    }

    protected String doConversion(Object obj) {
        return ClassUtil.getFTLTypeDescription((TemplateModel) obj);
    }

}
