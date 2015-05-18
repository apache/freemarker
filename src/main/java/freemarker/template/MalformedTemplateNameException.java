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

import java.io.IOException;

import freemarker.cache.TemplateNameFormat;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that the template name given was malformed according the {@link TemplateNameFormat} in use. Note that for
 * backward compatibility, {@link TemplateNameFormat#DEFAULT_2_3_0} doesn't throw this exception,
 * {@link TemplateNameFormat#DEFAULT_2_4_0} does. This exception extends {@link IOException} for backward compatibility.
 * 
 * @since 2.3.22
 * 
 * @see TemplateNotFoundException
 * @see Configuration#getTemplate(String)
 */
public class MalformedTemplateNameException extends IOException {
    
    private final String templateName;
    private final String malformednessDescription;

    public MalformedTemplateNameException(String templateName, String malformednessDescription) {
        super("Malformed template name, " + StringUtil.jQuote(templateName) + ": " + malformednessDescription);
        this.templateName = templateName;
        this.malformednessDescription = malformednessDescription;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getMalformednessDescription() {
        return malformednessDescription;
    }

}
