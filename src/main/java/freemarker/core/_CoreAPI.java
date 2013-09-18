package freemarker.core;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

import freemarker.template.ObjectWrapper;
import freemarker.template.Version;


/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _CoreAPI {
    
    // Can't be instantiated
    private _CoreAPI() { }

    public static final String STACK_SECTION_SEPARATOR = Environment.STACK_SECTION_SEPARATOR;
    
    public static final int DEFAULT_TL_AND_OW_CHANGE_VERSION = Configurable.DEFAULT_TL_AND_OW_CHANGE_VERSION;
    
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
    
    public static ObjectWrapper getDefaultObjectWrapper(Version incompatibleImprovements) {
        return Configurable.getDefaultObjectWrapper(incompatibleImprovements);
    }
    
}
