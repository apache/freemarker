package freemarker.debug.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.List;

import freemarker.debug.Breakpoint;
import freemarker.debug.Debugger;
import freemarker.debug.DebuggerListener;

/**
 * @author Attila Szegedi
 */
class RmiDebuggerImpl
extends
    UnicastRemoteObject
implements
    Debugger
{
    private static final long serialVersionUID = 1L;

    private final RmiDebuggerService service;
    
    protected RmiDebuggerImpl(RmiDebuggerService service) throws RemoteException
    {
        this.service = service;
    }

    public void addBreakpoint(Breakpoint breakpoint)
    {
        service.addBreakpoint(breakpoint);
    }

    public Object addDebuggerListener(DebuggerListener listener)
    {
        return service.addDebuggerListener(listener);
    }

    public List getBreakpoints()
    {
        return service.getBreakpointsSpi();
    }

    public List getBreakpoints(String templateName)
    {
        return service.getBreakpointsSpi(templateName);
    }

    public Collection getSuspendedEnvironments()
    {
        return service.getSuspendedEnvironments();
    }

    public void removeBreakpoint(Breakpoint breakpoint)
    {
        service.removeBreakpoint(breakpoint);
    }

    public void removeDebuggerListener(Object id)
    {
        service.removeDebuggerListener(id);
    }

    public void removeBreakpoints()
    {
        service.removeBreakpoints();
    }

    public void removeBreakpoints(String templateName)
    {
        service.removeBreakpoints(templateName);
    }
}
