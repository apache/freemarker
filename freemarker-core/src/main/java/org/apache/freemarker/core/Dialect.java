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

/**
 * A {@linkplain TemplateLanguage template language} dialect; specifies what predefined callables (like directives,
 * built-ins) are available in the template language, but it doesn't specify the syntax. Unlike callables exposed to
 * templates otherwise, callables that are part of the dialect use the same syntax as the callables traditionally
 * predefined ones (like {@code <#if ...>}, {@code ?upperCase}", etc.), and they are linked statically (see
 * {@link StaticallyLinkedNamespaceEntry} for the advantages).
 * 
 * <p>
 * A {@link Dialect} object is usually a static singleton. If you need internal state that's bound to a
 * {@link Configuration} instance, you can do that in the {@link ConfiguredDialect} created by
 * {@link Dialect#createConfiguredDialect(Configuration)} object.
 * 
 * @see StaticallyLinkedNamespaceEntry
 */
//TODO [FM3] will be public. Also, then move it into core.dialect?
abstract class Dialect {
    
    private final String name;
    private final Version version;
    
    public Dialect(String name, Version version) {
        this.name = name;
        this.version = version;
    }

    /**
     * The informal name of this dialect (maybe used in error messages)
     */
    public String getName() {
        return name;
    }
    
    /**
     * The version number of this dialect (maybe used in error messages).
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Creates a {@link ConfiguredDialect} object that's bound to the parameter {@link Configuration} object. FreeMarker
     * calls this method of each {@link Dialect} that will be used with the {@link Configuration} when the
     * {@link Configuration} object is constructed.
     * 
     * @param cfg
     *            The fully initialized {@link Configuration}, except that some {@link Dialect}-s in it may not have an
     *            associated {@link ConfiguredDialect} yet. This {@link Configuration} is not yet "safely published"
     *            (see in the Java Memory Model), so it must not be exposed to other threads by this method.
     * 
     * @throws ConfigurationException
     *             If any problem occurs, it should be wrapped into this.
     */
    public abstract ConfiguredDialect createConfiguredDialect(Configuration cfg) throws ConfigurationException;
    
    /**
     * A {@link Dialect} that is bound to a {@link Configuration}. While a dialect is in principle a static singleton,
     * and so is independent of any particular {@link Configuration} object, some dialects may have internal state
     * initialized depending on some {@link Configuration} settings.
     * 
     * <p>
     * Instances should be created by {@link Dialect#createConfiguredDialect(Configuration)}. The concrete
     * {@link ConfiguredDialect} is usually private and is a nested class in the concrete {@link Dialect}, which
     * overrides {@link Dialect#createConfiguredDialect(Configuration)} to return the concrete instance
     * 
     * <p>
     * The main function of a {@link ConfiguredDialect} is to expose the namespace of the dialect. The dialect namespace
     * contains the entries that are statically linked, and can usually can be accessed without any namespace prefix (
     * usually, as the exact details depend on the template language). For example, for the default dialect the
     * namespace contains entries like "if", "list", "upperCase", and so on.
     */
    //TODO [FM3] will be public. Also, then move it into core.dialect?
    abstract class ConfiguredDialect {
        
        /**
         * Returns a namespace entry of this dialect. or {@code null} if there's no match. 
         */
        public abstract StaticallyLinkedNamespaceEntry getNamespaceEntry(String name);
        
        /**
         * To iterate through the namespace entries of this dialect.
         */
        public abstract Iterable<StaticallyLinkedNamespaceEntry> getNamespaceEntries();
        
        /**
         * Returns the {@link Dialect} whose {@link Dialect#createConfiguredDialect(Configuration)} method has created this
         * instance.
         */
        public final Dialect getDialect() {
            return Dialect.this;
        }
        
    }

    // Final implementation, as {@link Dialect}-s maybe used as hash keys.
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    // Final implementation, as {@link Dialect}-s maybe used as hash keys.
    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }
    
}
