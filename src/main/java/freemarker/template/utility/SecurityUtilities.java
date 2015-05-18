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

package freemarker.template.utility;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import freemarker.log.Logger;

/**
 */
public class SecurityUtilities
{
    private static final Logger LOG = Logger.getLogger("freemarker.security");
    private SecurityUtilities()
    {
    }
    
    public static String getSystemProperty(final String key)
    {
        return (String) AccessController.doPrivileged(
            new PrivilegedAction()
            {
                public Object run()
                {
                    return System.getProperty(key);
                }
            });
    }

    public static String getSystemProperty(final String key, final String defValue)
    {
        try
        {
            return (String) AccessController.doPrivileged(
                new PrivilegedAction()
                {
                    public Object run()
                    {
                        return System.getProperty(key, defValue);
                    }
                });
        }
        catch(AccessControlException e)
        {
            LOG.warn("Insufficient permissions to read system property " + 
                    StringUtil.jQuoteNoXSS(key) + ", using default value " +
                    StringUtil.jQuoteNoXSS(defValue));
            return defValue;
        }
    }

    public static Integer getSystemProperty(final String key, final int defValue)
    {
        try
        {
            return (Integer) AccessController.doPrivileged(
                new PrivilegedAction()
                {
                    public Object run()
                    {
                        return Integer.getInteger(key, defValue);
                    }
                });
        }
        catch(AccessControlException e)
        {
            LOG.warn("Insufficient permissions to read system property " + 
                    StringUtil.jQuote(key) + ", using default value " + defValue);
            return new Integer(defValue);
        }
    }
}
