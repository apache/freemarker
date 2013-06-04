package freemarker.core;

import java.util.Collections;
import java.util.Set;


/**
 * For internal usage only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class Internal_CoreAPI {
    
    private Internal_CoreAPI() { }

    /**
     * Returns the names of the currently supported "built-ins" ({@code expr?builtin_name}-like things).
     * @return {@link Set} of {@link String}-s. 
     */
    public static Set/*<String>*/ getSupportedBuiltInNames() {
        return Collections.unmodifiableSet(BuiltIn.builtins.keySet());
    }
    
}
