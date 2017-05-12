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

package org.apache.freemarker.core.util;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.freemarker.core._CoreLogs;
import org.slf4j.Logger;

/**
 */
public class _SecurityUtil {
    
    private static final Logger LOG = _CoreLogs.SECURITY;
    
    private _SecurityUtil() {
    }
    
    public static String getSystemProperty(final String key) {
        return (String) AccessController.doPrivileged(
            new PrivilegedAction()
            {
                @Override
                public Object run() {
                    return System.getProperty(key);
                }
            });
    }

    public static String getSystemProperty(final String key, final String defValue) {
        try {
            return (String) AccessController.doPrivileged(
                new PrivilegedAction()
                {
                    @Override
                    public Object run() {
                        return System.getProperty(key, defValue);
                    }
                });
        } catch (AccessControlException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Insufficient permissions to read system property " + 
                        _StringUtil.jQuoteNoXSS(key) + ", using default value " +
                        _StringUtil.jQuoteNoXSS(defValue));
            }
            return defValue;
        }
    }

    public static Integer getSystemProperty(final String key, final int defValue) {
        try {
            return (Integer) AccessController.doPrivileged(
                new PrivilegedAction()
                {
                    @Override
                    public Object run() {
                        return Integer.getInteger(key, defValue);
                    }
                });
        } catch (AccessControlException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Insufficient permissions to read system property " + 
                        _StringUtil.jQuote(key) + ", using default value " + defValue);
            }
            return Integer.valueOf(defValue);
        }
    }
}
