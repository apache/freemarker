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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import freemarker.debug.Debugger;
import freemarker.log.Logger;
import freemarker.template.utility.SecurityUtilities;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 */
class DebuggerServer
{
    private static final Logger LOG = Logger.getLogger("freemarker.debug.server");
    // TODO: Eventually replace with Yarrow    
    private static final Random R = new SecureRandom();
    
    private final byte[] password;
    private final int port;
    private final Serializable debuggerStub;
    private boolean stop = false;
    private ServerSocket serverSocket;
    
    public DebuggerServer(Serializable debuggerStub)
    {
        port = SecurityUtilities.getSystemProperty("freemarker.debug.port", Debugger.DEFAULT_PORT).intValue();
        try
        {
            password = SecurityUtilities.getSystemProperty("freemarker.debug.password", "").getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new UndeclaredThrowableException(e);
        }
        this.debuggerStub = debuggerStub;
    }
    
    public void start()
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                startInternal();
            }
        }, "FreeMarker Debugger Server Acceptor").start();
    }
    
    private void startInternal()
    {
        try
        {
            serverSocket = new ServerSocket(port);
            while(!stop)
            {
                Socket s = serverSocket.accept();
                new Thread(new DebuggerAuthProtocol(s)).start();
            }
        }
        catch(IOException e)
        {
            LOG.error("Debugger server shut down.", e);
        }
    }
    
    private class DebuggerAuthProtocol implements Runnable
    {
        private final Socket s;
        
        DebuggerAuthProtocol(Socket s)
        {
            this.s = s;
        }
        
        public void run()
        {
            try
            {
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                byte[] challenge = new byte[512];
                R.nextBytes(challenge);
                out.writeInt(220); // protocol version
                out.writeObject(challenge);
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(password);
                md.update(challenge);
                byte[] response = (byte[])in.readObject();
                if(Arrays.equals(response, md.digest()))
                {
                    out.writeObject(debuggerStub);
                }
                else
                {
                    out.writeObject(null);
                }
            }
            catch(Exception e)
            {
                LOG.warn("Connection to " + s.getInetAddress().getHostAddress() + " abruply broke", e);
            }
        }

    }

    public void stop()
    {
        this.stop = true;
        if(serverSocket != null)
        {
            try
            {
                serverSocket.close();
            }
            catch(IOException e)
            {
                LOG.error("Unable to close server socket.", e);
            }
        }
    }
}
