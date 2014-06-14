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

/**
 * "string" template language data-type; like in Java, an unmodifiable UNICODE character sequence.
 * (The name of this interface should be {@code TemplateStringModel}. The misnomer is inherited from the
 * old times, when this was the only single-value type in FreeMarker.)
 */
public interface TemplateScalarModel extends TemplateModel {

    /**
     * A constant value to use as the empty string.
     */
    public TemplateModel EMPTY_STRING = new SimpleScalar("");

    /**
     * Returns the string representation of this model. Don't return {@code null}, as that will cause exception.
     * (In classic-compatible mode the engine will convert {@code null} into empty string, though.)
     */
    public String getAsString() throws TemplateModelException;

}
