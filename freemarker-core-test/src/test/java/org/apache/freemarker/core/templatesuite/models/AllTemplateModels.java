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

package org.apache.freemarker.core.templatesuite.models;

import java.util.Date;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * Implements all template models that are interesting when calling overloaded Java methods.
 */
public class AllTemplateModels implements
        TemplateStringModel, TemplateNumberModel, TemplateDateModel, TemplateBooleanModel,
        TemplateHashModelEx, TemplateSequenceModel, TemplateIterableModel {

    public static final AllTemplateModels INSTANCE = new AllTemplateModels();

    @Override
    public TemplateModel get(String key) throws TemplateException {
        return new SimpleString("value for key " + key);
    }

    @Override
    public boolean isEmptyHash() throws TemplateException {
        return true;
    }

    @Override
    public int getHashSize() throws TemplateException {
        return 0;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return TemplateModelIterator.EMPTY_ITERATOR;
    }

    @Override
    public TemplateModel get(int index) throws TemplateException {
        return null;
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        return 0;
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return true;
    }

    @Override
    public TemplateIterableModel keys() throws TemplateException {
        return EMPTY_ITERABLE;
    }

    @Override
    public TemplateIterableModel values() throws TemplateException {
        return EMPTY_ITERABLE;
    }

    @Override
    public boolean getAsBoolean() throws TemplateException {
        return true;
    }

    @Override
    public Date getAsDate() throws TemplateException {
        return new Date(0);
    }

    @Override
    public int getDateType() {
        return TemplateDateModel.DATE_TIME;
    }

    @Override
    @SuppressWarnings("boxing")
    public Number getAsNumber() throws TemplateException {
        return 1;
    }

    @Override
    public String getAsString() throws TemplateException {
        return "s";
    }

}
