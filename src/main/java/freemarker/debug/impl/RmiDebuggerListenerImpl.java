package freemarker.debug.impl;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

import freemarker.debug.DebuggerClient;
import freemarker.debug.DebuggerListener;
import freemarker.debug.EnvironmentSuspendedEvent;
import freemarker.log.Logger;

/**
 * Used by the {@link DebuggerClient} to create local 
 * @author Attila Szegedi
 */
public class RmiDebuggerListenerImpl
extends
    UnicastRemoteObject
implements
    DebuggerListener, Unreferenced
{
    private static final Logger logger = Logger.getLogger(
            "freemarker.debug.client");
    
    private static final long serialVersionUID = 1L;

    private final DebuggerListener listener;

    public void unreferenced()
    {
        try
        {
            UnicastRemoteObject.unexportObject(this, false);
        }
        catch (NoSuchObjectException e)
        {
            logger.warn("Failed to unexport RMI debugger listener", e);
        }
    }
    
    public RmiDebuggerListenerImpl(DebuggerListener listener) 
    throws RemoteException
    {
        this.listener = listener;
    }

    public void environmentSuspended(EnvironmentSuspendedEvent e) 
    throws RemoteException
    {
        listener.environmentSuspended(e);
    }
}
