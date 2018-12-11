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

package org.apache.freemarker.core.debug;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import org.apache.freemarker.core.util.UndeclaredThrowableException;
import org.apache.freemarker.core.util._SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
class DebuggerServer {

    private static final Logger LOG = LoggerFactory.getLogger(DebuggerServer.class);
    
    // TODO: Eventually replace with Yarrow    
    // TODO: Can be extremely slow (on Linux, not enough entropy)
    private static final Random R = new SecureRandom();
    
    private final byte[] password;
    private final int port;
    private final Serializable debuggerStub;
    private boolean stop = false;
    private ServerSocket serverSocket;
    
    public DebuggerServer(Serializable debuggerStub) {
        port = _SecurityUtils.getSystemProperty("org.apache.freemarker.core.debug.port", Debugger.DEFAULT_PORT).intValue();
        try {
            password = _SecurityUtils.getSystemProperty("org.apache.freemarker.core.debug.password", "").getBytes(
                    StandardCharsets.UTF_8);
        } catch (UnsupportedCharsetException e) {
            throw new UndeclaredThrowableException(e);
        }
        this.debuggerStub = debuggerStub;
    }
    
    public void start() {
        new Thread(new Runnable()
        {
            @Override
            public void run() {
                startInternal();
            }
        }, "FreeMarker Debugger Server Acceptor").start();
    }
    
    private void startInternal() {
        try {
            serverSocket = new ServerSocket(port);
            while (!stop) {
                Socket s = serverSocket.accept();
                new Thread(new DebuggerAuthProtocol(s)).start();
            }
        } catch (IOException e) {
            LOG.error("Debugger server shut down.", e);
        }
    }
    
    private class DebuggerAuthProtocol implements Runnable {
        private final Socket s;
        
        DebuggerAuthProtocol(Socket s) {
            this.s = s;
        }
        
        @Override
        public void run() {
            try {
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                byte[] challenge = new byte[512];
                R.nextBytes(challenge);
                out.writeInt(220); // protocol version
                out.writeObject(challenge);
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(password);
                md.update(challenge);
                byte[] response = (byte[]) in.readObject();
                if (Arrays.equals(response, md.digest())) {
                    out.writeObject(debuggerStub);
                } else {
                    out.writeObject(null);
                }
            } catch (Exception e) {
                LOG.warn("Connection to {} abruptly broke", s.getInetAddress().getHostAddress(), e);
            }
        }

    }

    public void stop() {
        stop = true;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.error("Unable to close server socket.", e);
            }
        }
    }
}
