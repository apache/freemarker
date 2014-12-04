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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.utility.SecurityUtilities;

/**
 * This class provides debugging hooks for the core FreeMarker engine. It is
 * not usable for anyone outside the FreeMarker core classes. It is public only
 * as an implementation detail.
 */
public abstract class DebuggerService
{
    private static final DebuggerService instance = createInstance();
    
    private static DebuggerService createInstance()
    {
        // Creates the appropriate service class. If the debugging is turned
        // off, this is a fast no-op service, otherwise it's the real-thing
        // RMI service.
        return 
            SecurityUtilities.getSystemProperty("freemarker.debug.password", null) == null
            ? (DebuggerService)new NoOpDebuggerService()
            : (DebuggerService)new RmiDebuggerService();
    }

    public static List getBreakpoints(String templateName)
    {
        return instance.getBreakpointsSpi(templateName);
    }
    
    abstract List getBreakpointsSpi(String templateName);

    public static void registerTemplate(Template template)
    {
        instance.registerTemplateSpi(template);
    }
    
    abstract void registerTemplateSpi(Template template);
    
    public static boolean suspendEnvironment(Environment env, String templateName, int line)
    throws
        RemoteException
    {
        return instance.suspendEnvironmentSpi(env, templateName, line);
    }
    
    abstract boolean suspendEnvironmentSpi(Environment env, String templateName, int line)
    throws
        RemoteException;

    abstract void shutdownSpi();

    public static void shutdown()
    {
        instance.shutdownSpi();
    }

    private static class NoOpDebuggerService extends DebuggerService
    {
        List getBreakpointsSpi(String templateName)
        {
            return Collections.EMPTY_LIST;
        }
        
        boolean suspendEnvironmentSpi(Environment env, String templateName, int line)
        {
            throw new UnsupportedOperationException();
        }
        
        void registerTemplateSpi(Template template)
        {
        }

        void shutdownSpi()
        {
        }
    }
}