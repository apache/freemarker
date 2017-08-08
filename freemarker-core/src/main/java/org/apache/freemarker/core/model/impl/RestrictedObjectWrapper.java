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

package org.apache.freemarker.core.model.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;

/**
 * A restricted version of {@link DefaultObjectWrapper} that doesn't expose arbitrary object, just those that directly
 * correspond to the {@link TemplateModel} sub-interfaces ({@code String}, {@code Map} and such). If it had to wrap
 * other kind of objects, it will throw exception. It will also block {@code ?api} calls on the values it wraps.
 */
public class RestrictedObjectWrapper extends DefaultObjectWrapper {

    protected RestrictedObjectWrapper(Builder builder, boolean finalizeConstruction) {
        super(builder, finalizeConstruction);
    }

    /**
     * Called if a type other than the simple ones we know about is passed in. 
     * In this implementation, this just throws an exception.
     */
    @Override
    protected TemplateModel wrapGenericObject(Object obj) throws TemplateModelException {
        throw new TemplateModelException("RestrictedObjectWrapper deliberately won't wrap this type: "
                + obj.getClass().getName());
    }

    @Override
    public TemplateHashModel wrapAsAPI(Object obj) throws TemplateModelException {
        throw new TemplateModelException("RestrictedObjectWrapper deliberately doesn't allow ?api.");
    }

    protected static abstract class ExtendableBuilder<
            ProductT extends RestrictedObjectWrapper, SelfT extends ExtendableBuilder<ProductT,
            SelfT>> extends DefaultObjectWrapper.ExtendableBuilder<ProductT, SelfT> {

        protected ExtendableBuilder(Version incompatibleImprovements, boolean isIncompImprsAlreadyNormalized) {
            super(incompatibleImprovements, isIncompImprsAlreadyNormalized);
        }

    }

    public static final class Builder extends ExtendableBuilder<RestrictedObjectWrapper, Builder> {

        private final static Map<ClassLoader, Map<Builder, WeakReference<RestrictedObjectWrapper>>>
                INSTANCE_CACHE = new WeakHashMap<>();

        private final static ReferenceQueue<RestrictedObjectWrapper> INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue<>();
        private boolean alreadyBuilt;

        public Builder(Version incompatibleImprovements) {
            super(incompatibleImprovements, false);
        }

        @Override
        public RestrictedObjectWrapper build() {
            if (alreadyBuilt) {
                throw new IllegalStateException("build() can only be executed once.");
            }

            RestrictedObjectWrapper singleton = DefaultObjectWrapperTCCLSingletonUtils.getSingleton(
                    this, INSTANCE_CACHE, INSTANCE_CACHE_REF_QUEUE, ConstructorInvoker.INSTANCE);
            alreadyBuilt = true;
            return singleton;
        }

        private static class ConstructorInvoker
                implements DefaultObjectWrapperTCCLSingletonUtils._ConstructorInvoker<RestrictedObjectWrapper, Builder> {

            private static final ConstructorInvoker INSTANCE = new ConstructorInvoker();

            @Override
            public RestrictedObjectWrapper invoke(Builder builder) {
                return new RestrictedObjectWrapper(builder, true);
            }
        }

    }

}
