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

import freemarker.template.TemplateException;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _DelayedGetMessageWithoutStackTop extends _DelayedConversionToString  {

    public _DelayedGetMessageWithoutStackTop(TemplateException exception) {
        super(exception);
    }

    protected String doConversion(Object obj) {
        return ((TemplateException) obj).getMessageWithoutStackTop();
    }
    
}
