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

import java.util.LinkedHashMap;
import java.util.Map;

final class StandardCFormats {
    private StandardCFormats() {
    }

    static final Map<String, CFormat> STANDARD_C_FORMATS;
    static {
            STANDARD_C_FORMATS = new LinkedHashMap<>();
            STANDARD_C_FORMATS.put(JSONCFormat.INSTANCE.getName(), JSONCFormat.INSTANCE);
            STANDARD_C_FORMATS.put(JavaScriptCFormat.INSTANCE.getName(), JavaScriptCFormat.INSTANCE);
            STANDARD_C_FORMATS.put(JavaCFormat.INSTANCE.getName(), JavaCFormat.INSTANCE);
            STANDARD_C_FORMATS.put(XSCFormat.INSTANCE.getName(), XSCFormat.INSTANCE);
            STANDARD_C_FORMATS.put(Default230CFormat.INSTANCE.getName(), Default230CFormat.INSTANCE);
            STANDARD_C_FORMATS.put(Default2321CFormat.INSTANCE.getName(), Default2321CFormat.INSTANCE);
    }

}
