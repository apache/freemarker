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
package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.Configuration;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
// [FM3] This was added temporary until we get to cleaning up the parts that depend on a static ObjectWrapper. The
// ObjectWrapper should always come from the Configuration, not from the statics here.
public final class _StaticObjectWrappers {
    
    private _StaticObjectWrappers() {
        //
    }

    public static final DefaultObjectWrapper DEFAULT_OBJECT_WRAPPER
            = new DefaultObjectWrapperBuilder(Configuration.VERSION_3_0_0).build();

    public static final SimpleObjectWrapper SIMPLE_OBJECT_WRAPPER
            = new SimpleObjectWrapper(Configuration.VERSION_3_0_0);
    {
        SIMPLE_OBJECT_WRAPPER.writeProtect();
    }    
}
