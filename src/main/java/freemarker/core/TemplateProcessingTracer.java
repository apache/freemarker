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

package freemarker.core;

import freemarker.ext.util.IdentityHashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.utility.ObjectFactory;

/**
 * Run-time tracer plug-in. This may be * used to implement profiling, coverage analytis, execution tracing,
 * and other on-the-fly debugging mechanisms.
 * <p>
 * Use {@link Environment#setTracer(TemplateProcessingTracer)} to configure a tracer for the current environment.
 * 
 * @since 2.3.33
 */
public interface TemplateProcessingTracer {

    /**
     * Invoked when {@link Template.process()} starts processing the template.
     * 
     * @since 2.3.23
     */
    void start();

    /**
     * Invoked by {@link Environment} whenever it starts processing a new template element. {@code
     * isLeafElement} indicates whether this element is a leaf, or whether the tracer should expect
     * to receive lower-level elements within the context of this one.
     * 
     * @since 2.3.23
     */
    void trace(Template template, int beginColumn, int beginLine, int endColumn, int endLine,
            boolean isLeafElement);

    /**
     * Invoked when template processing is finished.
     * 
     * @since 2.3.23
     */
    void end();

}
