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

import java.util.Collection;
import java.util.Collections;

/**
 * Used as the return value of {@link TemplateElement#accept(Environment)} when the invoked element has nested elements
 * to invoke. It would be more natural to invoke child elements before returning from
 * {@link TemplateElement#accept(Environment)}, however, if there's nothing to do after the child elements were invoked,
 * that would mean wasting stack space.
 * 
 * @since 2.3.24
 */
class TemplateElementsToVisit {

    private final Collection<TemplateElement> templateElements;

    TemplateElementsToVisit(Collection<TemplateElement> templateElements) {
        this.templateElements = null != templateElements ? templateElements : Collections.<TemplateElement> emptyList();
    }

    TemplateElementsToVisit(TemplateElement nestedBlock) {
        this(Collections.singleton(nestedBlock));
    }

    Collection<TemplateElement> getTemplateElements() {
        return templateElements;
    }
    
}
