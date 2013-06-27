/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

/**
 * The main debugger interface. Allows management of breakpoints as well as
 * installation of listeners for debug events.
 * @author Attila Szegedi
 */
public interface Debugger extends Remote
{
    public static final int DEFAULT_PORT = 7011;

    /**
     * Adds a breakpoint
     * @param breakpoint the breakpoint to add
     * @throws RemoteException
     */
    public void addBreakpoint(Breakpoint breakpoint)
    throws
        RemoteException;
    
    /**
     * Removes a single breakpoint
     * @param breakpoint the breakpoint to remove
     * @throws RemoteException
     */
    public void removeBreakpoint(Breakpoint breakpoint)
    throws
        RemoteException;

    /**
     * Removes all breakpoints for a specific template
     * @param templateName
     * @throws RemoteException
     */
    public void removeBreakpoints(String templateName)
    throws
        RemoteException;

    /**
     * Removes all breakpoints
     * @throws RemoteException
     */
    public void removeBreakpoints()
    throws
        RemoteException;

    /**
     * Retrieves a list of all {@link Breakpoint} objects.
     * @throws RemoteException
     */
    public List getBreakpoints()
    throws
        RemoteException;
        
    /**
     * Retrieves a list of all {@link Breakpoint} objects for the specified
     * template.
     * @throws RemoteException
     */
    public List getBreakpoints(String templateName)
    throws
        RemoteException;

    /**
     * Retrieves a collection of all {@link DebuggedEnvironment} objects that 
     * are currently suspended.
     * @throws RemoteException
     */
    public Collection getSuspendedEnvironments()
    throws
        RemoteException;
        
    /**
     * Adds a listener for debugger events.
     * @return an identification token that should be passed to 
     * {@link #removeDebuggerListener(Object)} to remove this listener.
     * @throws RemoteException
     */
    public Object addDebuggerListener(DebuggerListener listener)
    throws
        RemoteException;
        
    /**
     * Removes a previously added debugger listener.
     * @param id the identification token for the listener that was returned
     * from a prior call to {@link #addDebuggerListener(DebuggerListener)}.
     * @throws RemoteException
     */
    public void removeDebuggerListener(Object id)
    throws
        RemoteException;
}
