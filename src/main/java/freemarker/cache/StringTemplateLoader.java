/*
 * Copyright (c) 2005 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.cache;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

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
 *   stringLoader.putTemplate("greetTemplate", "<#macro greet>Hello</#macro>");
 *   stringLoader.putTemplate("myTemplate", "<#include \"greetTemplate\"><@greet/> World!");
 * </pre>
 * Then you tell your Configuration object to use it:
 * <pre>
 *   cfg.setTemplateLoader(stringLoader);
 * </pre>
 * After that you should be able to use the templates as usual. Often you will
 * want to combine a <tt>StringTemplateLoader</tt> with another loader. You can
 * do so using a {@link freemarker.cache.MultiTemplateLoader}.
 *
 * @author Meikel Bisping
 * @author Attila Szegedi
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
}
