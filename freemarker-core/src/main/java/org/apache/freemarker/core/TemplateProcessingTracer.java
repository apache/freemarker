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

package org.apache.freemarker.core;

/**
 * Hooks to monitor as templates run. This may be used to implement profiling, coverage analysis, execution tracing,
 * and other on-the-fly debugging mechanisms.
 * <p>
 * Use {@link Environment#setTemplateProcessingTracer(TemplateProcessingTracer)} to set a tracer for the current
 * environment.
 */
public interface TemplateProcessingTracer {

    /**
     * Invoked by {@link Environment} whenever it starts processing a new template element. A template element is a
     * directive call, an interpolation (like <code>${...}</code>), a comment block, or static text. Expressions
     * are not template elements.
     */
    void enterElement(Environment env, TracedElement tracedElement);

    /**
     * Invoked by {@link Environment} whenever it completes processing a new template element.
     *
     * @see #enterElement(Environment, TracedElement)
     */
    void exitElement(Environment env);

    /**
     * Information about the template element that we enter of exit.
     */
    interface TracedElement {
        /**
         * The {@link Template} that contains this element.
         */
        Template getTemplate();

        /**
         * 1-based index of the line (row) of the first character of the element in the template.
         */
        int getBeginLine();

        /**
         * 1-based index of the column of the first character of the element in the template.
         */
        int getBeginColumn();

        /**
         * 1-based index of the line (row) of the last character of the element in the template.
         */
        int getEndColumn();

        /**
         * 1-based index of the column of the last character of the element in the template.
         */
        int getEndLine();

        /**
         * If this is an element that has no nested elements.
         */
        boolean isLeaf();

        /**
         * One-line description of the element, that also contains the parameter expressions, but not the nested content
         * (child elements). There are no hard backward-compatibility guarantees regarding the format used, although
         * it shouldn't change unless to fix a bug.
         */
        String getLabelWithParameters();
    }

}
