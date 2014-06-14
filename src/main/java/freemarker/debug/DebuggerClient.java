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
