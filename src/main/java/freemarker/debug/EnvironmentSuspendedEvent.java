package freemarker.debug;

import java.io.Serializable;
import java.util.EventObject;

/**
 * Event describing a suspension of an environment (ie because it hit a
 * breakpoint).
 * @author Attila Szegedi
 */
public class EnvironmentSuspendedEvent extends EventObject
{
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int line;
    private final Serializable data;
    private final DebuggedEnvironment env;

    public EnvironmentSuspendedEvent(Object source, String templateName, int line, Serializable data, DebuggedEnvironment env)
    {
        super(source);
        this.name = templateName;
        this.line = line;
        this.data = data;
        this.env = env;
    }

    /**
     * The name of the template where the execution of the environment
     * was suspended
     * @return String the template name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * The line number in the template where the execution of the environment
     * was suspended.
     * @return int the line number
     */
    public int getLine()
    {
        return line;
    }
    
    /**
     * Returns the arbitrary data that the debugger clients has attached to the {@link Breakpoint};
     * possibly {@code null}.
     */
    public Serializable getData() {
        return data;
    }

    /**
     * The environment that was suspended
     * @return DebuggedEnvironment
     */
    public DebuggedEnvironment getEnvironment()
    {
        return env;
    }
}
