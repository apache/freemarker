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

package freemarker.test.templatesuite.models;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * Tests the impact that the isEmpty() has on template hash models.
 */
public class BooleanHash2 implements TemplateHashModel {

    /**
     * Gets a {@code TemplateModel} from the hash.
     *
     * @param key the name by which the {@code TemplateModel}
     * is identified in the template.
     * @return the {@code TemplateModel} referred to by the key,
     * or null if not found.
     */
    public TemplateModel get(String key) {
        return null;
    }

    /**
     * @return true if this object is empty.
     */
    public boolean isEmpty() {
        return false;
    }
}
