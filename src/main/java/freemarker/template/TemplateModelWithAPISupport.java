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

package freemarker.template;

import freemarker.template.utility.ObjectWrapperWithAPISupport;

/**
 * A {@link TemplateModel} on which the {@code ?api} operation can be applied.
 *
 * @since 2.3.22
 */
public interface TemplateModelWithAPISupport extends TemplateModel {

    /**
     * Returns the model that exposes the (Java) API of the value. This is usually implemented by delegating to
     * {@link ObjectWrapperWithAPISupport#wrapAsAPI(Object)}.
     */
    TemplateModel getAPI() throws TemplateModelException;

}
