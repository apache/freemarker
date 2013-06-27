package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.utility.ClassUtil;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedFTLTypeDescription extends _DelayedConversionToString {
    
    public _DelayedFTLTypeDescription(TemplateModel tm) {
        super(tm);
    }

    protected String doConversion(Object obj) {
        return ClassUtil.getFTLTypeDescription((TemplateModel) obj);
    }

}
