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

import freemarker.template.TemplateModel;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * 
 * <p>Implemented by {@link TemplateModel}-s that can explain why they don't implement a certain type. 
 * */
public interface _UnexpectedTypeErrorExplainerTemplateModel extends TemplateModel {

    /**
     * @return A single {@link _ErrorDescriptionBuilder} tip, or {@code null}.
     */
    Object[] explainTypeError(Class[]/*<? extends TemplateModel>*/ expectedClasses);
    
}
