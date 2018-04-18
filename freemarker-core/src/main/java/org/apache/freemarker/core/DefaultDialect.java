package org.apache.freemarker.core;

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

import java.util.HashMap;
import java.util.Map;

import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.UnregisteredOutputFormatException;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;

/**
 * The default {@link Dialect} for FreeMarker. Most applications are expected to use this. 
 */
//TODO [FM3][DIALECTS] will be public. Also, then move it into core.dialect?
final class DefaultDialect extends Dialect {
    
    public static final DefaultDialect INSTANCE = new DefaultDialect(); 

    private DefaultDialect() {
        super("Default FreeMarker 3 Dialect", Configuration.getVersion());
    }

    @Override
    public ConfiguredDialect createConfiguredDialect(Configuration cfg) {
        return new ConfiguredDefaultDialect(cfg);
    }

    private class ConfiguredDefaultDialect extends ConfiguredDialect {
        private final Map<String, StaticallyLinkedNamespaceEntry> namespaceEntriesByName;

        ConfiguredDefaultDialect(Configuration cfg) {
            MarkupOutputFormat<?> htmlOutputFormat;
            try {
                htmlOutputFormat = cfg.getMarkupOutputFormat(HTMLOutputFormat.INSTANCE.getName());
            } catch (UnregisteredOutputFormatException e) {
                throw new ConfigurationException("Couldn't get HTML output format.", e);
            }
            namespaceEntriesByName = new HashMap<>(16, 0.5f); // The speed of get(key) is important
            //!!T Test entries until FREEMARKER-99 is finished: 
            addNamespaceEntry(
                    new StaticallyLinkedNamespaceEntry("adhocTest1", ASTDirAdhocTest1.VALUE,
                            new ASTDirAdhocTest1.Factory(htmlOutputFormat), null));
            addNamespaceEntry(
                    new StaticallyLinkedNamespaceEntry("adhocTest2", ASTDirAdhocTest2.VALUE,
                    ASTDirAdhocTest2.FACTORY, null));
        }

        @Override
        public StaticallyLinkedNamespaceEntry getNamespaceEntry(String name) {
            return namespaceEntriesByName.get(name);
        }

        @Override
        public Iterable<StaticallyLinkedNamespaceEntry> getNamespaceEntries() {
            return namespaceEntriesByName.values();
        }
        
        private void addNamespaceEntry(StaticallyLinkedNamespaceEntry entry) {
            namespaceEntriesByName.put(entry.getName(), entry);
        }
        
    }

}
