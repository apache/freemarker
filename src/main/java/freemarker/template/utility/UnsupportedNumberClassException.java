package freemarker.template.utility;

/**
 * Thrown when FreeMarker runs into a {@link Number} subclass that it doesn't yet support.  
 */
public class UnsupportedNumberClassException extends RuntimeException {

    private final Class fClass;
    
    public UnsupportedNumberClassException(Class pClass) {
        super("Unsupported number class: " + pClass.getName());
        fClass = pClass;
    }
    
    public Class getUnsupportedClass() {
        return fClass;
    }
    
}
