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
 */
public class RmiDebuggerListenerImpl
extends
    UnicastRemoteObject
implements
    DebuggerListener, Unreferenced
{
    private static final Logger LOG = Logger.getLogger(
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
            LOG.warn("Failed to unexport RMI debugger listener", e);
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
