/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

 */
public interface DebuggedEnvironment extends DebugModel
{
    /**
     * Resumes the processing of the environment in the remote VM after it was 
     * stopped on a breakpoint.
     */
    public void resume() throws RemoteException;
    
    /**
     * Stops the processing of the environment after it was stopped on
     * a breakpoint. Causes a {@link freemarker.core.StopException} to be
     * thrown in the processing thread in the remote VM. 
     */
    public void stop() throws RemoteException;
    
    /**
     * Returns a unique identifier for this environment
     */
    public long getId() throws RemoteException;
}
