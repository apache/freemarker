package freemarker.debug;

import java.rmi.RemoteException;

/**
 * Represents the debugger-side mirror of a debugged 
 * {@link freemarker.core.Environment} object in the remote VM. This interface
 * extends {@link DebugModel}, and the properties of the Environment are exposed
 * as hash keys on it. Specifically, the following keys are supported:
 * "currentNamespace", "dataModel", "globalNamespace", "knownVariables", 
 * "mainNamespace", and "template".
 * <p>The debug model for the template supports keys "configuration" and "name".
 * <p>The debug model for the configuration supports key "sharedVariables".
 * <p>Additionally, all of the debug models for environment, template, and 
 * configuration also support all the setting keys of 
 * {@link freemarker.core.Configurable} objects. 

 * @author Attila Szegedi
 */
public interface DebuggedEnvironment extends DebugModel
{
    /**
     * Resumes the processing of the environment in the remote VM after it was 
     * stopped on a breakpoint.
     * @throws RemoteException
     */
    public void resume() throws RemoteException;
    
    /**
     * Stops the processing of the environment after it was stopped on
     * a breakpoint. Causes a {@link freemarker.core.StopException} to be
     * thrown in the processing thread in the remote VM. 
     * @throws RemoteException
     */
    public void stop() throws RemoteException;
    
    /**
     * Returns a unique identifier for this environment
     * @throws RemoteException
     */
    public long getId() throws RemoteException;
}
