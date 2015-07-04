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

package freemarker.template;

import java.io.Serializable;

/**
 * A simple implementation of the <tt>TemplateScalarModel</tt>
 * interface, using a <tt>String</tt>.
 * As of version 2.0 this object is immutable.
 *
 * <p>This class is thread-safe.
 *
 * @see SimpleSequence
 * @see SimpleHash
 */
public final class SimpleScalar 
implements TemplateScalarModel, Serializable {
    
    /**
     * @serial the value of this <tt>SimpleScalar</tt> if it wraps a
     * <tt>String</tt>.
     */
    private final String value;

    /**
     * Constructs a <tt>SimpleScalar</tt> containing a string value.
     * @param value the string value. If this is {@code null}, its value in FTL will be {@code ""}.
     */
    public SimpleScalar(String value) {
        this.value = value;
    }

    public String getAsString() {
        return (value == null) ? "" : value;
    }

    public String toString() {
        // [2.4] Shouldn't return null
        return value;
    }
    
    /**
     * Same as calling the constructor, except that for a {@code null} parameter it returns null. 
     * 
     * @since 2.3.23
     */
    public static SimpleScalar newInstanceOrNull(String s) {
        return s != null ? new SimpleScalar(s) : null;
    }
    
}
