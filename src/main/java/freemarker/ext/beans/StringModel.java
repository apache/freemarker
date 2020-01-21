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

package freemarker.ext.beans;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Subclass of {@link BeanModel} that exposes the return value of the {@link
 * java.lang.Object#toString()} method through the {@link TemplateScalarModel}
 * interface.
 */
public class StringModel extends BeanModel
implements TemplateScalarModel {
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            @Override
            public TemplateModel create(Object object, ObjectWrapper wrapper) {
                return new StringModel(object, (BeansWrapper) wrapper);
            }
        };

    // Package visible for testing
    static final String TO_STRING_NOT_EXPOSED = "[toString not exposed]";

    /**
     * Creates a new model that wraps the specified object with BeanModel + scalar
     * functionality.
     * @param object the object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public StringModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    /**
     * Returns the result of calling {@link Object#toString()} on the wrapped
     * object.
     */
    @Override
    public String getAsString() {
        boolean exposeToString = wrapper.getMemberAccessPolicy().isToStringAlwaysExposed()
                || !wrapper.getClassIntrospector().get(object.getClass())
                        .containsKey(ClassIntrospector.TO_STRING_HIDDEN_FLAG_KEY);
        return exposeToString ? object.toString() : TO_STRING_NOT_EXPOSED;
    }
}
