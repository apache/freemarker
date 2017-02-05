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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that uses a {@link Map} with {@code String} as its source of templates. This is similar to
 * {@link StringTemplateLoader}, but uses {@code String} instead of {@link String}; see more details there.
 * 
 * <p>Note that {@link StringTemplateLoader} can't be used with a distributed (cluster-wide) {@link CacheStorage},
 * as it produces {@link TemplateLoadingSource}-s that deliberately throw exception on serialization (because the
 * content is only accessible within a single JVM, and is also volatile).
 */
// TODO JUnit tests
public class StringTemplateLoader implements TemplateLoader {
    
    private static final AtomicLong INSTANCE_COUNTER = new AtomicLong();
    
    private final long instanceId = INSTANCE_COUNTER.get();
    private final AtomicLong templatesRevision = new AtomicLong();
    private final ConcurrentMap<String, ContentHolder> templates = new ConcurrentHashMap<>();
    
    /**
     * Puts a template into the template loader. The name can contain slashes to denote logical directory structure, but
     * must not start with a slash. Each template will get an unique revision number, thus replacing a template will
     * cause the template cache to reload it (when the update delay expires).
     * 
     * <p>This method is thread-safe.
     * 
     * @param name
     *            the name of the template.
     * @param content
     *            the source code of the template.
     */
    public void putTemplate(String name, String content) {
        templates.put(
                name,
                new ContentHolder(content, new Source(instanceId, name), templatesRevision.incrementAndGet()));
    }
    
    /**
     * Removes the template with the specified name if it was added earlier.
     * 
     * <p>
     * This method is thread-safe.
     * 
     * @param name
     *            Exactly the key with which the template was added.
     * 
     * @return Whether a template was found with the given key (and hence was removed now)
     */ 
    public boolean removeTemplate(String name) {
        return templates.remove(name) != null;
    }
    
    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        ContentHolder contentHolder = templates.get(name);
        if (contentHolder == null) {
            return TemplateLoadingResult.NOT_FOUND;
        } else if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(contentHolder.source)
                && Objects.equals(ifVersionDiffersFrom, contentHolder.version)) {
            return TemplateLoadingResult.NOT_MODIFIED;
        } else {
            return new TemplateLoadingResult(
                    contentHolder.source, contentHolder.version,
                    new StringReader(contentHolder.content),
                    null);
        }
    }

    @Override
    public void resetState() {
        // Do nothing
    }

    /**
     * Show class name and some details that are useful in template-not-found errors.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TemplateLoaderUtils.getClassNameForToString(this));
        sb.append("(Map { ");
        int cnt = 0;
        for (String name : templates.keySet()) {
            cnt++;
            if (cnt != 1) {
                sb.append(", ");
            }
            if (cnt > 10) {
                sb.append("...");
                break;
            }
            sb.append(StringUtil.jQuote(name));
            sb.append("=...");
        }
        if (cnt != 0) {
            sb.append(' ');
        }
        sb.append("})");
        return sb.toString();
    }

    private static class ContentHolder {
        private final String content;
        private final Source source;
        private final long version;
        
        public ContentHolder(String content, Source source, long version) {
            this.content = content;
            this.source = source;
            this.version = version;
        }
        
    }
    
    @SuppressWarnings("serial")
    private static class Source implements TemplateLoadingSource {
        
        private final long instanceId;
        private final String name;
        
        public Source(long instanceId, String name) {
            this.instanceId = instanceId;
            this.name = name;
        }
    
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (instanceId ^ (instanceId >>> 32));
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }
    
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Source other = (Source) obj;
            if (instanceId != other.instanceId) return false;
            if (name == null) {
                if (other.name != null) return false;
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }
        
        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException(StringTemplateLoader.class.getName()
                    + " sources can't be serialized, as they don't support clustering.");
        }
        
    }
    
}
