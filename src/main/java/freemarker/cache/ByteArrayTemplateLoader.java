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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that uses a {@link Map} with {@code byte[]} as its source of templates. This is similar to
 * {@link StringTemplateLoader}, but uses {@code byte[]} instead of {@link String}; see more details there.
 * 
 * @since 2.3.24
 */
public class ByteArrayTemplateLoader implements TemplateLoader {

    private final Map<String, ByteArrayTemplateSource> templates = new HashMap<>();
    
    /**
     * Adds a template to this template loader; see {@link StringTemplateLoader#putTemplate(String, String)} for more.
     */
    public void putTemplate(String name, byte[] templateContent) {
        putTemplate(name, templateContent, System.currentTimeMillis());
    }
    
    /**
     * Adds a template to this template loader; see {@link StringTemplateLoader#putTemplate(String, String, long)} for
     * more.
     */
    public void putTemplate(String name, byte[] templateContent, long lastModified) {
        templates.put(name, new ByteArrayTemplateSource(name, templateContent, lastModified));
    }
    
    /**
     * Removes the template with the specified name if it was added earlier.; see
     * {@link StringTemplateLoader#removeTemplate(String)} for more details.
     * 
     * @since 2.3.26
     */
    public boolean removeTemplate(String name) {
        return templates.remove(name) != null;
    }
    
    @Override
    public void closeTemplateSource(Object templateSource) {
    }
    
    @Override
    public Object findTemplateSource(String name) {
        return templates.get(name);
    }
    
    @Override
    public long getLastModified(Object templateSource) {
        return ((ByteArrayTemplateSource) templateSource).lastModified;
    }
    
    @Override
    public Reader getReader(Object templateSource, String encoding) throws UnsupportedEncodingException {
        return new InputStreamReader(
                new ByteArrayInputStream(((ByteArrayTemplateSource) templateSource).templateContent),
                encoding);
    }
    
    private static class ByteArrayTemplateSource {
        private final String name;
        private final byte[] templateContent;
        private final long lastModified;
        
        ByteArrayTemplateSource(String name, byte[] templateContent, long lastModified) {
            if (name == null) {
                throw new IllegalArgumentException("name == null");
            }
            if (templateContent == null) {
                throw new IllegalArgumentException("templateContent == null");
            }
            if (lastModified < -1L) {
                throw new IllegalArgumentException("lastModified < -1L");
            }
            this.name = name;
            this.templateContent = templateContent;
            this.lastModified = lastModified;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ByteArrayTemplateSource other = (ByteArrayTemplateSource) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
        
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
    
}
