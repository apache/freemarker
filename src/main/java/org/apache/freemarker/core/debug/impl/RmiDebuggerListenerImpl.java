/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.debug.impl;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

import org.apache.freemarker.core._CoreLogs;
import org.apache.freemarker.core.debug.DebuggerClient;
import org.apache.freemarker.core.debug.DebuggerListener;
import org.apache.freemarker.core.debug.EnvironmentSuspendedEvent;
import org.slf4j.Logger;

/**
 * Used by the {@link DebuggerClient} to create local 
 */
public class RmiDebuggerListenerImpl
extends
    UnicastRemoteObject
implements
    DebuggerListener, Unreferenced {
    
    private static final Logger LOG = _CoreLogs.DEBUG_CLIENT;
    
    private static final long serialVersionUID = 1L;

    private final DebuggerListener listener;

    @Override
    public void unreferenced() {
        try {
            UnicastRemoteObject.unexportObject(this, false);
        } catch (NoSuchObjectException e) {
            LOG.warn("Failed to unexport RMI debugger listener", e);
        }
    }
    
    public RmiDebuggerListenerImpl(DebuggerListener listener) 
    throws RemoteException {
        this.listener = listener;
    }

    @Override
    public void environmentSuspended(EnvironmentSuspendedEvent e) 
    throws RemoteException {
        listener.environmentSuspended(e);
    }
}
