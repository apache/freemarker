package freemarker.core;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedGetMessage extends _DelayedConversionToString  {

    public _DelayedGetMessage(Throwable exception) {
        super(exception);
    }

    protected String doConversion(Object obj) {
        return ((Throwable) obj).getMessage();
    }
    
}
