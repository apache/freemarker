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

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import freemarker.template.utility.StringUtil;

/**
 * A {@link TemplateLoader} that uses a Map with Strings as its source of 
 * templates.
 *
 * In most case the regular way of loading templates from files will be fine.
 * However, there can be situations where you don't want to or can't load a
 * template from a file, e.g. if you have to deploy a single jar for 
 * JavaWebStart or if they are contained within a database.
 * A single template can be created manually
 * e.g.
 * <pre>
 *   String templateStr="Hello ${user}";
 *   Template t = new Template("name", new StringReader(templateStr),
 *               new Configuration());
 * </pre>
 * If, however, you want to create templates from strings which import other 
 * templates this method doesn't work.
 *
 * In that case you can create a StringTemplateLoader and add each template to 
 * it:
 * <pre>
 *   StringTemplateLoader stringLoader = new StringTemplateLoader();
 *   stringLoader.putTemplate("greetTemplate", "&lt;#macro greet&gt;Hello&lt;/#macro&gt;");
 *   stringLoader.putTemplate("myTemplate", "&lt;#include \"greetTemplate\"&gt;&lt;@greet/&gt; World!");
 * </pre>
 * Then you tell your Configuration object to use it:
 * <pre>
 *   cfg.setTemplateLoader(stringLoader);
 * </pre>
 * After that you should be able to use the templates as usual. Often you will
 * want to combine a <tt>StringTemplateLoader</tt> with another loader. You can
 * do so using a {@link freemarker.cache.MultiTemplateLoader}.
 */
public class StringTemplateLoader implements TemplateLoader {
    private final Map templates = new HashMap();
    
    /**
     * Puts a template into the loader. A call to this method is identical to 
     * the call to the three-arg {@link #putTemplate(String, String, long)} 
     * passing <tt>System.currentTimeMillis()</tt> as the third argument.
     * @param name the name of the template.
     * @param templateSource the source code of the template.
     */
    public void putTemplate(String name, String templateSource) {
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
    public void putTemplate(String name, String templateSource, long lastModified) {
        templates.put(name, new StringTemplateSource(name, templateSource, lastModified));
    }
    
    public void closeTemplateSource(Object templateSource) {
    }
    
    public Object findTemplateSource(String name) {
        return templates.get(name);
    }
    
    public long getLastModified(Object templateSource) {
        return ((StringTemplateSource)templateSource).lastModified;
    }
    
    public Reader getReader(Object templateSource, String encoding) {
        return new StringReader(((StringTemplateSource)templateSource).source);
    }
    
    private static class StringTemplateSource {
        private final String name;
        private final String source;
        private final long lastModified;
        
        StringTemplateSource(String name, String source, long lastModified) {
            if(name == null) {
                throw new IllegalArgumentException("name == null");
            }
            if(source == null) {
                throw new IllegalArgumentException("source == null");
            }
            if(lastModified < -1L) {
                throw new IllegalArgumentException("lastModified < -1L");
            }
            this.name = name;
            this.source = source;
            this.lastModified = lastModified;
        }
        
        public boolean equals(Object obj) {
            if(obj instanceof StringTemplateSource) {
                return name.equals(((StringTemplateSource)obj).name);
            }
            return false;
        }
        
        public int hashCode() {
            return name.hashCode();
        }
    }
    
    /**
     * Show class name and some details that are useful in template-not-found errors.
     * 
     * @since 2.3.21
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
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
