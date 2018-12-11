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

package org.apache.freemarker.core.debug;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;

/**
 */
class RmiDebugModelImpl extends UnicastRemoteObject implements DebugModel {
    private static final long serialVersionUID = 1L;

    private final TemplateModel model;
    private final int type;
    
    RmiDebugModelImpl(TemplateModel model, int extraTypes) throws RemoteException {
        super();
        this.model = model;
        type = calculateType(model) + extraTypes;
    }

    private static DebugModel getDebugModel(TemplateModel tm) throws RemoteException {
        return (DebugModel) RmiDebuggedEnvironmentImpl.getCachedWrapperFor(tm);
    }
    @Override
    public String getAsString() throws TemplateException {
        return ((TemplateStringModel) model).getAsString();
    }

    @Override
    public Number getAsNumber() throws TemplateException {
        return ((TemplateNumberModel) model).getAsNumber();
    }

    @Override
    public Date getAsDate() throws TemplateException {
        return ((TemplateDateModel) model).getAsDate();
    }

    @Override
    public int getDateType() {
        return ((TemplateDateModel) model).getDateType();
    }

    @Override
    public boolean getAsBoolean() throws TemplateException {
        return ((TemplateBooleanModel) model).getAsBoolean();
    }

    @Override
    public int size() throws TemplateException {
        if (model instanceof TemplateSequenceModel) {
            return ((TemplateSequenceModel) model).getCollectionSize();
        }
        return ((TemplateHashModelEx) model).getHashSize();
    }

    @Override
    public DebugModel get(int index) throws TemplateException, RemoteException {
        return getDebugModel(((TemplateSequenceModel) model).get(index));
    }
    
    @Override
    public DebugModel[] get(int fromIndex, int toIndex) throws TemplateException, RemoteException {
        DebugModel[] dm = new DebugModel[toIndex - fromIndex];
        TemplateSequenceModel s = (TemplateSequenceModel) model;
        for (int i = fromIndex; i < toIndex; i++) {
            dm[i - fromIndex] = getDebugModel(s.get(i));
        }
        return dm;
    }

    @Override
    public DebugModel[] getCollection() throws TemplateException, RemoteException {
        List list = new ArrayList();
        TemplateModelIterator i = ((TemplateIterableModel) model).iterator();
        while (i.hasNext()) {
            list.add(getDebugModel(i.next()));
        }
        return (DebugModel[]) list.toArray(new DebugModel[list.size()]);
    }
    
    @Override
    public DebugModel get(String key) throws TemplateException, RemoteException {
        return getDebugModel(((TemplateHashModel) model).get(key));
    }
    
    @Override
    public DebugModel[] get(String[] keys) throws TemplateException, RemoteException {
        DebugModel[] dm = new DebugModel[keys.length];
        TemplateHashModel h = (TemplateHashModel) model;
        for (int i = 0; i < keys.length; i++) {
            dm[i] = getDebugModel(h.get(keys[i]));
        }
        return dm;
    }

    @Override
    public String[] keys() throws TemplateException {
        TemplateHashModelEx h = (TemplateHashModelEx) model;
        List list = new ArrayList();
        TemplateModelIterator i = h.keys().iterator();
        while (i.hasNext()) {
            list.add(((TemplateStringModel) i.next()).getAsString());
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    @Override
    public int getModelTypes() {
        return type;
    }
    
    private static int calculateType(TemplateModel model) {
        int type = 0;
        if (model instanceof TemplateStringModel) type += TYPE_STRING;
        if (model instanceof TemplateNumberModel) type += TYPE_NUMBER;
        if (model instanceof TemplateDateModel) type += TYPE_DATE;
        if (model instanceof TemplateBooleanModel) type += TYPE_BOOLEAN;
        if (model instanceof TemplateSequenceModel) type += TYPE_SEQUENCE;
        if (model instanceof TemplateIterableModel) type += TYPE_COLLECTION;
        if (model instanceof TemplateHashModelEx) type += TYPE_HASH_EX;
        if (model instanceof TemplateHashModel) type += TYPE_HASH;
        if (model instanceof TemplateFunctionModel) type += TYPE_FUNCTION;
        if (model instanceof TemplateDirectiveModel) type += TYPE_DIRECTIVE;
        return type;
    }
}
