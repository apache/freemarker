/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.dom;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapperExtension;
import org.w3c.dom.Node;

/**
 * Add this extension to {@link DefaultObjectWrapper} if you want {@link Node}-s to be wrapped into {@link NodeModel}-s.
 */
public class DOMDefaultObjectWrapperExtension extends DefaultObjectWrapperExtension {

    /**
     * The singleton instance of this class.
     */
    public static final DOMDefaultObjectWrapperExtension INSTANCE = new DOMDefaultObjectWrapperExtension();

    private DOMDefaultObjectWrapperExtension() {
        // private to hide it from outside
    }

    @Override
    public TemplateModel wrap(Object obj) {
        if (obj instanceof Node) {
            return NodeModel.wrap((Node) obj);
        }
        return null;
    }
}
