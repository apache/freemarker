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

import freemarker.template.Configuration;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * Represents an output format. If you need auto-escaping, see its subclass, {@link MarkupOutputFormat}. 
 * 
 * @see Configuration#setOutputFormat(OutputFormat)
 * @see Configuration#setRegisteredCustomOutputFormats(java.util.Collection)
 * @see MarkupOutputFormat
 * 
 * @since 2.3.24
 */
public abstract class OutputFormat {

    /**
     * The short name used to refer to this format (like in the {@code #ftl} header).
     */
    public abstract String getName();
    
    /**
     * Returns the MIME type of the output format. This might comes handy when generating a HTTP response. {@code null}
     * {@code null} if this output format doesn't clearly corresponds to a specific MIME type.
     */
    public abstract String getMimeType();

    /**
     * Tells if this output format allows inserting {@link TemplateMarkupOutputModel}-s of another output formats into
     * it.
     *
     * <p>If {@code true}, the foreign {@link TemplateMarkupOutputModel} will be inserted into the output. If the current
     * output format is a {@link MarkupOutputFormat} this is done using the
     * {@link MarkupOutputFormat#outputForeign(TemplateMarkupOutputModel, Writer)} method, which can implement smart
     * conversions. The default behavior (and the only behavior for non-markup outputs) is to behave as if the surrounding
     * output format was the same; this is usually a bad idea to allow, as such an event could
     * indicate application bugs.
     *
     * <p>If this method returns {@code false} (recommended), then FreeMarker will try to assimilate the inserted value by
     * converting its format to this format, which will currently (2.3.24) cause exception, unless the inserted value is
     * made by escaping plain text and the target format is non-escaping, in which case format conversion is trivially
     * possible. (It's not impossible that conversions will be extended beyond this, if there will be demand for that.)
     * 
     * <p>
     * {@code true} value is used by {@link UndefinedOutputFormat}.
     */
    public abstract boolean isOutputFormatMixingAllowed();

    /**
     * Returns the short description of this format, to be used in error messages.
     * Override {@link #toStringExtraProperties()} to customize this.
     */
    @Override
    public final String toString() {
        String extras = toStringExtraProperties();
        return getName() + "("
                + "mimeType=" + StringUtil.jQuote(getMimeType()) + ", "
                + "class=" + ClassUtil.getShortClassNameOfObject(this, true)
                + (extras.length() != 0 ? ", " : "") + extras
                + ")";
    }
    
    /**
     * Should be like {@code "foo=\"something\", bar=123"}; this will be inserted inside the parentheses in
     * {@link #toString()}. Shouldn't return {@code null}; should return {@code ""} if there are no extra properties.  
     */
    protected String toStringExtraProperties() {
        return "";
    }

}
