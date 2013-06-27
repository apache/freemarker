package freemarker.template.utility;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The equivalent of JDK 1.3 UndeclaredThrowableException.
 * @author Attila Szegedi
 */
public class UndeclaredThrowableException extends RuntimeException
{
    private final Throwable t;
    
    public UndeclaredThrowableException(Throwable t)
    {
        this.t = t;
    }
    
    public void printStackTrace()
    {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream ps)
    {
        synchronized (ps)
        {
            ps.print("Undeclared throwable:");
            t.printStackTrace(ps);
        }
    }

    public void printStackTrace(PrintWriter pw)
    {
        synchronized (pw)
        {
            pw.print("Undeclared throwable:");
            t.printStackTrace(pw);
        }
    }
    
    public Throwable getUndeclaredThrowable() {
        return t;
    }
    
    public Throwable getCause() {
    	return t;
    }
    
}
