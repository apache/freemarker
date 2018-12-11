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

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * A holder for builtins that operate exclusively on hash left-hand value.
 */
class BuiltInsForHashes {

    static class keysBI extends BuiltInForHashEx {

        @Override
        TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateException, InvalidReferenceException {
            TemplateCollectionModel keys = hashExModel.keys();
            if (keys == null) throw newNullPropertyException("keys", hashExModel, env);
            return keys;
        }
        
    }
    
    static class valuesBI extends BuiltInForHashEx {
        @Override
        TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateException, InvalidReferenceException {
            TemplateCollectionModel values = hashExModel.values();
            if (values == null) throw newNullPropertyException("values", hashExModel, env);
            return values;
        }
    }

    // Can't be instantiated
    private BuiltInsForHashes() { }
    
}
