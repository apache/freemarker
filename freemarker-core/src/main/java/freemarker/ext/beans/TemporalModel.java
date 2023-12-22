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

import java.time.temporal.Temporal;
import java.util.Date;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateTemporalModel;

/**
 * Wraps arbitrary subclass of {@link Date} into a reflective model.
 * Beside acting as a {@link TemplateDateModel}, you can call all Java methods
 * on these objects as well.
 */
public class TemporalModel extends BeanModel implements TemplateTemporalModel {
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            @Override
            public TemplateModel create(Object object, ObjectWrapper wrapper) {
                return new TemporalModel((Temporal) object, (BeansWrapper) wrapper);
            }
        };

    private final Temporal temporal;

    public TemporalModel(Temporal temporal, BeansWrapper wrapper) {
        super(temporal, wrapper);
        if (temporal == null) {
            throw new IllegalArgumentException("temporal == null");
        }
        this.temporal = temporal;
    }

    @Override
    public Temporal getAsTemporal() {
        return temporal;
    }

}
