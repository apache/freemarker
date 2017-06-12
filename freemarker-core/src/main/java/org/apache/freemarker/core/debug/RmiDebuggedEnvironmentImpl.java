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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.MutableProcessingConfiguration;
import org.apache.freemarker.core.ProcessingConfiguration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
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

    private static final DefaultObjectWrapper OBJECT_WRAPPER = new DefaultObjectWrapper.Builder(Configuration
            .VERSION_3_0_0)
            .build();
    
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
            return new SimpleCollection(keySet(), OBJECT_WRAPPER);
        }

        @Override
        public TemplateCollectionModel values() throws TemplateModelException {
            Collection keys = keySet();
            List list = new ArrayList(keys.size());
            
            for (Iterator it = keys.iterator(); it.hasNext(); ) {
                list.add(get((String) it.next()));
            }
            return new SimpleCollection(list, OBJECT_WRAPPER);
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
                MutableProcessingConfiguration.ARITHMETIC_ENGINE_KEY,
                MutableProcessingConfiguration.BOOLEAN_FORMAT_KEY,
                MutableProcessingConfiguration.LOCALE_KEY,
                MutableProcessingConfiguration.NUMBER_FORMAT_KEY,
                Configuration.Builder.OBJECT_WRAPPER_KEY,
                MutableProcessingConfiguration.TEMPLATE_EXCEPTION_HANDLER_KEY);

        final ProcessingConfiguration ProcessingConfiguration;
        
        DebugConfigurableModel(ProcessingConfiguration processingConfiguration) {
            this.ProcessingConfiguration = processingConfiguration;
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
                return ((Configuration) ProcessingConfiguration).getSharedVariables().keySet();
            }
        
            @Override
            public TemplateModel get(String key) {
                return ((Configuration) ProcessingConfiguration).getWrappedSharedVariable(key);
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
            name = new SimpleScalar(template.getLookupName());
        }

        @Override
        Collection keySet() {
            return KEYS;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            if ("configuration".equals(key)) {
                try {
                    return (TemplateModel) getCachedWrapperFor(((Template) ProcessingConfiguration).getConfiguration());
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
                    return ((Environment) ProcessingConfiguration).getKnownVariableNames();
                } catch (TemplateModelException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        
            @Override
            public TemplateModel get(String key) throws TemplateModelException {
                return ((Environment) ProcessingConfiguration).getVariable(key);
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
                return ((Environment) ProcessingConfiguration).getCurrentNamespace();
            }
            if ("dataModel".equals(key)) {
                return ((Environment) ProcessingConfiguration).getDataModel();
            }
            if ("globalNamespace".equals(key)) {
                return ((Environment) ProcessingConfiguration).getGlobalNamespace();
            }
            if ("knownVariables".equals(key)) {
                return knownVariables;
            }
            if ("mainNamespace".equals(key)) {
                return ((Environment) ProcessingConfiguration).getMainNamespace();
            }
            if ("mainTemplate".equals(key)) {
                try {
                    return (TemplateModel) getCachedWrapperFor(((Environment) ProcessingConfiguration).getMainTemplate());
                } catch (RemoteException e) {
                    throw new TemplateModelException(e);
                }
            }
            if ("currentTemplate".equals(key)) {
                try {
                    return (TemplateModel) getCachedWrapperFor(((Environment) ProcessingConfiguration).getCurrentTemplate());
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
