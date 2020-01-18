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

package freemarker.cache;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import freemarker.template.utility.NullArgumentException;

/**
 * A {@link TemplateLoader} that uses a set of other loaders to load the templates. On every request, loaders are
 * queried in the order of their appearance in the array of loaders provided to the constructor. However, by default, if
 * a request for some template name was already satisfied in the past by one of the loaders, that loader is queried
 * first (stickiness). This behavior can be disabled with {@link #setSticky(boolean)}, then the loaders are always
 * queried in the order of their appearance in the array.
 * 
 * <p>This class is thread-safe.
 */
public class MultiTemplateLoader implements StatefulTemplateLoader {

    private final TemplateLoader[] templateLoaders;
    private final Map<String, TemplateLoader> lastTemplateLoaderForName
            = new ConcurrentHashMap<>();
    
    private boolean sticky = true;

    /**
     * Creates a new instance that will use the specified template loaders.
     * 
     * @param templateLoaders
     *            the template loaders that are used to load templates, in the order as they will be searched
     *            (except where {@linkplain #setSticky(boolean) stickiness} says otherwise).
     */
    public MultiTemplateLoader(TemplateLoader[] templateLoaders) {
        NullArgumentException.check("templateLoaders", templateLoaders);
        this.templateLoaders = templateLoaders.clone();
    }

    @Override
    public Object findTemplateSource(String name)
            throws IOException {
        TemplateLoader lastTemplateLoader = null;
        if (sticky) {
            // Use soft affinity - give the loader that last found this
            // resource a chance to find it again first.
            lastTemplateLoader = lastTemplateLoaderForName.get(name);
            if (lastTemplateLoader != null) {
                Object source = lastTemplateLoader.findTemplateSource(name);
                if (source != null) {
                    return new MultiSource(source, lastTemplateLoader);
                }
            }
        }

        // If there is no affine loader, or it could not find the resource
        // again, try all loaders in order of appearance. If any manages
        // to find the resource, then associate it as the new affine loader
        // for this resource.
        for (TemplateLoader templateLoader : templateLoaders) {
            if (lastTemplateLoader != templateLoader) {
                Object source = templateLoader.findTemplateSource(name);
                if (source != null) {
                    if (sticky) {
                        lastTemplateLoaderForName.put(name, templateLoader);
                    }
                    return new MultiSource(source, templateLoader);
                }
            }
        }

        if (sticky) {
            lastTemplateLoaderForName.remove(name);
        }
        // Resource not found
        return null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return ((MultiSource) templateSource).getLastModified();
    }

    @Override
    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        return ((MultiSource) templateSource).getReader(encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource)
            throws IOException {
        ((MultiSource) templateSource).close();
    }

    /**
     * Clears the sickiness memory, also resets the state of all enclosed {@link StatefulTemplateLoader}-s.
     */
    @Override
    public void resetState() {
        lastTemplateLoaderForName.clear();
        for (TemplateLoader loader : templateLoaders) {
            if (loader instanceof StatefulTemplateLoader) {
                ((StatefulTemplateLoader) loader).resetState();
            }
        }
    }

    /**
     * Represents a template source bound to a specific template loader. It serves as the complete template source
     * descriptor used by the MultiTemplateLoader class.
     */
    static final class MultiSource {

        private final Object source;
        private final TemplateLoader loader;

        MultiSource(Object source, TemplateLoader loader) {
            this.source = source;
            this.loader = loader;
        }

        long getLastModified() {
            return loader.getLastModified(source);
        }

        Reader getReader(String encoding)
                throws IOException {
            return loader.getReader(source, encoding);
        }

        void close()
                throws IOException {
            loader.closeTemplateSource(source);
        }

        Object getWrappedSource() {
            return source;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MultiSource) {
                MultiSource m = (MultiSource) o;
                return m.loader.equals(loader) && m.source.equals(source);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return loader.hashCode() + 31 * source.hashCode();
        }

        @Override
        public String toString() {
            return source.toString();
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
     * 
     * @since 2.3.24
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * Sets if for a name that was already loaded earlier the same {@link TemplateLoader} will be tried first, or
     * we always try the {@link TemplateLoader}-s strictly in the order as it was specified in the constructor.
     * The default is {@code true}.
     * 
     * @since 2.3.24
     */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }


}
