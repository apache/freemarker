package freemarker.core;

import freemarker.template.utility.StringUtil;

/** Don't use; used internally by FreeMarker, might changes without notice. */
public class Internal_DelayedJQuote extends Internal_DelayedConversionToString {

    public Internal_DelayedJQuote(Object object) {
        super(object);
    }

    protected String doConversion(Object obj) {
        return StringUtil.jQuote(obj);
    }

}
