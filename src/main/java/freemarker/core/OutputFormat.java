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

import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * Encapsulates the {@link TemplateMarkupOutputModel} factories and {@code TemplateOutputModel} operations, and other meta
 * information (like MIME type) about a certain output format.
 * 
 * @since 2.3.24
 */
public abstract class OutputFormat {

    /**
     * Tells if this output format allows inserting {@link TemplateMarkupOutputModel}-s of another output formats into it. If
     * {@code true}, the foreign {@link TemplateMarkupOutputModel} will be inserted into the output as is (like if the
     * surrounding output format was the same). This is usually a bad idea, as such an even could indicate application
     * bugs. If this method returns {@code false} (recommended), then FreeMarker will try to assimilate the inserted
     * value by converting its format to this format, which will currently (2.3.24) cause exception, unless the inserted
     * value is made by escaping plain text and the target format is not escaping, in which case format conversion is
     * trivially possible. (It's not impossible to extending conversions beyond this, if there will be real world demand
     * for it.)
     * 
     * <p>{@code true} value is used by {@link UndefinedOutputFormat}.
     */
    public abstract boolean isOutputFormatMixingAllowed();
    
    /**
     * The short name we used to refer to this format (like in the {@code #ftl} header).
     */
    public abstract String getName();
    
    /**
     * Returns the MIME type of the output format. This might comes handy when generating generating a HTTP response.
     * {@code null} if the output format doesn't clearly corresponds to a specific MIME type.
     */
    public abstract String getMimeType();

    @Override
    public final String toString() {
        return getName() + "("
                + "mimeType=" + StringUtil.jQuote(getMimeType()) + ", "
                + "class=" + ClassUtil.getShortClassNameOfObject(this, true)
                + ")";
    }

}
