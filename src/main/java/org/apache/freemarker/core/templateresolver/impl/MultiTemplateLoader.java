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

package org.apache.freemarker.core.templateresolver.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResultStatus;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.util.NullArgumentException;

/**
 * A {@link TemplateLoader} that uses a set of other loaders to load the templates. On every request, loaders are
 * queried in the order of their appearance in the array of loaders provided to the constructor. Except, when the
 * {@linkplain #setSticky(boolean)} sticky} setting is set to {@code true} (default is false {@code false}), if
 * a request for some template name was already satisfied in the past by one of the loaders, that loader is queried
 * first (stickiness).
 * 
 * <p>This class is thread-safe.
 */
// TODO JUnit test
public class MultiTemplateLoader implements TemplateLoader {

    private final TemplateLoader[] templateLoaders;
    private final Map<String, TemplateLoader> lastTemplateLoaderForName = new ConcurrentHashMap<String, TemplateLoader>();
    
    private boolean sticky = false;

    /**
     * Creates a new instance that will use the specified template loaders.
     * 
     * @param templateLoaders
     *            the template loaders that are used to load templates, in the order as they will be searched
     *            (except where {@linkplain #setSticky(boolean) stickiness} says otherwise).
     */
    public MultiTemplateLoader(TemplateLoader... templateLoaders) {
        NullArgumentException.check("templateLoaders", templateLoaders);
        this.templateLoaders = templateLoaders.clone();
    }

    /**
     * Clears the sickiness memory, also resets the state of all enclosed {@link TemplateLoader}-s.
     */
    @Override
    public void resetState() {
        lastTemplateLoaderForName.clear();
        for (TemplateLoader templateLoader : templateLoaders) {
            templateLoader.resetState();
        }
    }

    /**
     * Show class name and some details that are useful in template-not-found errors.
     * 
     * @since 2.3.21
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MultiTemplateLoader(");
        for (int i = 0; i < templateLoaders.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("loader").append(i + 1).append(" = ").append(templateLoaders[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Returns the number of {@link TemplateLoader}-s directly inside this {@link TemplateLoader}.
     * 
     * @since 2.3.23
     */
    public int getTemplateLoaderCount() {
        return templateLoaders.length;
    }

    /**
     * Returns the {@link TemplateLoader} at the given index.
     * 
     * @param index
     *            Must be below {@link #getTemplateLoaderCount()}.
     */
    public TemplateLoader getTemplateLoader(int index) {
        return templateLoaders[index];
    }

    /**
     * Getter pair of {@link #setSticky(boolean)}.
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * Sets if for a name that was already loaded earlier the same {@link TemplateLoader} will be tried first, or
     * we always try the {@link TemplateLoader}-s strictly in the order as it was specified in the constructor.
     * The default is {@code false}.
     */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        TemplateLoader lastLoader = null;
        if (sticky) {
            // Use soft affinity - give the loader that last found this
            // resource a chance to find it again first.
            lastLoader = lastTemplateLoaderForName.get(name);
            if (lastLoader != null) {
                TemplateLoadingResult result = lastLoader.load(name, ifSourceDiffersFrom, ifVersionDiffersFrom, session);
                if (result.getStatus() != TemplateLoadingResultStatus.NOT_FOUND) {
                    return result;
                }
            }
        }

        // If there is no affine loader, or it could not find the resource
        // again, try all loaders in order of appearance. If any manages
        // to find the resource, then associate it as the new affine loader
        // for this resource.
        for (TemplateLoader templateLoader : templateLoaders) {
            if (lastLoader != templateLoader) {
                TemplateLoadingResult result = templateLoader.load(
                        name, ifSourceDiffersFrom, ifVersionDiffersFrom, session);
                if (result.getStatus() != TemplateLoadingResultStatus.NOT_FOUND) {
                    if (sticky) {
                        lastTemplateLoaderForName.put(name, templateLoader);
                    }
                    return result;
                }
            }
        }

        if (sticky) {
            lastTemplateLoaderForName.remove(name);
        }
        return TemplateLoadingResult.NOT_FOUND;
    }

}
