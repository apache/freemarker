/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.cache;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that uses a {@link Map} with {@code byte[]} as its source of templates. This is similar to
 * {@link StringTemplateLoader}, but uses {@code byte[]} instead of {@link String}; see more details there.
 * 
 * @since 2.3.24
 */
public class ByteArrayTemplateLoader implements TemplateLoader {
    
    private final Map<String, ByteArrayTemplateSource> templates = new HashMap<String, ByteArrayTemplateSource>();
    
    /**
     * Puts a template into the loader. A call to this method is identical to 
     * the call to the three-arg {@link #putTemplate(String, byte[], long)} 
     * passing <tt>System.currentTimeMillis()</tt> as the third argument.
     * @param name the name of the template.
     * @param templateSource the source code of the template.
     */
    public void putTemplate(String name, byte[] templateSource) {
        putTemplate(name, templateSource, System.currentTimeMillis());
    }
    
    /**
     * Puts a template into the loader. The name can contain slashes to denote
     * logical directory structure, but must not start with a slash. If the 
     * method is called multiple times for the same name and with different
     * last modified time, the configuration's template cache will reload the 
     * template according to its own refresh settings (note that if the refresh 
     * is disabled in the template cache, the template will not be reloaded).
     * Also, since the cache uses lastModified to trigger reloads, calling the
     * method with different source and identical timestamp won't trigger
     * reloading.
     * @param name the name of the template.
     * @param templateSource the source code of the template.
     * @param lastModified the time of last modification of the template in 
     * terms of <tt>System.currentTimeMillis()</tt>
     */
    public void putTemplate(String name, byte[] templateSource, long lastModified) {
        templates.put(name, new ByteArrayTemplateSource(name, templateSource, lastModified));
    }
    
    public void closeTemplateSource(Object templateSource) {
    }
    
    public Object findTemplateSource(String name) {
        return templates.get(name);
    }
    
    public long getLastModified(Object templateSource) {
        return ((ByteArrayTemplateSource) templateSource).lastModified;
    }
    
    public Reader getReader(Object templateSource, String encoding) throws UnsupportedEncodingException {
        return new InputStreamReader(
                new ByteArrayInputStream(((ByteArrayTemplateSource) templateSource).source),
                encoding);
    }
    
    private static class ByteArrayTemplateSource {
        private final String name;
        private final byte[] source;
        private final long lastModified;
        
        ByteArrayTemplateSource(String name, byte[] source, long lastModified) {
            if (name == null) {
                throw new IllegalArgumentException("name == null");
            }
            if (source == null) {
                throw new IllegalArgumentException("source == null");
            }
            if (lastModified < -1L) {
                throw new IllegalArgumentException("lastModified < -1L");
            }
            this.name = name;
            this.source = source;
            this.lastModified = lastModified;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ByteArrayTemplateSource) {
                return name.equals(((ByteArrayTemplateSource) obj).name);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return name.hashCode();
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
        for (Iterator it = templates.keySet().iterator(); it.hasNext(); ) {
            cnt++;
            if (cnt != 1) {
                sb.append(", ");
            }
            if (cnt > 10) {
                sb.append("...");
                break;
            }
            sb.append(StringUtil.jQuote(it.next()));
            sb.append("=...");
        }
        if (cnt != 0) {
            sb.append(' ');
        }
        sb.append("})");
        return sb.toString();
    }
    
}
