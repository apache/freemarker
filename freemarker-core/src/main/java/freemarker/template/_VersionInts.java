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

package freemarker.template;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't.
 */
// Because we refer to other classes in the static initializer of this class, be careful with referring this class in
// the static initializers of other classes, as that can lead to deadlock if the class initialization locks are acquired
// by the JVM in different orders! This is also why this was extracted from _TemplateAPI.
public final class _VersionInts {
    private _VersionInts() {
    }

    // Constants for faster access... probably unnecessary and should be removed.
    public static final int V_2_3_0 = Configuration.VERSION_2_3_0.intValue();
    public static final int V_2_3_19 = Configuration.VERSION_2_3_19.intValue();
    public static final int V_2_3_20 = Configuration.VERSION_2_3_20.intValue();
    public static final int V_2_3_21 = Configuration.VERSION_2_3_21.intValue();
    public static final int V_2_3_22 = Configuration.VERSION_2_3_22.intValue();
    public static final int V_2_3_23 = Configuration.VERSION_2_3_23.intValue();
    public static final int V_2_3_24 = Configuration.VERSION_2_3_24.intValue();
    public static final int V_2_3_25 = Configuration.VERSION_2_3_25.intValue();
    public static final int V_2_3_26 = Configuration.VERSION_2_3_26.intValue();
    public static final int V_2_3_27 = Configuration.VERSION_2_3_27.intValue();
    public static final int V_2_3_28 = Configuration.VERSION_2_3_28.intValue();
    public static final int V_2_3_30 = Configuration.VERSION_2_3_30.intValue();
    public static final int V_2_3_31 = Configuration.VERSION_2_3_31.intValue();
    public static final int V_2_3_32 = Configuration.VERSION_2_3_32.intValue();
    public static final int V_2_4_0 = Version.intValueFor(2, 4, 0);
}
