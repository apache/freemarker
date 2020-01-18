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

import java.util.List;

import freemarker.template.utility.Constants;

/**
 * Singleton object representing nothing, used by ?if_exists built-in.
 * It is meant to be interpreted in the most sensible way possible in various contexts.
 * This can be returned to avoid exceptions.
 */

final class GeneralPurposeNothing
implements TemplateBooleanModel, TemplateScalarModel, TemplateSequenceModel, TemplateHashModelEx2, TemplateMethodModelEx {

    private static final TemplateModel instance = new GeneralPurposeNothing();

    private GeneralPurposeNothing() {
    }

    static TemplateModel getInstance() {
        return instance;
    }

    @Override
    public String getAsString() {
        return "";
    }

    @Override
    public boolean getAsBoolean() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public TemplateModel get(int i) throws TemplateModelException {
        throw new TemplateModelException("Can't get item from an empty sequence.");
    }

    @Override
    public TemplateModel get(String key) {
        return null;
    }

    @Override
    public Object exec(List args) {
        return null;
    }
    
    @Override
    public TemplateCollectionModel keys() {
        return Constants.EMPTY_COLLECTION;
    }

    @Override
    public TemplateCollectionModel values() {
        return Constants.EMPTY_COLLECTION;
    }

    @Override
    public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
        return Constants.EMPTY_KEY_VALUE_PAIR_ITERATOR;
    }
}
