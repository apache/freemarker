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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;

import freemarker.debug.impl.RmiDebuggerListenerImpl;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * A utility class that allows you to connect to the FreeMarker debugger service
 * running on a specific host and port. 
 * @author Attila Szegedi
 */
public class DebuggerClient
{
    private DebuggerClient()
    {
    }
    
    /**
     * Connects to the FreeMarker debugger service running on a specific host
     * and port. The Java VM to which the connection is made must have defined
     * the system property <tt>freemarker.debug.password</tt> in order to enable
     * the debugger service. Additionally, the <tt>freemarker.debug.port</tt>
     * system property can be set to specify the port where the debugger service
     * is listening. When not specified, it defaults to 
     * {@link Debugger#DEFAULT_PORT}.
     * @param host the host address of the machine where the debugger service is
     * running.
     * @param port the port of the debugger service
     * @param password the password required to connect to the debugger service
     * @return Debugger a debugger object. null is returned in case incorrect
     * password was supplied.
     * @throws IOException if an exception occurs.
     */
    public static Debugger getDebugger(InetAddress host, int port, String password)
    throws
        IOException
    {
        try
        {
            Socket s = new Socket(host, port);
            try
            {
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                int protocolVersion = in.readInt();
                if(protocolVersion > 220)
                {
                    throw new IOException(
                        "Incompatible protocol version " + protocolVersion + 
                        ". At most 220 was expected.");
                }
                byte[] challenge = (byte[])in.readObject();
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(password.getBytes("UTF-8"));
                md.update(challenge);
                out.writeObject(md.digest());
                return new LocalDebuggerProxy((Debugger)in.readObject());
                //return (Debugger)in.readObject();
            }
            finally
            {
                s.close();
            }
        }
        catch(IOException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new UndeclaredThrowableException(e); 
        }
    }
    
    private static class LocalDebuggerProxy implements Debugger
    {
        private final Debugger remoteDebugger;

        LocalDebuggerProxy(Debugger remoteDebugger)
        {
            this.remoteDebugger = remoteDebugger;
        }

        public void addBreakpoint(Breakpoint breakpoint) throws RemoteException
        {
            remoteDebugger.addBreakpoint(breakpoint);
        }

        public Object addDebuggerListener(DebuggerListener listener) 
        throws RemoteException
        {
            if(listener instanceof RemoteObject)
            {
                return remoteDebugger.addDebuggerListener(listener);
            }
            else
            {
                RmiDebuggerListenerImpl remotableListener = 
                    new RmiDebuggerListenerImpl(listener);
                return remoteDebugger.addDebuggerListener(remotableListener);
            }
        }

        public List getBreakpoints() throws RemoteException
        {
            return remoteDebugger.getBreakpoints();
        }

        public List getBreakpoints(String templateName) throws RemoteException
        {
            return remoteDebugger.getBreakpoints(templateName);
        }

        public Collection getSuspendedEnvironments() throws RemoteException
        {
            return remoteDebugger.getSuspendedEnvironments();
        }

        public void removeBreakpoint(Breakpoint breakpoint) throws RemoteException
        {
            remoteDebugger.removeBreakpoint(breakpoint);
        }

        public void removeBreakpoints(String templateName) throws RemoteException
        {
            remoteDebugger.removeBreakpoints(templateName);
        }

        public void removeBreakpoints() throws RemoteException
        {
            remoteDebugger.removeBreakpoints();
        }

        public void removeDebuggerListener(Object id) throws RemoteException
        {
            remoteDebugger.removeDebuggerListener(id);
        }
    }
}
