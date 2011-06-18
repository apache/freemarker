package freemarker.ext.script;

import javax.script.ScriptContext;

/**
 * Various variable names that you can use in various bindings to customize the
 * operation of the scripts.
 * @author Attila Szegedi
 * @version $Id: $
 */
public class FreeMarkerScriptConstants
{
    /**
     * If you bind an instance of Boolean.TRUE under this name in your script
     * context, the eval() will return the output of the template as a string.
     * When this value is not present (or it doesn't equal Boolean.TRUE), 
     * eval() calls return null, and the template output goes to 
     * {@link ScriptContext#getWriter()}.
     */
    public static final String STRING_OUTPUT = "freeMarker.stringOutput";
    
    /**
     * If you bind an instance of Configuration under this name in your script
     * engine, it will be used when evaluting a script, compiling a script, and
     * evaluating a script compiled from that engine. However, if there is a 
     * security manager in the JVM and the invoking code does not posess the 
     * "freeMarker.script.setEngineConfiguration" runtime permission, the 
     * attempt to evaluate/compile a script will throw a SecurityException.
     * Instead of a Configuration, you can also bind an instance of Properties.
     * In this case, a new Configuration object will be constructed on each
     * evaluate/compile operation and initialized from the Properties object.
     * This is however not really recommended, as it creates a new 
     * Configuration for each template - it is also subject to the security
     * check, of course.
     */
    public static final String CONFIGURATION = "freeMarker.configuration";
}
