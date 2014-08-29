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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

/**
 * Java {@link DateFormat}-based format.
 */
class JavaTemplateDateFormat extends TemplateDateFormat {
    
    private final DateFormat javaDateFormat;

    public JavaTemplateDateFormat(DateFormat javaDateFormat) {
        this.javaDateFormat = javaDateFormat;
    }
    
    public String format(TemplateDateModel dateModel) throws TemplateModelException {
        return javaDateFormat.format(dateModel.getAsDate());
    }

    public Date parse(String s) throws ParseException {
        return javaDateFormat.parse(s);
    }

    public String getDescription() {
        return javaDateFormat instanceof SimpleDateFormat
                ? ((SimpleDateFormat) javaDateFormat).toPattern()
                : javaDateFormat.toString();
    }

    public boolean isLocaleBound() {
        return true;
    }

}
