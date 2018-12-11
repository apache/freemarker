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

package org.apache.freemarker.core.templateresolver;

import java.io.IOException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.core.util._StringUtils;

/**
 * Indicates that the template name given was malformed according the {@link TemplateNameFormat} in use.
 * This exception extends {@link IOException} for backward compatibility.
 * 
 *
 * @see TemplateNotFoundException
 * @see Configuration#getTemplate(String)
 */
@SuppressWarnings("serial")
public class MalformedTemplateNameException extends IOException {
    
    private final String templateName;
    private final String malformednessDescription;

    public MalformedTemplateNameException(String templateName, String malformednessDescription) {
        super("Malformed template name, " + _StringUtils.jQuote(templateName) + ": " + malformednessDescription);
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
