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
package org.apache.freemarker.core.ast;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Used internally only, might changes without notice!
 * Used for accessing functionality that's only present in Java 6 or later.
 */
public final class _Java6Impl implements _Java6 {
    
    public static final _Java6 INSTANCE = new _Java6Impl();

    private _Java6Impl() {
        // Not meant to be instantiated
    }

    public void setRoundingMode(DecimalFormat df, RoundingMode roundingMode) {
        df.setRoundingMode(roundingMode);
    }

    public void setExponentSeparator(DecimalFormatSymbols dfs, String exponentSeparator) {
        dfs.setExponentSeparator(exponentSeparator);
    }
    
}
