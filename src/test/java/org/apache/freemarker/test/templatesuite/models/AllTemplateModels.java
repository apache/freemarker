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

package org.apache.freemarker.test.templatesuite.models;

import java.util.Date;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;

/**
 * Implements all template models that are interesting when calling overloaded Java methods.
 */
public class AllTemplateModels implements
        TemplateScalarModel, TemplateNumberModel, TemplateDateModel, TemplateBooleanModel,
        TemplateHashModelEx, TemplateSequenceModel, TemplateCollectionModel {

    public static final AllTemplateModels INSTANCE = new AllTemplateModels();
    
    private final TemplateModelIterator EMPTY_ITERATOR = new TemplateModelIterator() {

        public TemplateModel next() throws TemplateModelException {
            return null;
        }

        public boolean hasNext() throws TemplateModelException {
            return false;
        }
        
    };
    
    private final TemplateCollectionModel EMPTY_COLLECTION = new TemplateCollectionModel() {

        public TemplateModelIterator iterator() throws TemplateModelException {
            return EMPTY_ITERATOR;
        }
    };
    
    public TemplateModel get(String key) throws TemplateModelException {
        return new SimpleScalar("value for key " + key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return true;
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return EMPTY_ITERATOR;
    }

    public TemplateModel get(int index) throws TemplateModelException {
        return null;
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

    public boolean getAsBoolean() throws TemplateModelException {
        return true;
    }

    public Date getAsDate() throws TemplateModelException {
        return new Date(0);
    }

    public int getDateType() {
        return TemplateDateModel.DATETIME;
    }

    @SuppressWarnings("boxing")
    public Number getAsNumber() throws TemplateModelException {
        return 1;
    }

    public String getAsString() throws TemplateModelException {
        return "s";
    }

}
