package freemarker.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * An interface for components that wish to receive debugging events.
 * @author Attila Szegedi
 * @version $Id: DebuggerListener.java,v 1.1 2003/05/02 15:55:48 szegedia Exp $
 */
public interface DebuggerListener extends Remote, EventListener
{
    /**
     * Called whenever an environment gets suspended (ie hits a breakpoint).
     * @param e the event object
     * @throws RemoteException
     */
    public void environmentSuspended(EnvironmentSuspendedEvent e)
    throws
        RemoteException;
}
