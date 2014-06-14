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

package freemarker.test.templatesuite.models;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;

public class HashAndScalarModel implements TemplateHashModelEx, TemplateScalarModel {
    
    public static final HashAndScalarModel INSTANCE = new HashAndScalarModel();
    
    private final TemplateCollectionModel EMPTY_COLLECTION = new TemplateCollectionModel() {

        public TemplateModelIterator iterator() throws TemplateModelException {
            return new TemplateModelIterator() {

                public TemplateModel next() throws TemplateModelException {
                    return null;
                }

                public boolean hasNext() throws TemplateModelException {
                    return false;
                }
                
            };
        }
    };

    public String getAsString() throws TemplateModelException {
        return "scalarValue";
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return new SimpleScalar("mapValue for " + key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return true;
    }

    public int size() throws TemplateModelException {
        return 0;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        return EMPTY_COLLECTION;
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        return EMPTY_COLLECTION;
    }

}
