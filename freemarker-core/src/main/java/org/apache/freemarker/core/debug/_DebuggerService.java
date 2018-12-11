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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.util._SecurityUtils;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * This class provides debugging hooks for the core FreeMarker engine. It is
 * not usable for anyone outside the FreeMarker core classes.
 */
public abstract class _DebuggerService {
    private static final _DebuggerService INSTANCE = createInstance();
    
    private static _DebuggerService createInstance() {
        // Creates the appropriate service class. If the debugging is turned
        // off, this is a fast no-op service, otherwise it's the real-thing
        // RMI service.
        return 
            _SecurityUtils.getSystemProperty("org.apache.freemarker.core.debug.password", null) == null
            ? new NoOpDebuggerService()
            : new RmiDebuggerService();
    }

    public static List getBreakpoints(String templateName) {
        return INSTANCE.getBreakpointsSpi(templateName);
    }
    
    abstract List getBreakpointsSpi(String templateName);

    public static void registerTemplate(Template template) {
        INSTANCE.registerTemplateSpi(template);
    }
    
    abstract void registerTemplateSpi(Template template);
    
    public static boolean suspendEnvironment(Environment env, String templateName, int line)
    throws RemoteException {
        return INSTANCE.suspendEnvironmentSpi(env, templateName, line);
    }
    
    abstract boolean suspendEnvironmentSpi(Environment env, String templateName, int line)
    throws RemoteException;

    abstract void shutdownSpi();

    public static void shutdown() {
        INSTANCE.shutdownSpi();
    }

    private static class NoOpDebuggerService extends _DebuggerService {
        @Override
        List getBreakpointsSpi(String templateName) {
            return Collections.EMPTY_LIST;
        }
        
        @Override
        boolean suspendEnvironmentSpi(Environment env, String templateName, int line) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        void registerTemplateSpi(Template template) {
        }

        @Override
        void shutdownSpi() {
        }
    }
}