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

package org.apache.freemarker.core;

import java.util.Properties;

import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * The runtime environment used during the evaluation of configuration {@link Properties}.
 */
public class _SettingEvaluationEnvironment {
    
    private static final ThreadLocal CURRENT = new ThreadLocal();

    private DefaultObjectWrapper objectWrapper;
    
    public static _SettingEvaluationEnvironment getCurrent() {
        Object r = CURRENT.get();
        if (r != null) {
            return (_SettingEvaluationEnvironment) r;
        }
        return new _SettingEvaluationEnvironment();
    }
    
    public static _SettingEvaluationEnvironment startScope() {
        Object previous = CURRENT.get();
        CURRENT.set(new _SettingEvaluationEnvironment());
        return (_SettingEvaluationEnvironment) previous;
    }
    
    public static void endScope(_SettingEvaluationEnvironment previous) {
        CURRENT.set(previous);
    }

    public DefaultObjectWrapper getObjectWrapper() {
        if (objectWrapper == null) {
            objectWrapper = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        }
        return objectWrapper;
    }
    
}
