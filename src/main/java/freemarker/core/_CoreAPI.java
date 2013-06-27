package freemarker.core;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;


/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CoreAPI {
    
    // Can't be instantiated
    private _CoreAPI() { }

    public static final String STACK_SECTION_SEPARATOR = Environment.STACK_SECTION_SEPARATOR;
    
    /**
     * Returns the names of the currently supported "built-ins" ({@code expr?builtin_name}-like things).
     * @return {@link Set} of {@link String}-s. 
     */
    public static Set/*<String>*/ getSupportedBuiltInNames() {
        return Collections.unmodifiableSet(BuiltIn.builtins.keySet());
    }
    
    public static String instructionStackItemToString(TemplateElement stackEl) {
        return Environment.instructionStackItemToString(stackEl);
    }
    
    public static TemplateElement[] getInstructionStackSnapshot(Environment env) {
        return env.getInstructionStackSnapshot();
    }
    
    public static void outputInstructionStack(
            TemplateElement[] instructionStackSnapshot, PrintWriter pw) {
        Environment.outputInstructionStack(instructionStackSnapshot, pw);
    }
    
}
