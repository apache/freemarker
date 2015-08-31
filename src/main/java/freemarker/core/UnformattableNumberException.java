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

import freemarker.template.TemplateNumberModel;

/**
 * Thrown when a {@link TemplateNumberModel} can't be formatted because of the value/properties of the
 * {@link TemplateNumberModel}. For example, some formatters might can't format NaN, or can't display numbers above
 * certain magnitude.
 * 
 * @since 2.3.24
 */
public class UnformattableNumberException extends Exception {

    public UnformattableNumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnformattableNumberException(String message) {
        super(message);
    }

}
