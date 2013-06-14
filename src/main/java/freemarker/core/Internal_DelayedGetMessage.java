package freemarker.core;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class Internal_DelayedGetMessage extends Internal_DelayedConversionToString  {

    public Internal_DelayedGetMessage(Throwable exception) {
        super(exception);
    }

    protected String doConversion(Object obj) {
        return ((Throwable) obj).getMessage();
    }
    
}
