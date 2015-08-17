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

import freemarker.template.Configuration;
import freemarker.template.TemplateModel;

/**
 * "markup output" template language data-type; stores markup (some kind of "rich text" / structured format), as opposed
 * to plain text that meant to be printed as template output. Each implementation of this type has a
 * {@link OutputFormat} subclass pair (like {@link TemplateHTMLModel} has {@link HTMLOutputFormat}). This type is
 * related to the {@link Configuration#setOutputFormat(OutputFormat)} and {@link Configuration#setAutoEscaping(boolean)}
 * mechanism; see more there. Values of this type are exempt from automatic escaping with that mechanism.
 * 
 * @param <MO>
 *            Refers to the interface's own type, which is useful in interfaces that extend
 *            {@link TemplateMarkupOutputModel} (Java Generics trick).
 * 
 * @since 2.3.24
 */
public interface TemplateMarkupOutputModel<MO extends TemplateMarkupOutputModel<MO>> extends TemplateModel {

    MarkupOutputFormat<MO> getOutputFormat();
    
}
