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

package freemarker.core;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * A holder for builtins that operate exclusively on hash left-hand value.
 */
class BuiltInsForHashes {

    static class keysBI extends BuiltInForHashEx {

        TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateModelException, InvalidReferenceException {
            TemplateCollectionModel keys = hashExModel.keys();
            if (keys == null) throw newNullPropertyException("keys", hashExModel, env);
            return keys instanceof TemplateSequenceModel ? keys : new CollectionAndSequence(keys);
        }
        
    }
    
    static class valuesBI extends BuiltInForHashEx {
        TemplateModel calculateResult(TemplateHashModelEx hashExModel, Environment env)
                throws TemplateModelException, InvalidReferenceException {
            TemplateCollectionModel values = hashExModel.values();
            if (values == null) throw newNullPropertyException("values", hashExModel, env);
            return values instanceof TemplateSequenceModel ? values : new CollectionAndSequence(values);
        }
    }

    // Can't be instantiated
    private BuiltInsForHashes() { }
    
}
