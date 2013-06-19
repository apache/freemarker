package freemarker.core;

import freemarker.template.TemplateException;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedGetMessageWithoutStackTop extends _DelayedConversionToString  {

    public _DelayedGetMessageWithoutStackTop(TemplateException exception) {
        super(exception);
    }

    protected String doConversion(Object obj) {
        return ((TemplateException) obj).getMessageWithoutStackTop();
    }
    
}
