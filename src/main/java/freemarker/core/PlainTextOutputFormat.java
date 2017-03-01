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

/**
 * Represents the plain text output format (MIME type "text/plain", name "plainText"). This format doesn't support
 * escaping. This format doesn't allow mixing in template output values of other output formats.
 * 
 * <p>
 * The main difference from {@link UndefinedOutputFormat} is that this format doesn't allow inserting values of another
 * output format into itself (unless they can be converted to plain text), while {@link UndefinedOutputFormat} would
 * just insert the foreign "markup" as is. Also, this format has {"text/plain"} MIME type, while
 * {@link UndefinedOutputFormat} has {@code null}.
 * 
 * @since 2.3.24
 */
public final class PlainTextOutputFormat extends OutputFormat {

    public static final PlainTextOutputFormat INSTANCE = new PlainTextOutputFormat();
    
    private PlainTextOutputFormat() {
        // Only to decrease visibility
    }

    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }

    @Override
    public String getName() {
        return "plainText";
    }

    @Override
    public String getMimeType() {
        return "text/plain";
    }

}
