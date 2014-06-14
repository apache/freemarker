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

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;

/**
 * Wraps arbitrary subclass of {@link java.lang.Number} into a reflective model.
 * Beside acting as a {@link TemplateNumberModel}, you can call all Java methods on
 * these objects as well.
 */
public class NumberModel
extends
    BeanModel
implements
    TemplateNumberModel
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new NumberModel((Number)object, (BeansWrapper)wrapper);
            }
        };
    /**
     * Creates a new model that wraps the specified number object.
     * @param number the number object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public NumberModel(Number number, BeansWrapper wrapper)
    {
        super(number, wrapper);
    }

    public Number getAsNumber()
    {
        return (Number)object;
    }
}
