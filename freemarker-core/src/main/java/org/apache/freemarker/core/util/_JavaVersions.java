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

/**
 * Used internally only, might change without notice!
 */
public final class _JavaVersions {

    private _JavaVersions() {
        // Not meant to be instantiated
    }

    private static final boolean IS_AT_LEAST_21 = Runtime.version().feature() >= 21;

    /**
     * {@code null} if Java 8 is not available, otherwise the object through with the Java 8 operations are available.
     */
    static public final _Java21 JAVA_21;
    static {
        if (IS_AT_LEAST_21) {
            try {
                JAVA_21 = (_Java21) Class.forName("freemarker.core._Java21Impl").getField("INSTANCE").get(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create _Java21Impl", e);
            }
        } else {
            JAVA_21 = null;
        }
    }

}
