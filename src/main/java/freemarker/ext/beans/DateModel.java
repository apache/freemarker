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

package freemarker.ext.beans;

import java.util.Date;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;

/**
 * Wraps arbitrary subclass of {@link java.util.Date} into a reflective model.
 * Beside acting as a {@link TemplateDateModel}, you can call all Java methods
 * on these objects as well.
 */
public class DateModel
extends
    BeanModel
implements
    TemplateDateModel
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new DateModel((Date)object, (BeansWrapper)wrapper);
            }
        };

    private final int type;
    
    /**
     * Creates a new model that wraps the specified date object.
     * @param date the date object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public DateModel(Date date, BeansWrapper wrapper)
    {
        super(date, wrapper);
        if(date instanceof java.sql.Date) {
            type = DATE;
        }
        else if(date instanceof java.sql.Time) {
            type = TIME;
        }
        else if(date instanceof java.sql.Timestamp) {
            type = DATETIME;
        }
        else {
            type = wrapper.getDefaultDateType();
        }
    }

    public Date getAsDate() {
        return (Date)object;
    }

    public int getDateType() {
        return type;
    }
}
