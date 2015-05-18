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

import java.io.FileNotFoundException;

/**
 * Thrown when {@link Configuration#getTemplate(String)} (or similar) doesn't find a template.
 * This extends {@link FileNotFoundException} for backward compatibility, but in fact has nothing to do with files, as
 * FreeMarker can load templates from many other sources.
 *
 * @since 2.3.22
 * 
 * @see MalformedTemplateNameException
 * @see Configuration#getTemplate(String)
 */
public final class TemplateNotFoundException extends FileNotFoundException {
    
    private final String templateName;
    private final Object customLookupCondition;

    public TemplateNotFoundException(String templateName, Object customLookupCondition, String message) {
        super(message);
        this.templateName = templateName;
        this.customLookupCondition = customLookupCondition;
    }

    /**
     * The name (path) of the template that wasn't found.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * The custom lookup condition with which the template was requested, or {@code null} if there's no such condition.
     * See the {@code customLookupCondition} parameter of
     * {@link Configuration#getTemplate(String, java.util.Locale, Object, String, boolean, boolean)}.
     */
    public Object getCustomLookupCondition() {
        return customLookupCondition;
    }

}
