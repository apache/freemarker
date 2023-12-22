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

package freemarker.core;

import java.lang.reflect.Method;

/**
 * Used internally only, might change without notice!
 */
public final class _JavaVersion {
    private _JavaVersion() {
        throw new AssertionError();
    }

    /**
     * Like 8 for Java 8, 9 for Java 9, etc. This is  at least 8, since we don't support earlier Java versions anymore.
     */
    public static final int FEATURE;

    static {
        Method versionMethod;
        try {
            versionMethod = Runtime.class.getMethod("version");
        } catch (NoSuchMethodException e) {
            // Runtime.version() was added in Java 9
            versionMethod = null;
        }

        if (versionMethod == null) {
            FEATURE = 8;
        } else {
            try {
                Object version = versionMethod.invoke(null);
                // major() was deprecated by feature() added in Java 10, but they do the same.
                FEATURE = (int) version.getClass().getMethod("major").invoke(version);
            } catch (Throwable e) {
                throw new IllegalStateException("Couldn't get Java version", e);
            }
        }
    }
}
