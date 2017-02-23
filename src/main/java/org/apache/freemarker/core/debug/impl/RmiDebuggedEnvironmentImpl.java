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

package org.apache.freemarker.core.debug.impl;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.freemarker.core.Configurable;
import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.debug.DebugModel;
import org.apache.freemarker.core.debug.DebuggedEnvironment;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.SimpleCollection;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.util.UndeclaredThrowableException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("serial")
class RmiDebuggedEnvironmentImpl extends RmiDebugModelImpl implements DebuggedEnvironment {

    private static final SoftCache CACHE = new SoftCache(new IdentityHashMap());
    private static final Object ID_LOCK = new Object();
    
    private static long nextId = 1;
    private static Set remotes = new HashSet();

    
    private boolean stopped = false;
    private final long id;
    
    private RmiDebuggedEnvironmentImpl(Environment env) throws RemoteException {
        super(new DebugEnvironmentModel(env), DebugModel.TYPE_ENVIRONMENT);
        synchronized (ID_LOCK) {
            id = nextId++;
        }
    }

    static synchronized Object getCachedWrapperFor(Object key)
    throws RemoteException {
        Object value = CACHE.get(key);
        if (value == null) {
            if (key instanceof TemplateModel) {
                int extraTypes;
                if (key instanceof DebugConfigurationModel) {
                    extraTypes = DebugModel.TYPE_CONFIGURATION;
                } else if (key instanceof DebugTemplateModel) {
                    extraTypes = DebugModel.TYPE_TEMPLATE;
                } else {
                    extraTypes = 0;
                }
                value = new RmiDebugModelImpl((TemplateModel) key, extraTypes);
            } else if (key instanceof Environment) {
                value = new RmiDebuggedEnvironmentImpl((Environment) key); 
            } else if (key instanceof Template) {
                value = new DebugTemplateModel((Template) key);
            } else if (key instanceof Configuration) {
                value = new DebugConfigurationModel((Configuration) key);
            }
        }
        if (value != null) {
            CACHE.put(key, value);
        }
        if (value instanceof Remote) {
            remotes.add(value);
        }
        return value;
    }

    // TODO See in SuppressFBWarnings
    @Override
    @SuppressFBWarnings(value="NN_NAKED_NOTIFY", justification="Will have to be re-desigend; postponed.")
    public void resume() {
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void stop() {
        stopped = true;
        resume();
    }

    @Override
    public long getId() {
        return id;
    }
    
    boolean isStopped() {
        return stopped;
    }
    
    private abstract static class DebugMapModel implements TemplateHashModelEx {
        @Override
        public int size() {
            return keySet().size();
        }

        @Override
        public TemplateCollectionModel keys() {
            return new SimpleCollection(keySet());
        }

        @Override
        public TemplateCollectionModel values() throws TemplateModelException {
            Collection keys = keySet();
            List list = new ArrayList(keys.size());
            
            for (Iterator it = keys.iterator(); it.hasNext(); ) {
                list.add(get((String) it.next()));
            }
            return new SimpleCollection(list);
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }
        
        abstract Collection keySet();

        static List composeList(Collection c1, Collection c2) {
            List list = new ArrayList(c1);
            list.addAll(c2);
            Collections.sort(list);
            return list;
        }
    }
    
    private static class DebugConfigurableModel extends DebugMapModel {
        static final List KEYS = Arrays.asList(
                Configurable.ARITHMETIC_ENGINE_KEY,
                Configurable.BOOLEAN_FORMAT_KEY,
                Configurable.LOCALE_KEY,
                Configurable.NUMBER_FORMAT_KEY,
                Configurable.OBJECT_WRAPPER_KEY,
                Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY);

        final Configurable configurable;
        
        DebugConfigurableModel(Configurable configurable) {
            this.configurable = configurable;
        }
        
        @Override
        Collection keySet() {
            return KEYS;
        }
        
        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            return null; // TODO
        }

    }
    
    private static class DebugConfigurationModel extends DebugConfigurableModel {
        private static final List KEYS = composeList(DebugConfigurableModel.KEYS, Collections.singleton("sharedVariables"));

        private TemplateModel sharedVariables = new DebugMapModel()
        {
            @Override
            Collection keySet() {
                return ((Configuration) configurable).getSharedVariableNames();
            }
        
            @Override
            public TemplateModel get(String key) {
                return ((Configuration) configurable).getSharedVariable(key);
            }
        };
        
        DebugConfigurationModel(Configuration config) {
            super(config);
        }
        
        @Override
        Collection keySet() {
            return KEYS;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            if ("sharedVariables".equals(key)) {
                return sharedVariables; 
            } else {
                return super.get(key);
            }
        }
    }
    
    private static class DebugTemplateModel extends DebugConfigurableModel {
        private static final List KEYS = composeList(DebugConfigurableModel.KEYS, 
            Arrays.asList("configuration", "name"));
    
        private final SimpleScalar name;

        DebugTemplateModel(Template template) {
            super(template);
            name = new SimpleScalar(template.getName());
        }

        @Override
        Collection keySet() {
            return KEYS;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            if ("configuration".equals(key)) {
                try {
                    return (TemplateModel) getCachedWrapperFor(((Template) configurable).getConfiguration());
                } catch (RemoteException e) {
                    throw new TemplateModelException(e);
                }
            }
            if ("name".equals(key)) {
                return name;
            }
            return super.get(key);
        }
    }

    private static class DebugEnvironmentModel extends DebugConfigurableModel {
        private static final List KEYS = composeList(DebugConfigurableModel.KEYS, 
            Arrays.asList(
                    "currentNamespace",
                    "dataModel",
                    "globalNamespace",
                    "knownVariables",
                    "mainNamespace",
                    "template"));
    
        private TemplateModel knownVariables = new DebugMapModel()
        {
            @Override
            Collection keySet() {
                try {
                    return ((Environment) configurable).getKnownVariableNames();
                } catch (TemplateModelException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        
            @Override
            public TemplateModel get(String key) throws TemplateModelException {
                return ((Environment) configurable).getVariable(key);
            }
        };
         
        DebugEnvironmentModel(Environment env) {
            super(env);
        }

        @Override
        Collection keySet() {
            return KEYS;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            if ("currentNamespace".equals(key)) {
                return ((Environment) configurable).getCurrentNamespace();
            }
            if ("dataModel".equals(key)) {
                return ((Environment) configurable).getDataModel();
            }
            if ("globalNamespace".equals(key)) {
                return ((Environment) configurable).getGlobalNamespace();
            }
            if ("knownVariables".equals(key)) {
                return knownVariables;
            }
            if ("mainNamespace".equals(key)) {
                return ((Environment) configurable).getMainNamespace();
            }
            if ("mainTemplate".equals(key)) {
                try {
                    return (TemplateModel) getCachedWrapperFor(((Environment) configurable).getMainTemplate());
                } catch (RemoteException e) {
                    throw new TemplateModelException(e);
                }
            }
            if ("currentTemplate".equals(key)) {
                try {
                    return (TemplateModel) getCachedWrapperFor(((Environment) configurable).getCurrentTemplate());
                } catch (RemoteException e) {
                    throw new TemplateModelException(e);
                }
            }
            return super.get(key);
        }
    }

    public static void cleanup() {
        for (Iterator i = remotes.iterator(); i.hasNext(); ) {
            Object remoteObject = i.next();
            try {
                UnicastRemoteObject.unexportObject((Remote) remoteObject, true);
            } catch (Exception e) {
            }
        }
    }
}
