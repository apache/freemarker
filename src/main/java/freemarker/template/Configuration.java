/*
 * Copyright (c) 2003-2006 The Visigoth Software Society. All rights
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

package freemarker.template;

import java.io.*;
import java.util.*;

import freemarker.cache.*;
import freemarker.core.*;
import freemarker.template.utility.*;

/**
 * Main entry point into the FreeMarker API, this class encapsulates the 
 * various configuration parameters with which FreeMarker is run, as well
 * as serves as a central template loading and caching point. Note that
 * this class uses a default strategy for loading 
 * and caching templates. You can plug in a replacement
 * template loading mechanism by using the {@link #setTemplateLoader(TemplateLoader)}
 * method.
 *
 * This object is <em>not synchronized</em>. Thus, the settings must not be changed
 * after you have started to access the object from multiple threads. If you use multiple
 * threads, set everything directly after you have instantiated the <code>Configuration</code>
 * object, and don't change the settings anymore.
 *
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @author Attila Szegedi
 * @version $Id: Configuration.java,v 1.122.2.5 2006/04/26 21:25:19 ddekany Exp $
 */

public class Configuration extends Configurable implements Cloneable {
    public static final String DEFAULT_ENCODING_KEY = "default_encoding"; 
    public static final String LOCALIZED_LOOKUP_KEY = "localized_lookup";
    public static final String STRICT_SYNTAX_KEY = "strict_syntax";
    public static final String WHITESPACE_STRIPPING_KEY = "whitespace_stripping";
    public static final String CACHE_STORAGE_KEY = "cache_storage";
    public static final String TEMPLATE_UPDATE_DELAY_KEY = "template_update_delay";
    public static final String AUTO_IMPORT_KEY = "auto_import";
    public static final String AUTO_INCLUDE_KEY = "auto_include";
    public static final String TAG_SYNTAX_KEY = "tag_syntax";
    public static final String EMULATE_2_3_PARSER_BUGS = "emulate_2_3_parser_bugs";
    public static final int AUTO_DETECT_TAG_SYNTAX = 0;
    public static final int ANGLE_BRACKET_TAG_SYNTAX = 1;
    public static final int SQUARE_BRACKET_TAG_SYNTAX = 2;

    private static Configuration defaultConfig = new Configuration();
    private static String cachedVersion;
    private boolean strictSyntax = true, localizedLookup = true, whitespaceStripping = true;
    private int tagSyntax = ANGLE_BRACKET_TAG_SYNTAX;

    private TemplateCache cache;
    private HashMap variables = new HashMap();
    private HashMap encodingMap = new HashMap();
    private Map autoImportMap = new HashMap();
    private ArrayList autoImports = new ArrayList(), autoIncludes = new ArrayList();
    private String defaultEncoding = SecurityUtilities.getSystemProperty("file.encoding");
     

    public Configuration() {
        cache = new TemplateCache();
        cache.setConfiguration(this);
        cache.setDelay(5000);
        loadBuiltInSharedVariables();
    }

    public Object clone() {
        try {
            Configuration copy = (Configuration)super.clone();
            copy.variables = new HashMap(variables);
            copy.encodingMap = new HashMap(encodingMap);
            copy.createTemplateCache(cache.getTemplateLoader(), cache.getCacheStorage());
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone is not supported, but it should be: " + e.getMessage());
        }
    }
    
    private void loadBuiltInSharedVariables() {
        variables.put("capture_output", new CaptureOutput());
        variables.put("compress", StandardCompress.INSTANCE);
        variables.put("html_escape", new HtmlEscape());
        variables.put("normalize_newlines", new NormalizeNewlines());
        variables.put("xml_escape", new XmlEscape());
    }
    
    /**
     * Loads a preset language-to-encoding map. It assumes the usual character
     * encodings for most languages.
     * The previous content of the encoding map will be lost.
     * This default map currently contains the following mappings:
     * <table>
     *   <tr><td>ar</td><td>ISO-8859-6</td></tr>
     *   <tr><td>be</td><td>ISO-8859-5</td></tr>
     *   <tr><td>bg</td><td>ISO-8859-5</td></tr>
     *   <tr><td>ca</td><td>ISO-8859-1</td></tr>
     *   <tr><td>cs</td><td>ISO-8859-2</td></tr>
     *   <tr><td>da</td><td>ISO-8859-1</td></tr>
     *   <tr><td>de</td><td>ISO-8859-1</td></tr>
     *   <tr><td>el</td><td>ISO-8859-7</td></tr>
     *   <tr><td>en</td><td>ISO-8859-1</td></tr>
     *   <tr><td>es</td><td>ISO-8859-1</td></tr>
     *   <tr><td>et</td><td>ISO-8859-1</td></tr>
     *   <tr><td>fi</td><td>ISO-8859-1</td></tr>
     *   <tr><td>fr</td><td>ISO-8859-1</td></tr>
     *   <tr><td>hr</td><td>ISO-8859-2</td></tr>
     *   <tr><td>hu</td><td>ISO-8859-2</td></tr>
     *   <tr><td>is</td><td>ISO-8859-1</td></tr>
     *   <tr><td>it</td><td>ISO-8859-1</td></tr>
     *   <tr><td>iw</td><td>ISO-8859-8</td></tr>
     *   <tr><td>ja</td><td>Shift_JIS</td></tr>
     *   <tr><td>ko</td><td>EUC-KR</td></tr>    
     *   <tr><td>lt</td><td>ISO-8859-2</td></tr>
     *   <tr><td>lv</td><td>ISO-8859-2</td></tr>
     *   <tr><td>mk</td><td>ISO-8859-5</td></tr>
     *   <tr><td>nl</td><td>ISO-8859-1</td></tr>
     *   <tr><td>no</td><td>ISO-8859-1</td></tr>
     *   <tr><td>pl</td><td>ISO-8859-2</td></tr>
     *   <tr><td>pt</td><td>ISO-8859-1</td></tr>
     *   <tr><td>ro</td><td>ISO-8859-2</td></tr>
     *   <tr><td>ru</td><td>ISO-8859-5</td></tr>
     *   <tr><td>sh</td><td>ISO-8859-5</td></tr>
     *   <tr><td>sk</td><td>ISO-8859-2</td></tr>
     *   <tr><td>sl</td><td>ISO-8859-2</td></tr>
     *   <tr><td>sq</td><td>ISO-8859-2</td></tr>
     *   <tr><td>sr</td><td>ISO-8859-5</td></tr>
     *   <tr><td>sv</td><td>ISO-8859-1</td></tr>
     *   <tr><td>tr</td><td>ISO-8859-9</td></tr>
     *   <tr><td>uk</td><td>ISO-8859-5</td></tr>
     *   <tr><td>zh</td><td>GB2312</td></tr>
     *   <tr><td>zh_TW</td><td>Big5</td></tr>
     * </table>
     * @see #clearEncodingMap
     * @see #setEncoding
     */
    public void loadBuiltInEncodingMap() {
        encodingMap.clear();
        encodingMap.put("ar", "ISO-8859-6");
        encodingMap.put("be", "ISO-8859-5");
        encodingMap.put("bg", "ISO-8859-5");
        encodingMap.put("ca", "ISO-8859-1");
        encodingMap.put("cs", "ISO-8859-2");
        encodingMap.put("da", "ISO-8859-1");
        encodingMap.put("de", "ISO-8859-1");
        encodingMap.put("el", "ISO-8859-7");
        encodingMap.put("en", "ISO-8859-1");
        encodingMap.put("es", "ISO-8859-1");
        encodingMap.put("et", "ISO-8859-1");
        encodingMap.put("fi", "ISO-8859-1");
        encodingMap.put("fr", "ISO-8859-1");
        encodingMap.put("hr", "ISO-8859-2");
        encodingMap.put("hu", "ISO-8859-2");
        encodingMap.put("is", "ISO-8859-1");
        encodingMap.put("it", "ISO-8859-1");
        encodingMap.put("iw", "ISO-8859-8");
        encodingMap.put("ja", "Shift_JIS");
        encodingMap.put("ko", "EUC-KR");    
        encodingMap.put("lt", "ISO-8859-2");
        encodingMap.put("lv", "ISO-8859-2");
        encodingMap.put("mk", "ISO-8859-5");
        encodingMap.put("nl", "ISO-8859-1");
        encodingMap.put("no", "ISO-8859-1");
        encodingMap.put("pl", "ISO-8859-2");
        encodingMap.put("pt", "ISO-8859-1");
        encodingMap.put("ro", "ISO-8859-2");
        encodingMap.put("ru", "ISO-8859-5");
        encodingMap.put("sh", "ISO-8859-5");
        encodingMap.put("sk", "ISO-8859-2");
        encodingMap.put("sl", "ISO-8859-2");
        encodingMap.put("sq", "ISO-8859-2");
        encodingMap.put("sr", "ISO-8859-5");
        encodingMap.put("sv", "ISO-8859-1");
        encodingMap.put("tr", "ISO-8859-9");
        encodingMap.put("uk", "ISO-8859-5");
        encodingMap.put("zh", "GB2312");
        encodingMap.put("zh_TW", "Big5");
    }

    /**
     * Clears language-to-encoding map.
     * @see #loadBuiltInEncodingMap
     * @see #setEncoding
     */
    public void clearEncodingMap() {
        encodingMap.clear();
    }

    /**
     * Returns the default (singleton) Configuration object. Note that you can
     * create as many separate configurations as you wish; this global instance
     * is provided for convenience, or when you have no reason to use a separate
     * instance.
     * 
     * @deprecated The usage of the static singleton (the "default")
     * {@link Configuration} instance can easily cause erroneous, unpredictable
     * behavior. This is because multiple independent software components may use
     * FreeMarker internally inside the same application, so they will interfere
     * because of the common {@link Configuration} instance. Each such component
     * should use its own private {@link Configuration} object instead, that it
     * typically creates with <code>new Configuration()</code> when the component
     * is initialized.
     */
    static public Configuration getDefaultConfiguration() {
        return defaultConfig;
    }

    /**
     * Sets the Configuration object that will be retrieved from future calls
     * to {@link #getDefaultConfiguration()}.
     * 
     * @deprecated Using the "default" {@link Configuration} instance can
     * easily lead to erroneous, unpredictable behaviour.
     * See more {@link Configuration#getDefaultConfiguration() here...}.
     */
    static public void setDefaultConfiguration(Configuration config) {
        defaultConfig = config;
    }
    
    /**
     * Sets a template loader that is used to look up and load templates.
     * By providing your own template loader, you can customize the way
     * templates are loaded. Several convenience methods in this class already
     * allow you to install commonly used loaders:
     * {@link #setClassForTemplateLoading(Class, String)}, 
     * {@link #setDirectoryForTemplateLoading(File)}, and
     * {@link #setServletContextForTemplateLoading(Object, String)}. By default,
     * a multi-loader is used that first tries to load a template from the file
     * in the current directory, then from a resource on the classpath.
     */
    public synchronized void setTemplateLoader(TemplateLoader loader) {
        createTemplateCache(loader, cache.getCacheStorage());
    }

    private void createTemplateCache(TemplateLoader loader, CacheStorage storage)
    {
        TemplateCache oldCache = cache;
        cache = new TemplateCache(loader, storage);
        cache.setDelay(oldCache.getDelay());
        cache.setConfiguration(this);
        cache.setLocalizedLookup(localizedLookup);
    }
    /**
     * @return the template loader that is used to look up and load templates.
     * @see #setTemplateLoader
     */
    public TemplateLoader getTemplateLoader()
    {
        return cache.getTemplateLoader();
    }

    /**
     * Sets the {@link CacheStorage} used for caching {@link Template}-s. The
     * default is a {@link SoftCacheStorage}. If the total size of the {@link Template}
     * objects is significant but most templates are used rarely, using a
     * {@link MruCacheStorage} instead might be advisable. If you don't want caching at
     * all, use {@link freemarker.cache.NullCacheStorage} (you can't use <tt>null</tt>).
     */
    public synchronized void setCacheStorage(CacheStorage storage) {
        createTemplateCache(cache.getTemplateLoader(), storage);
    }
    
    /**
     * Set the explicit directory from which to load templates.
     */
    public void setDirectoryForTemplateLoading(File dir) throws IOException {
        TemplateLoader tl = getTemplateLoader();
        if (tl instanceof FileTemplateLoader) {
            String path = ((FileTemplateLoader) tl).baseDir.getCanonicalPath();
            if (path.equals(dir.getCanonicalPath()))
                return;
        }
        setTemplateLoader(new FileTemplateLoader(dir));
    }

    /**
     * Sets the servlet context from which to load templates
     * @param sctxt the ServletContext object. Note that the type is <code>Object</code>
     *        to prevent class loading errors when user who uses FreeMarker not for
     *        servlets has no javax.servlet in the CLASSPATH.
     * @param path the path relative to the ServletContext.
     * If this path is absolute, it is taken to be relative
     * to the server's URL, i.e. http://myserver.com/
     * and if the path is relative, it is taken to be 
     * relative to the web app context, i.e.
     * http://myserver.context.com/mywebappcontext/
     */
    public void setServletContextForTemplateLoading(Object sctxt, String path) {
        try {
            if (path == null) {
                setTemplateLoader( (TemplateLoader)
                        ClassUtil.forName("freemarker.cache.WebappTemplateLoader")
                            .getConstructor(new Class[]{ClassUtil.forName("javax.servlet.ServletContext")})
                                    .newInstance(new Object[]{sctxt}) );
            }
            else {
                setTemplateLoader( (TemplateLoader)
                        ClassUtil.forName("freemarker.cache.WebappTemplateLoader")
                            .getConstructor(new Class[]{ClassUtil.forName("javax.servlet.ServletContext"), String.class})
                                    .newInstance(new Object[]{sctxt, path}) );
            }
        } catch (Exception exc) {
            throw new RuntimeException("Internal FreeMarker error: " + exc);
        }
    }

    /**
     * Sets a class relative to which we do the 
     * Class.getResource() call to load templates.
     */
    public void setClassForTemplateLoading(Class clazz, String pathPrefix) {
        setTemplateLoader(new ClassTemplateLoader(clazz, pathPrefix));
    }

    /**
     * Set the time in seconds that must elapse before checking whether there is a newer
     * version of a template file.
     * This method is thread-safe and can be called while the engine works.
     */
    public void setTemplateUpdateDelay(int delay) {
        cache.setDelay(1000L * delay);
    }

    /**
     * Sets whether directives such as if, else, etcetera
     * must be written as #if, #else, etcetera.
     * Any tag not starting with &lt;# or &lt;/# is considered as plain text
     * and will go to the output as is. Tag starting with &lt# or &lt/# must
     * be valid FTL tag, or else the template is invalid (i.e. &lt;#noSuchDirective>
     * is an error).
     */

    public void setStrictSyntaxMode(boolean b) {
        strictSyntax = b;
    }

    /**
     * Tells whether directives such as if, else, etcetera
     * must be written as #if, #else, etcetera.
     *
     * @see #setStrictSyntaxMode
     */
    public boolean getStrictSyntaxMode() {
        return strictSyntax;
    }

    /**
     * Was used for setting whether the parser bugs of 2.3 should be emulated for backward
     * compatibility; only <code>false</code> is supported now.
     */
    public void setEmulate23ParserBugs(boolean b) {
        if (!b) throw new IllegalArgumentException(
                "emulate23ParserBugs == true is not supported in FreeMarker 2.4.0 and later.");
    }

    public boolean getEmulate23ParserBugs() {
        return false;
    }    
    
    /**
     * Sets whether the FTL parser will try to remove
     * superfluous white-space around certain FTL tags.
     */
    public void setWhitespaceStripping(boolean b) {
        whitespaceStripping = b;
    }

    /**
     * Gets whether the FTL parser will try to remove
     * superfluous white-space around certain FTL tags.
     *
     * @see #setWhitespaceStripping
     */
    public boolean getWhitespaceStripping() {
        return whitespaceStripping;
    }
    
    /**
     * Determines the syntax of the template files (angle bracket VS square bracket)
     * that has no <markup>ftl</markup> directive in it. The <code>tagSyntax</code>
     * parameter must be one of:
     * <ul>
     *   <li>{@link Configuration#AUTO_DETECT_TAG_SYNTAX}:
     *     use the syntax of the first FreeMarker tag (can be anything, like <tt>list</tt>,
     *     <tt>include</tt>, user defined, ...etc)
     *   <li>{@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}:
     *     use the angle bracket syntax (the normal syntax)
     *   <li>{@link Configuration#SQUARE_BRACKET_TAG_SYNTAX}:
     *     use the square bracket syntax
     * </ul>
     *
     * <p>In FreeMarker 2.3.x {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX} is the
     * default for better backward compatibility. Starting from 2.4.x {@link
     * Configuration#AUTO_DETECT_TAG_SYNTAX} is the default, so it is recommended to use
     * that even for 2.3.x.
     * 
     * <p>This setting is ignored for the templates that have <tt>ftl</tt> directive in
     * it. For those templates the syntax used for the <tt>ftl</tt> directive determines
     * the syntax.
     */
    public void setTagSyntax(int tagSyntax) {
        if (tagSyntax != AUTO_DETECT_TAG_SYNTAX
            && tagSyntax != SQUARE_BRACKET_TAG_SYNTAX
            && tagSyntax != ANGLE_BRACKET_TAG_SYNTAX)
        {
            throw new IllegalArgumentException("This can only be set to one of three settings: Configuration.AUTO_DETECT_TAG_SYNTAX, Configuration.ANGLE_BRACKET_SYNTAX, or Configuration.SQAUARE_BRACKET_SYNTAX");
        }
        this.tagSyntax = tagSyntax;
    }
    
    /**
     * See {@link #setTagSyntax(int)} to see the returned number.
     */
    public int getTagSyntax() {
        return tagSyntax;
    }
    
    /**
     * Equivalent to <tt>getTemplate(name, thisCfg.getLocale(), thisCfg.getEncoding(thisCfg.getLocale()), true)</tt>.
     */
    public Template getTemplate(String name) throws IOException {
        Locale loc = getLocale();
        return getTemplate(name, loc, getEncoding(loc), true);
    }

    /**
     * Equivalent to <tt>getTemplate(name, locale, thisCfg.getEncoding(locale), true)</tt>.
     */
    public Template getTemplate(String name, Locale locale) throws IOException {
        return getTemplate(name, locale, getEncoding(locale), true);
    }

    /**
     * Equivalent to <tt>getTemplate(name, thisCfg.getLocale(), encoding, true)</tt>.
     */
    public Template getTemplate(String name, String encoding) throws IOException {
        return getTemplate(name, getLocale(), encoding, true);
    }

    /**
     * Equivalent to <tt>getTemplate(name, locale, encoding, true)</tt>.
     */
    public Template getTemplate(String name, Locale locale, String encoding) throws IOException {
        return getTemplate(name, locale, encoding, true);
    }

    /**
     * Retrieves a template specified by a name and locale, interpreted using
     * the specified character encoding, either parsed or unparsed. For the
     * exact semantics of parameters, see 
     * {@link TemplateCache#getTemplate(String, Locale, String, boolean)}.
     * @return the requested template.
     * @throws FileNotFoundException if the template could not be found.
     * @throws IOException if there was a problem loading the template.
     * @throws ParseException (extends <code>IOException</code>) if the template is syntactically bad.
     */
    public Template getTemplate(String name, Locale locale, String encoding, boolean parse) throws IOException {
        Template result = cache.getTemplate(name, locale, encoding, parse);
        if (result == null) {
            throw new FileNotFoundException("Template " + name + " not found.");
        }
        return result;
    }

    /**
     * Sets the default encoding for converting bytes to characters when
     * reading template files in a locale for which no explicit encoding
     * was specified. Defaults to default system encoding.
     */
    public void setDefaultEncoding(String encoding) {
        defaultEncoding = encoding;
    }

    /**
     * Gets the default encoding for converting bytes to characters when
     * reading template files in a locale for which no explicit encoding
     * was specified. Defaults to default system encoding.
     */
    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Gets the preferred character encoding for the given locale, or the 
     * default encoding if no encoding is set explicitly for the specified
     * locale. You can associate encodings with locales using 
     * {@link #setEncoding(Locale, String)} or {@link #loadBuiltInEncodingMap()}.
     * @param loc the locale
     * @return the preferred character encoding for the locale.
     */
    public String getEncoding(Locale loc) {
        // Try for a full name match (may include country and variant)
        String charset = (String) encodingMap.get(loc.toString());
        if (charset == null) {
            if (loc.getVariant().length() > 0) {
                Locale l = new Locale(loc.getLanguage(), loc.getCountry());
                charset = (String) encodingMap.get(l.toString());
                if (charset != null) {
                    encodingMap.put(loc.toString(), charset);
                }
            } 
            charset = (String) encodingMap.get(loc.getLanguage());
            if (charset != null) {
                encodingMap.put(loc.toString(), charset);
            }
        }
        return charset != null ? charset : defaultEncoding;
    }

    /**
     * Sets the character set encoding to use for templates of
     * a given locale. If there is no explicit encoding set for some
     * locale, then the default encoding will be used, what you can
     * set with {@link #setDefaultEncoding}.
     *
     * @see #clearEncodingMap
     * @see #loadBuiltInEncodingMap
     */
    public void setEncoding(Locale locale, String encoding) {
        encodingMap.put(locale.toString(), encoding);
    }

    /**
     * Adds a shared variable to the configuration.
     * Shared variables are variables that are visible
     * as top-level variables for all templates which use this
     * configuration, if the data model does not contain a
     * variable with the same name.
     *
     * <p>Never use <tt>TemplateModel</tt> implementation that is not thread-safe for shared variables,
     * if the configuration is used by multiple threads! It is the typical situation for Servlet based Web sites.
     *
     * @param name the name used to access the data object from your template.
     *     If a shared variable with this name already exists, it will replace
     *     that.
     * @see #setSharedVariable(String,Object)
     * @see #setAllSharedVariables
     */
    public void setSharedVariable(String name, TemplateModel tm) {
        variables.put(name, tm);
    }

    /**
     * Returns the set containing the names of all defined shared variables.
     * The method returns a new Set object on each call that is completely
     * disconnected from the Configuration. That is, modifying the set will have
     * no effect on the Configuration object.
     */
    public Set getSharedVariableNames() {
        return new HashSet(variables.keySet());
    }
    
    /**
     * Adds shared variable to the configuration.
     * It uses {@link Configurable#getObjectWrapper()} to wrap the 
     * <code>obj</code>.
     * @see #setSharedVariable(String,TemplateModel)
     * @see #setAllSharedVariables
     */
    public void setSharedVariable(String name, Object obj) throws TemplateModelException {
        setSharedVariable(name, getObjectWrapper().wrap(obj));
    }

    /**
     * Adds all object in the hash as shared variable to the configuration.
     *
     * <p>Never use <tt>TemplateModel</tt> implementation that is not thread-safe for shared variables,
     * if the configuration is used by multiple threads! It is the typical situation for Servlet based Web sites.
     *
     * @param hash a hash model whose objects will be copied to the
     * configuration with same names as they are given in the hash.
     * If a shared variable with these names already exist, it will be replaced
     * with those from the map.
     *
     * @see #setSharedVariable(String,Object)
     * @see #setSharedVariable(String,TemplateModel)
     */
    public void setAllSharedVariables(TemplateHashModelEx hash) throws TemplateModelException {
        TemplateModelIterator keys = hash.keys().iterator();
        TemplateModelIterator values = hash.values().iterator();
        while(keys.hasNext())
        {
            setSharedVariable(((TemplateScalarModel)keys.next()).getAsString(), values.next());
        }
    }
    
    /**
     * Gets a shared variable. Shared variables are variables that are 
     * available to all templates. When a template is processed, and an identifier
     * is undefined in the data model, a shared variable object with the same identifier
     * is then looked up in the configuration. There are several predefined variables
     * that are always available through this method, see the FreeMarker manual
     * for a comprehensive list of them.
     *
     * @see #setSharedVariable(String,Object)
     * @see #setSharedVariable(String,TemplateModel)
     * @see #setAllSharedVariables
     */
    public TemplateModel getSharedVariable(String name) {
        return (TemplateModel) variables.get(name);
    }
    
    /**
     * Removes all shared variables, except the predefined ones (compress, html_escape, etc.).
     */
    public void clearSharedVariables() {
        variables.clear();
        loadBuiltInSharedVariables();
    }
    
    /**
     * Removes all entries from the template cache, thus forcing reloading of templates
     * on subsequent <code>getTemplate</code> calls.
     * This method is thread-safe and can be called while the engine works.
     */
    public void clearTemplateCache() {
        cache.clear();
    }
    
    /**
     * Returns if localized template lookup is enabled or not.
     * This method is thread-safe and can be called while the engine works.
     */
    public boolean getLocalizedLookup() {
        return cache.getLocalizedLookup();
    }
    
    /**
     * Enables/disables localized template lookup. Enabled by default.
     * This method is thread-safe and can be called while the engine works.
     */
    public void setLocalizedLookup(boolean localizedLookup) {
        this.localizedLookup = localizedLookup;
        cache.setLocalizedLookup(localizedLookup);
    }
    
    /**
     * Sets a setting by name and string value.
     *
     * In additional to the settings understood by
     * {@link Configurable#setSetting the super method}, it understands these:
     * <ul>
     *   <li><code>"auto_import"</code>: Sets the list of auto-imports. Example of valid value:
     *       <br><code>/lib/form.ftl as f, /lib/widget as w, "/lib/evil name.ftl" as odd</code>
     *       See: {@link #setAutoImports}
     *   <li><code>"auto_include"</code>: Sets the list of auto-includes. Example of valid value:
     *       <br><code>/include/common.ftl, "/include/evil name.ftl"</code>
     *       See: {@link #setAutoIncludes}
     *   <li><code>"default_encoding"</code>: The name of the charset, such as <code>"UTF-8"</code>.
     *       See: {@link #setDefaultEncoding}
     *   <li><code>"localized_lookup"</code>:
     *       <code>"true"</code>, <code>"false"</code>, <code>"yes"</code>, <code>"no"</code>,
     *       <code>"t"</code>, <code>"f"</code>, <code>"y"</code>, <code>"n"</code>.
     *       Case insensitive.
     *      See: {@link #setLocalizedLookup}
     *   <li><code>"strict_syntax"</code>: <code>"true"</code>, <code>"false"</code>, etc.
     *       See: {@link #setStrictSyntaxMode}
     *   <li><code>"whitespace_stripping"</code>: <code>"true"</code>, <code>"false"</code>, etc.
     *       See: {@link #setWhitespaceStripping}
     *   <li><code>"cache_storage"</code>: If the value contains dot, then it is
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then a {@link freemarker.cache.MruCacheStorage} will be used with the
     *       maximum strong and soft sizes specified with the setting value. Examples
     *       of valid setting values:
     *       <table border=1 cellpadding=4>
     *         <tr><th>Setting value<th>max. strong size<th>max. soft size
     *         <tr><td><code>"strong:50, soft:500"</code><td>50<td>500
     *         <tr><td><code>"strong:100, soft"</code><td>100<td><code>Integer.MAX_VALUE</code>
     *         <tr><td><code>"strong:100"</code><td>100<td>0
     *         <tr><td><code>"soft:100"</code><td>0<td>100
     *         <tr><td><code>"strong"</code><td><code>Integer.MAX_VALUE</code><td>0
     *         <tr><td><code>"soft"</code><td>0<td><code>Integer.MAX_VALUE</code>
     *       </table>
     *       The value is not case sensitive. The order of <tt>soft</tt> and <tt>strong</tt>
     *       entries is not significant.
     *       For more details see: {@link #setCacheStorage}
     *   <li><code>"template_update_delay"</code>: Valid positive integer, the
     *       update delay measured in seconds.
     *       See: {@link #setTemplateUpdateDelay}
     *   <li><code>"tag_syntax"</code>: Must be one of:
     *       <code>"auto_detect"</code>, <code>"angle_bracket"</code>,
     *       <code>"square_bracket"</code>.
     *   <li><code>"emulate_23_parser_bugs"</code>: must be <code>"false"</code> starting from
     *       FreeMarker 2.4.0.
     * </ul>
     *
     * @param key the name of the setting.
     * @param value the string that describes the new value of the setting.
     *
     * @throws UnknownSettingException if the key is wrong.
     * @throws TemplateException if the new value of the setting can't be set
     *     for any other reasons.
     */
    public void setSetting(String key, String value) throws TemplateException {
        if ("TemplateUpdateInterval".equalsIgnoreCase(key)) {
            key = TEMPLATE_UPDATE_DELAY_KEY;
        } else if ("DefaultEncoding".equalsIgnoreCase(key)) {
            key = DEFAULT_ENCODING_KEY;
        }
        boolean callSuper = false;
        try {
            if (DEFAULT_ENCODING_KEY.equals(key)) {
                setDefaultEncoding(value);
            } else if (LOCALIZED_LOOKUP_KEY.equals(key)) {
                setLocalizedLookup(StringUtil.getYesNo(value));
            } else if (STRICT_SYNTAX_KEY.equals(key)) {
                setStrictSyntaxMode(StringUtil.getYesNo(value));
            } else if (WHITESPACE_STRIPPING_KEY.equals(key)) {
                setWhitespaceStripping(StringUtil.getYesNo(value));
            } else if (CACHE_STORAGE_KEY.equals(key)) {
                if (value.indexOf('.') == -1) {
                    int strongSize = 0;
                    int softSize = 0;
                    Map map = StringUtil.parseNameValuePairList(
                            value, String.valueOf(Integer.MAX_VALUE));
                    Iterator it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry ent = (Map.Entry) it.next();
                        String pname = (String) ent.getKey();
                        int pvalue;
                        try {
                            pvalue = Integer.parseInt((String) ent.getValue());
                        } catch (NumberFormatException e) {
                            throw invalidSettingValueException(key, value);
                        }
                        if ("soft".equalsIgnoreCase(pname)) {
                            softSize = pvalue;
                        } else if ("strong".equalsIgnoreCase(pname)) {
                            strongSize = pvalue;
                        } else {
                            throw invalidSettingValueException(key, value);
                        }
                    }
                    if (softSize == 0 && strongSize == 0) {
                        throw invalidSettingValueException(key, value);
                    }
                    setCacheStorage(new MruCacheStorage(strongSize, softSize));
                } else {
                    setCacheStorage((CacheStorage) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (TEMPLATE_UPDATE_DELAY_KEY.equals(key)) {
                setTemplateUpdateDelay(Integer.parseInt(value));
            } else if (AUTO_INCLUDE_KEY.equals(key)) {
                setAutoIncludes(parseAsList(value));
            } else if (AUTO_IMPORT_KEY.equals(key)) {
                setAutoImports(parseAsImportList(value));
            } else if (TAG_SYNTAX_KEY.equals(key)) {
                if ("auto_detect".equals(value)) {
                    setTagSyntax(AUTO_DETECT_TAG_SYNTAX);
                } else if ("angle_bracket".equals(value)) {
                    setTagSyntax(ANGLE_BRACKET_TAG_SYNTAX);
                } else if ("square_bracket".equals(value)) {
                    setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
                } else {
                    throw invalidSettingValueException(key, value);
                }
            } else if (EMULATE_2_3_PARSER_BUGS.equals(key)) {
                setEmulate23ParserBugs(StringUtil.getYesNo(value));
            } else {
                callSuper = true;
            }
        } catch(Exception e) {
            throw new TemplateException(
                    "Failed to set setting " + 
                    StringUtil.jQuote(key) + " to value " + StringUtil.jQuote(value) +
                    "; see cause exception.",
                    e, getEnvironment());
        }
        if (callSuper) {
            super.setSetting(key, value);
        }
    }
    
    
    /**
     * Add an auto-imported template.
     * The importing will happen at the top of any template that
     * is vended by this Configuration object.
     * @param namespace the name of the namespace into which the template is imported
     * @param template the name of the template
     */
    public synchronized void addAutoImport(String namespace, String template) {
        autoImports.remove(namespace);
        autoImports.add(namespace);
        autoImportMap.put(namespace, template);
    }
    
    /**
     * Remove an auto-imported template
     * @param namespace the name of the namespace into which the template was imported
     */
    
    public synchronized void removeAutoImport(String namespace) {
        autoImports.remove(namespace);
        autoImportMap.remove(namespace);
    }
    
    /**
     * set a map of namespace names to templates for auto-importing 
     * a set of templates. Note that all previous auto-imports are removed.
     */
    
    public synchronized void setAutoImports(Map map) {
        autoImports = new ArrayList(map.keySet());
        if (map instanceof HashMap) {
            autoImportMap = (Map) ((HashMap) map).clone();
        } 
        else if (map instanceof SortedMap) {
            autoImportMap = new TreeMap(map);             
        }
        else {
            autoImportMap = new HashMap(map);
        }
    }
    
    protected void doAutoImportsAndIncludes(Environment env)
    throws TemplateException, IOException
    {
        for (int i=0; i<autoImports.size(); i++) {
            String namespace = (String) autoImports.get(i);
            String templateName = (String) autoImportMap.get(namespace);
            env.importLib(templateName, namespace);
        }
        for (int i = 0; i < autoIncludes.size(); i++) {
            String templateName = (String) autoIncludes.get(i);
            Template template = getTemplate(templateName, env.getLocale());
            env.include(template);
        }
    }
    
    /**
     * add a template to be automatically included at the top of any template that
     * is vended by this Configuration object.
     * @param templateName the lookup name of the template.
     */
     
    public synchronized void addAutoInclude(String templateName) {
        autoIncludes.remove(templateName);
        autoIncludes.add(templateName);
    }

    /**
     * set the list of automatically included templates.
     * Note that all previous auto-includes are removed.
     */
    public synchronized void setAutoIncludes(List templateNames) {
        autoIncludes.clear();
        Iterator it = templateNames.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("List items must be String-s.");
            }
            autoIncludes.add(o);
        }
    }
    
    /**
     * remove a template from the auto-include list.
     * @param templateName the lookup name of the template in question.
     */
     
    public synchronized void removeAutoInclude(String templateName) {
        autoIncludes.remove(templateName);
    }

    /**
     * Returns FreeMarker version number string. 
     * Examples of possible return values:
     * <code>"2.2.5"</code>, <code>"2.3pre13"</code>,
     * <code>"2.3pre13mod"</code>, <code>"2.3rc1"</code>, <code>"2.3"</code>,
     * <code>"3.0"</code>.
     *
     * <p>Notes on FreeMarker version numbering rules:
     * <ul>
     *   <li>"pre" and "rc" (lowercase!) means "preview" and "release
     *       candidate" respectively. It is must be followed with a
     *       number (as "1" for the first release candidate).
     *   <li>The "mod" after the version number indicates that it's an
     *       unreleased modified version of the released version.
     *       After releases, the nighly builds are such releases. E.g.
     *       the nightly build after releasing "2.2.1" but before releasing
     *       "2.2.2" is "2.2.1mod".
     *   <li>The 2nd version number must be present, and maybe 0,
     *       as in "3.0".
     *   <li>The 3rd version number is never 0. E.g. the version
     *       number string for the first release of the 2.2 series
     *       is "2.2", and NOT "2.2.0". 
     *   <li>When only the 3rd version number increases
     *       (2.2 -> 2.2.1, 2.2.1 -> 2.2.2 etc.), 100% backward compatiblity
     *       with the previous version MUST be kept.
     *       This means that <tt>freemarker.jar</tt> can be replaced in an
     *       application without risk (as far as the application doesn't depend
     *       on the presence of a FreeMarker bug).
     *       Note that backward compatibility restrictions do not apply for
     *       preview releases.
     * </ul>
     */
    public static String getVersionNumber() {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        try {
            Properties vp = new Properties();
            InputStream ins = Configuration.class.getClassLoader()
                    .getResourceAsStream("freemarker/version.properties");
            if (ins == null) {
                throw new RuntimeException("Version file is missing.");
            } else {
                try {
                    vp.load(ins);
                } finally {
                    ins.close();
                }
                String v = vp.getProperty("version");
                if (v == null) {
                    throw new RuntimeException("Version file is corrupt: version key is missing.");
                }
                cachedVersion = v;
            }
            return cachedVersion;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load version file: " + e);
        }
    }
}
