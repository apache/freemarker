package freemarker.core;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public abstract class _DelayedConversionToString {

    private static final String NOT_SET = new String();
    
    private Object object;
    private String stringValue = NOT_SET;

    public _DelayedConversionToString(Object object) {
        this.object = object;
    }

    public synchronized String toString() {
        // Switch to double-check + volatile with Java 5
        if (stringValue == NOT_SET) {
            stringValue = doConversion(object);
            this.object = null;
        }
        return stringValue;
    }
    
    protected abstract String doConversion(Object obj);
    
}
