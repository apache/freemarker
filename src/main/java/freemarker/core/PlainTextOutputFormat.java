/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package freemarker.core;

/**
 * Represents the plain text output format. Plain text is text without any characters with special meaning. As such, it
 * has no escaping.
 * 
 * <p>
 * String literals in FTL expressions use this output format, which has importance when <code>${...}</code> is used
 * inside them.
 * 
 * <p>
 * The main difference from {@link UndefinedOutputFormat} is that this format doesn't allow inserting values of another
 * output formats into itself (unless they can be converted to plain text), while {@link UndefinedOutputFormat} would
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
