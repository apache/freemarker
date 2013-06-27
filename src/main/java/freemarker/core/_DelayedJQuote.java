package freemarker.core;

import freemarker.template.utility.StringUtil;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedJQuote extends _DelayedConversionToString {

    public _DelayedJQuote(Object object) {
        super(object);
    }

    protected String doConversion(Object obj) {
        return StringUtil.jQuote(obj);
    }

}
