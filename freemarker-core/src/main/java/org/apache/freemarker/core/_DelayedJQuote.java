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

import org.apache.freemarker.core.util._StringUtils;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedJQuote extends _DelayedConversionToString {

    public _DelayedJQuote(Object object) {
        super(object);
    }

    @Override
    protected String doConversion(Object obj) {
        return _StringUtils.jQuote(_ErrorDescriptionBuilder.toString(obj));
    }

}
