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

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import freemarker.core.DebugBreak;
import freemarker.core.Environment;
import freemarker.core.TemplateElement;
import freemarker.debug.Breakpoint;
import freemarker.debug.DebuggerListener;
import freemarker.debug.EnvironmentSuspendedEvent;
import freemarker.template.Template;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * @version $Id
 */
class RmiDebuggerService
extends
    DebuggerService
{
    private final Map templateDebugInfos = new HashMap();
    private final HashSet suspendedEnvironments = new HashSet();
    private final Map listeners = new HashMap();
    private final ReferenceQueue refQueue = new ReferenceQueue();
     

    private final RmiDebuggerImpl debugger;
    private DebuggerServer server;

    RmiDebuggerService()
    {
        try
        {
            debugger = new RmiDebuggerImpl(this);
            server = new DebuggerServer((Serializable)RemoteObject.toStub(debugger));
            server.start();
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
            throw new UndeclaredThrowableException(e);
        }
    }
    
    List getBreakpointsSpi(String templateName)
    {
        synchronized(templateDebugInfos)
        {
            TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
            return tdi == null ? Collections.EMPTY_LIST : tdi.breakpoints;
        }
    }

    List getBreakpointsSpi()
    {
        List sumlist = new ArrayList();
        synchronized(templateDebugInfos)
        {
            for (Iterator iter = templateDebugInfos.values().iterator(); iter.hasNext();)
            {
                sumlist.addAll(((TemplateDebugInfo) iter.next()).breakpoints);
            }
        }
        Collections.sort(sumlist);
        return sumlist;
    }

    boolean suspendEnvironmentSpi(Environment env, String templateName, int line)
    throws
        RemoteException
    {
        RmiDebuggedEnvironmentImpl denv = 
            (RmiDebuggedEnvironmentImpl)
                RmiDebuggedEnvironmentImpl.getCachedWrapperFor(env);
                
        synchronized(suspendedEnvironments)
        {
            suspendedEnvironments.add(denv);
        }
        try
        {
            EnvironmentSuspendedEvent breakpointEvent = 
                new EnvironmentSuspendedEvent(this, templateName, line, denv);
    
            synchronized(listeners)
            {
                for (Iterator iter = listeners.values().iterator(); iter.hasNext();)
                {
                    DebuggerListener listener = (DebuggerListener) iter.next();
                    listener.environmentSuspended(breakpointEvent);
                }
            }
            synchronized(denv)
            {
                try
                {
                    denv.wait();
                }
                catch(InterruptedException e)
                {
                    ;// Intentionally ignored
                }
            }
            return denv.isStopped();
        }
        finally
        {
            synchronized(suspendedEnvironments)
            {
                suspendedEnvironments.remove(denv);
            }
        }
    }
    
    void registerTemplateSpi(Template template)
    {
        String templateName = template.getName();
        synchronized(templateDebugInfos)
        {
            TemplateDebugInfo tdi = createTemplateDebugInfo(templateName);
            tdi.templates.add(new TemplateReference(templateName, template, refQueue));
            // Inject already defined breakpoints into the template
            for (Iterator iter = tdi.breakpoints.iterator(); iter.hasNext();)
            {
                Breakpoint breakpoint = (Breakpoint) iter.next();
                insertDebugBreak(template, breakpoint);
            }
        }
    }
    
    Collection getSuspendedEnvironments()
    {
        return (Collection)suspendedEnvironments.clone();
    }

    Object addDebuggerListener(DebuggerListener listener)
    {
        Object id; 
        synchronized(listeners)
        {
            id = new Long(System.currentTimeMillis());
            listeners.put(id, listener);
        }
        return id;
    }
    
    void removeDebuggerListener(Object id)
    {
        synchronized(listeners)
        {
            listeners.remove(id);
        }
    }

    void addBreakpoint(Breakpoint breakpoint)
    {
        String templateName = breakpoint.getTemplateName();
        synchronized(templateDebugInfos)
        {
            TemplateDebugInfo tdi = createTemplateDebugInfo(templateName);
            List breakpoints = tdi.breakpoints;
            int pos = Collections.binarySearch(breakpoints, breakpoint);
            if(pos < 0)
            {
                // Add to the list of breakpoints
                breakpoints.add(-pos - 1, breakpoint);
                // Inject the breakpoint into all templates with this name
                for (Iterator iter = tdi.templates.iterator(); iter.hasNext();)
                {
                    TemplateReference ref = (TemplateReference) iter.next();
                    Template t = ref.getTemplate();
                    if(t == null)
                    {
                        iter.remove();
                    }
                    else
                    {
                        insertDebugBreak(t, breakpoint);
                    }
                }
            }
        }
    }

    private static void insertDebugBreak(Template t, Breakpoint breakpoint)
    {
        TemplateElement te = findTemplateElement(t.getRootTreeNode(), breakpoint.getLine());
        if(te == null)
        {
            return;
        }
        TemplateElement parent = (TemplateElement)te.getParent();
        DebugBreak db = new DebugBreak(te);
        // TODO: Ensure there always is a parent by making sure
        // that the root element in the template is always a MixedContent
        // Also make sure it doesn't conflict with anyone's code.
        parent.setChildAt(parent.getIndex(te), db);
    }

    private static TemplateElement findTemplateElement(TemplateElement te, int line)
    {
        if(te.getBeginLine() > line || te.getEndLine() < line)
        {
            return null;
        }
        // Find the narrowest match
        List childMatches = new ArrayList();
        for(Enumeration children = te.children(); children.hasMoreElements();)
        {
            TemplateElement child = (TemplateElement)children.nextElement();
            TemplateElement childmatch = findTemplateElement(child, line);
            if(childmatch != null)
            {
                childMatches.add(childmatch);
            }
        }
        //find a match that exactly matches the begin/end line
        TemplateElement bestMatch = null;
        for(int i = 0; i < childMatches.size(); i++)
        {
            TemplateElement e = (TemplateElement) childMatches.get(i);

            if( bestMatch == null )
            {
                bestMatch = e;
            }

            if( e.getBeginLine() == line && e.getEndLine() > line )
            {
                bestMatch = e;
            }

            if( e.getBeginLine() == e.getEndLine() && e.getBeginLine() == line)
            {
                bestMatch = e;
                break;
            }
        }
        if( bestMatch != null)
        {
           return bestMatch;
        }
        // If no child provides narrower match, return this
        return te;
    }
    
    private TemplateDebugInfo findTemplateDebugInfo(String templateName)
    {
        processRefQueue();
        return (TemplateDebugInfo)templateDebugInfos.get(templateName); 
    }
    
    private TemplateDebugInfo createTemplateDebugInfo(String templateName)
    {
        TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
        if(tdi == null)
        {
            tdi = new TemplateDebugInfo();
            templateDebugInfos.put(templateName, tdi);
        }
        return tdi;
    }
    
    void removeBreakpoint(Breakpoint breakpoint)
    {
        String templateName = breakpoint.getTemplateName();
        synchronized(templateDebugInfos)
        {
            TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
            if(tdi != null)
            {
                List breakpoints = tdi.breakpoints;
                int pos = Collections.binarySearch(breakpoints, breakpoint);
                if(pos >= 0)
                { 
                    breakpoints.remove(pos);
                    for (Iterator iter = tdi.templates.iterator(); iter.hasNext();)
                    {
                        TemplateReference ref = (TemplateReference) iter.next();
                        Template t = ref.getTemplate();
                        if(t == null)
                        {
                            iter.remove();
                        }
                        else
                        {
                            removeDebugBreak(t, breakpoint);
                        }
                    }
                }
                if(tdi.isEmpty())
                {
                    templateDebugInfos.remove(templateName);
                }
            }
        }
    }

    private void removeDebugBreak(Template t, Breakpoint breakpoint)
    {
        TemplateElement te = findTemplateElement(t.getRootTreeNode(), breakpoint.getLine());
        if(te == null)
        {
            return;
        }
        DebugBreak db = null;
        while(te != null)
        {
            if(te instanceof DebugBreak)
            {
                db = (DebugBreak)te;
                break;
            }
            te = (TemplateElement)te.getParent();
        }
        if(db == null)
        {
            return;
        }
        TemplateElement parent = (TemplateElement)db.getParent(); 
        parent.setChildAt(parent.getIndex(db), (TemplateElement)db.getChildAt(0));
    }
    
    void removeBreakpoints(String templateName)
    {
        synchronized(templateDebugInfos)
        {
            TemplateDebugInfo tdi = findTemplateDebugInfo(templateName);
            if(tdi != null)
            {
                removeBreakpoints(tdi);
                if(tdi.isEmpty())
                {
                    templateDebugInfos.remove(templateName);
                }
            }
        }
    }

    void removeBreakpoints()
    {
        synchronized(templateDebugInfos)
        {
            for (Iterator iter = templateDebugInfos.values().iterator(); iter.hasNext();)
            {
                TemplateDebugInfo tdi = (TemplateDebugInfo) iter.next(); 
                removeBreakpoints(tdi);
                if(tdi.isEmpty())
                {
                    iter.remove();
                }
            }
        }
    }

    private void removeBreakpoints(TemplateDebugInfo tdi)
    {
        tdi.breakpoints.clear();
        for (Iterator iter = tdi.templates.iterator(); iter.hasNext();)
        {
            TemplateReference ref = (TemplateReference) iter.next();
            Template t = ref.getTemplate();
            if(t == null)
            {
                iter.remove();
            }
            else
            {
                removeDebugBreaks(t.getRootTreeNode());
            }
        }
    }
    
    private void removeDebugBreaks(TemplateElement te)
    {
        int count = te.getChildCount();
        for(int i = 0; i < count; ++i)
        {
            TemplateElement child = (TemplateElement)te.getChildAt(i);
            while(child instanceof DebugBreak)
            {
                TemplateElement dbchild = (TemplateElement)child.getChildAt(0); 
                te.setChildAt(i, dbchild);
                child = dbchild;
            }
            removeDebugBreaks(child);
        }
    }
    
    private static final class TemplateDebugInfo
    {
        final List templates = new ArrayList();
        final List breakpoints = new ArrayList();
        
        boolean isEmpty()
        {
            return templates.isEmpty() && breakpoints.isEmpty();
        }
    }
    
    private static final class TemplateReference extends WeakReference
    {
        final String templateName;
         
        TemplateReference(String templateName, Template template, ReferenceQueue queue)
        {
            super(template, queue);
            this.templateName = templateName;
        }
        
        Template getTemplate()
        {
            return (Template)get();
        }
    }
    
    private void processRefQueue()
    {
        for(;;)
        {
            TemplateReference ref = (TemplateReference)refQueue.poll();
            if(ref == null)
            {
                break;
            }
            TemplateDebugInfo tdi = findTemplateDebugInfo(ref.templateName);
            if(tdi != null)
            {
                tdi.templates.remove(ref);
                if(tdi.isEmpty())
                {
                    templateDebugInfos.remove(ref.templateName);
                }
            }
        }
    }

    void shutdownSpi()
    {
        server.stop();
        try
        {
            UnicastRemoteObject.unexportObject(this.debugger, true);
        }
        catch(Exception e)
        {
        }

        RmiDebuggedEnvironmentImpl.cleanup();
    }
}
