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

package freemarker.debug.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.List;

import freemarker.debug.Breakpoint;
import freemarker.debug.Debugger;
import freemarker.debug.DebuggerListener;

/**
 */
class RmiDebuggerImpl
extends
    UnicastRemoteObject
implements
    Debugger {
    private static final long serialVersionUID = 1L;

    private final RmiDebuggerService service;
    
    protected RmiDebuggerImpl(RmiDebuggerService service) throws RemoteException {
        this.service = service;
    }

    @Override
    public void addBreakpoint(Breakpoint breakpoint) {
        service.addBreakpoint(breakpoint);
    }

    @Override
    public Object addDebuggerListener(DebuggerListener listener) {
        return service.addDebuggerListener(listener);
    }

    @Override
    public List getBreakpoints() {
        return service.getBreakpointsSpi();
    }

    @Override
    public List getBreakpoints(String templateName) {
        return service.getBreakpointsSpi(templateName);
    }

    @Override
    public Collection getSuspendedEnvironments() {
        return service.getSuspendedEnvironments();
    }

    @Override
    public void removeBreakpoint(Breakpoint breakpoint) {
        service.removeBreakpoint(breakpoint);
    }

    @Override
    public void removeDebuggerListener(Object id) {
        service.removeDebuggerListener(id);
    }

    @Override
    public void removeBreakpoints() {
        service.removeBreakpoints();
    }

    @Override
    public void removeBreakpoints(String templateName) {
        service.removeBreakpoints(templateName);
    }
}
