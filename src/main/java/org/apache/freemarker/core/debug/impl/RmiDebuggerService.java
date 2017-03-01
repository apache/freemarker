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

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core._Debug;
import org.apache.freemarker.core.debug.Breakpoint;
import org.apache.freemarker.core.debug.DebuggerListener;
import org.apache.freemarker.core.debug.EnvironmentSuspendedEvent;
import org.apache.freemarker.core.util.UndeclaredThrowableException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version $Id
 */
class RmiDebuggerService
extends
    DebuggerService {
    private final Map templateDebugInfos = new HashMap();
    private final HashSet suspendedEnvironments = new HashSet();
    private final Map listeners = new HashMap();
    private final ReferenceQueue refQueue = new ReferenceQueue();
     

    private final RmiDebuggerImpl debugger;
    private DebuggerServer server;

    RmiDebuggerService() {
        try {
            debugger = new RmiDebuggerImpl(this);
            server = new DebuggerServer((Serializable) RemoteObject.toStub(debugger));
            server.start();
        } catch (RemoteException e) {
            e.printStackTrace();
            throw new UndeclaredThrowableException(e);
        }
    }
    
    @Override
    List getBreakpointsSpi(String templateName) {
        synchronized (templateDebugInfos) {
            TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
            return tdi == null ? Collections.EMPTY_LIST : tdi.breakpoints;
        }
    }

    List getBreakpointsSpi() {
        List sumlist = new ArrayList();
        synchronized (templateDebugInfos) {
            for (Iterator iter = templateDebugInfos.values().iterator(); iter.hasNext(); ) {
                sumlist.addAll(((TemplateDebugInfo) iter.next()).breakpoints);
            }
        }
        Collections.sort(sumlist);
        return sumlist;
    }

    // TODO See in SuppressFBWarnings
    @Override
    @SuppressFBWarnings(value={ "UW_UNCOND_WAIT", "WA_NOT_IN_LOOP" }, justification="Will have to be re-desigend; postponed.")
    boolean suspendEnvironmentSpi(Environment env, String templateName, int line)
    throws RemoteException {
        RmiDebuggedEnvironmentImpl denv = 
            (RmiDebuggedEnvironmentImpl)
                RmiDebuggedEnvironmentImpl.getCachedWrapperFor(env);
                
        synchronized (suspendedEnvironments) {
            suspendedEnvironments.add(denv);
        }
        try {
            EnvironmentSuspendedEvent breakpointEvent = 
                new EnvironmentSuspendedEvent(this, templateName, line, denv);
    
            synchronized (listeners) {
                for (Iterator iter = listeners.values().iterator(); iter.hasNext(); ) {
                    DebuggerListener listener = (DebuggerListener) iter.next();
                    listener.environmentSuspended(breakpointEvent);
                }
            }
            synchronized (denv) {
                try {
                    denv.wait();
                } catch (InterruptedException e) {
                    // Intentionally ignored
                }
            }
            return denv.isStopped();
        } finally {
            synchronized (suspendedEnvironments) {
                suspendedEnvironments.remove(denv);
            }
        }
    }
    
    @Override
    void registerTemplateSpi(Template template) {
        String templateName = template.getName();
        synchronized (templateDebugInfos) {
            TemplateDebugInfo tdi = createTemplateDebugInfo(templateName);
            tdi.templates.add(new TemplateReference(templateName, template, refQueue));
            // Inject already defined breakpoints into the template
            for (Iterator iter = tdi.breakpoints.iterator(); iter.hasNext(); ) {
                Breakpoint breakpoint = (Breakpoint) iter.next();
                _Debug.insertDebugBreak(template, breakpoint.getLine());
            }
        }
    }
    
    Collection getSuspendedEnvironments() {
        return (Collection) suspendedEnvironments.clone();
    }

    Object addDebuggerListener(DebuggerListener listener) {
        Object id; 
        synchronized (listeners) {
            id = Long.valueOf(System.currentTimeMillis());
            listeners.put(id, listener);
        }
        return id;
    }
    
    void removeDebuggerListener(Object id) {
        synchronized (listeners) {
            listeners.remove(id);
        }
    }

    void addBreakpoint(Breakpoint breakpoint) {
        String templateName = breakpoint.getTemplateName();
        synchronized (templateDebugInfos) {
            TemplateDebugInfo tdi = createTemplateDebugInfo(templateName);
            List breakpoints = tdi.breakpoints;
            int pos = Collections.binarySearch(breakpoints, breakpoint);
            if (pos < 0) {
                // Add to the list of breakpoints
                breakpoints.add(-pos - 1, breakpoint);
                // Inject the breakpoint into all templates with this name
                for (Iterator iter = tdi.templates.iterator(); iter.hasNext(); ) {
                    TemplateReference ref = (TemplateReference) iter.next();
                    Template t = ref.getTemplate();
                    if (t == null) {
                        iter.remove();
                    } else {
                        _Debug.insertDebugBreak(t, breakpoint.getLine());
                    }
                }
            }
        }
    }

    private TemplateDebugInfo findTemplateDebugInfo(String templateName) {
        processRefQueue();
        return (TemplateDebugInfo) templateDebugInfos.get(templateName); 
    }
    
    private TemplateDebugInfo createTemplateDebugInfo(String templateName) {
        TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
        if (tdi == null) {
            tdi = new TemplateDebugInfo();
            templateDebugInfos.put(templateName, tdi);
        }
        return tdi;
    }
    
    void removeBreakpoint(Breakpoint breakpoint) {
        String templateName = breakpoint.getTemplateName();
        synchronized (templateDebugInfos) {
            TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
            if (tdi != null) {
                List breakpoints = tdi.breakpoints;
                int pos = Collections.binarySearch(breakpoints, breakpoint);
                if (pos >= 0) { 
                    breakpoints.remove(pos);
                    for (Iterator iter = tdi.templates.iterator(); iter.hasNext(); ) {
                        TemplateReference ref = (TemplateReference) iter.next();
                        Template t = ref.getTemplate();
                        if (t == null) {
                            iter.remove();
                        } else {
                            _Debug.removeDebugBreak(t, breakpoint.getLine());
                        }
                    }
                }
                if (tdi.isEmpty()) {
                    templateDebugInfos.remove(templateName);
                }
            }
        }
    }

    void removeBreakpoints(String templateName) {
        synchronized (templateDebugInfos) {
            TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
            if (tdi != null) {
                removeBreakpoints(tdi);
                if (tdi.isEmpty()) {
                    templateDebugInfos.remove(templateName);
                }
            }
        }
    }

    void removeBreakpoints() {
        synchronized (templateDebugInfos) {
            for (Iterator iter = templateDebugInfos.values().iterator(); iter.hasNext(); ) {
                TemplateDebugInfo tdi = (TemplateDebugInfo) iter.next(); 
                removeBreakpoints(tdi);
                if (tdi.isEmpty()) {
                    iter.remove();
                }
            }
        }
    }

    private void removeBreakpoints(TemplateDebugInfo tdi) {
        tdi.breakpoints.clear();
        for (Iterator iter = tdi.templates.iterator(); iter.hasNext(); ) {
            TemplateReference ref = (TemplateReference) iter.next();
            Template t = ref.getTemplate();
            if (t == null) {
                iter.remove();
            } else {
                _Debug.removeDebugBreaks(t);
            }
        }
    }

    private static final class TemplateDebugInfo {
        final List templates = new ArrayList();
        final List breakpoints = new ArrayList();
        
        boolean isEmpty() {
            return templates.isEmpty() && breakpoints.isEmpty();
        }
    }
    
    private static final class TemplateReference extends WeakReference {
        final String templateName;
         
        TemplateReference(String templateName, Template template, ReferenceQueue queue) {
            super(template, queue);
            this.templateName = templateName;
        }
        
        Template getTemplate() {
            return (Template) get();
        }
    }
    
    private void processRefQueue() {
        for (; ; ) {
            TemplateReference ref = (TemplateReference) refQueue.poll();
            if (ref == null) {
                break;
            }
            TemplateDebugInfo tdi = findTemplateDebugInfo(ref.templateName);
            if (tdi != null) {
                tdi.templates.remove(ref);
                if (tdi.isEmpty()) {
                    templateDebugInfos.remove(ref.templateName);
                }
            }
        }
    }

    @Override
    void shutdownSpi() {
        server.stop();
        try {
            UnicastRemoteObject.unexportObject(debugger, true);
        } catch (Exception e) {
        }

        RmiDebuggedEnvironmentImpl.cleanup();
    }
}
