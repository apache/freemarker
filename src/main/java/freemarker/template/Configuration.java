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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import freemarker.cache.CacheStorage;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MruCacheStorage;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.SoftCacheStorage;
import freemarker.cache.TemplateCache;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.core._ConcurrentMapFactory;
import freemarker.core._CoreAPI;
import freemarker.core._DelayedJQuote;
import freemarker.core._MiscTemplateException;
import freemarker.template.utility.CaptureOutput;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.NormalizeNewlines;
import freemarker.template.utility.SecurityUtilities;
import freemarker.template.utility.StandardCompress;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.XmlEscape;

/**
 * <b>The main entry point into the FreeMarker API</b>; encapsulates the configuration settings of FreeMarker,
 * also serves as a central template-loading and caching service.
 *
 * <p>This class is meant to be used in a singleton pattern. That is, you create an instance of this at the beginning of
 * the application life-cycle, set its {@link #setSetting(String, String) configuration settings} there (either with the
 * setter methods or by loading a {@code .properties} file), and then use that single instance everywhere in your
 * application. Frequently re-creating {@link Configuration} is a typical and grave mistake from performance standpoint,
 * as the {@link Configuration} holds the template cache, and often also the class introspection cache, which then will
 * be lost. (Note that, naturally, having multiple long-lived instances, like one per component that internally uses
 * FreeMarker is fine.)  
 * 
 * <p>The basic usage pattern is like:
 * 
 * <pre>
 *  // Where the application is initialized; in general you do this ONLY ONCE in the application life-cycle!
 *  Configuration cfg = new Configuration();
 *  cfg.set<i>SomeSetting</i>(...);
 *  cfg.set<i>OtherSetting</i>(...);
 *  ...
 *  
 *  // Later, whenever the application needs a template (so you may do this a lot, and from multiple threads):
 *  {@link Template Template} myTemplate = cfg.{@link #getTemplate(String) getTemplate}("myTemplate.html");
 *  myTemplate.{@link Template#process(Object, java.io.Writer) process}(dataModel, out);</pre>
 * 
 * <p>A couple of settings that you should not leave on its default value are:
 * <ul>
 *   <li>{@link #setTemplateLoader(TemplateLoader) template_loader}: The default value is deprecated and in fact quite
 *       useless. (Most user can use the convenience methods {@link #setDirectoryForTemplateLoading(File)},
 *       {@link #setClassForTemplateLoading(Class, String)} too.)
 *   <li>{@link #setDefaultEncoding(String) default_encoding}: The default value is system dependent, which makes it
 *       fragile on servers, so it should be set explicitly, like to "UTF-8" nowadays. 
 *   <li>{@link #setIncompatibleImprovements(Version) incompatible_improvements}: As far the 1st and 2nd version number
 *       remains, it's quite safe to set it as high as possible, so for new or actively developed products it's
 *       recommended to do.
 *   <li>{@link #setTemplateExceptionHandler(TemplateExceptionHandler) template_exception_handler}: For developing
 *       HTML pages, the most convenient value is {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}. For production,
 *       {@link TemplateExceptionHandler#RETHROW_HANDLER} is safer to use.
 *   <!-- 2.4: recommend the new object wrapper here -->
 * </ul>
 * 
 * <p>A {@link Configuration} object is thread-safe only after you have stopped modify the configuration settings.
 * Generally, you set everything directly after you have instantiated the {@link Configuration} object, then you don't
 * change the settings anymore, so then it's safe to make it accessible from multiple threads.
 *
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @author Attila Szegedi
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
    public static final String INCOMPATIBLE_IMPROVEMENTS = "incompatible_improvements";
    /** @deprecated Use {@link #INCOMPATIBLE_IMPROVEMENTS} instead. */
    public static final String INCOMPATIBLE_ENHANCEMENTS = "incompatible_enhancements";
    public static final int AUTO_DETECT_TAG_SYNTAX = 0;
    public static final int ANGLE_BRACKET_TAG_SYNTAX = 1;
    public static final int SQUARE_BRACKET_TAG_SYNTAX = 2;
    
    /** The default of {@link #getIncompatibleImprovements()}, currently {@code new Version(2, 3, 0)}. */
    public static final Version DEFAULT_INCOMPATIBLE_IMPROVEMENTS = new Version(2, 3, 0);
    /** @deprecated Use {@link #DEFAULT_INCOMPATIBLE_IMPROVEMENTS} instead. */
    public static final String DEFAULT_INCOMPATIBLE_ENHANCEMENTS = DEFAULT_INCOMPATIBLE_IMPROVEMENTS.toString();
    /** @deprecated Use {@link #DEFAULT_INCOMPATIBLE_IMPROVEMENTS} instead. */
    public static final int PARSED_DEFAULT_INCOMPATIBLE_ENHANCEMENTS = DEFAULT_INCOMPATIBLE_IMPROVEMENTS.intValue(); 
    
    private static Configuration defaultConfig = new Configuration();
    
    private static boolean versionPropertiesLoaded;
    /** @deprecated Use {@link #version} instead. */
    private static String versionNumber;
    private static Version version;
    
    private boolean strictSyntax = true;
    private volatile boolean localizedLookup = true;
    private boolean whitespaceStripping = true;
    private Version incompatibleImprovements = DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
    private int tagSyntax = ANGLE_BRACKET_TAG_SYNTAX;

    private TemplateCache cache;
    
    private HashMap sharedVariables = new HashMap();
    
    private String defaultEncoding = SecurityUtilities.getSystemProperty("file.encoding");
    private Map localeToCharsetMap = _ConcurrentMapFactory.newThreadSafeMap();
    
    private ArrayList autoImports = new ArrayList(), autoIncludes = new ArrayList(); 
    private Map autoImportNsToTmpMap = new HashMap();   // TODO No need for this, instead use List<NamespaceToTemplate> below.

    public Configuration() {
        cache = new TemplateCache();
        cache.setConfiguration(this);
        cache.setDelay(5000);
        loadBuiltInSharedVariables();
    }

    public Object clone() {
        try {
            Configuration copy = (Configuration)super.clone();
            copy.sharedVariables = new HashMap(sharedVariables);
            copy.localeToCharsetMap = new HashMap(localeToCharsetMap);
            copy.autoImportNsToTmpMap = new HashMap(autoImportNsToTmpMap);
            copy.autoImports = (ArrayList) autoImports.clone();
            copy.autoIncludes = (ArrayList) autoIncludes.clone();
            copy.createTemplateCache(cache.getTemplateLoader(), cache.getCacheStorage());
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone is not supported, but it should be: " + e.getMessage());
        }
    }
    
    private void loadBuiltInSharedVariables() {
        sharedVariables.put("capture_output", new CaptureOutput());
        sharedVariables.put("compress", StandardCompress.INSTANCE);
        sharedVariables.put("html_escape", new HtmlEscape());
        sharedVariables.put("normalize_newlines", new NormalizeNewlines());
        sharedVariables.put("xml_escape", new XmlEscape());
    }
    
    /**
     * Loads a preset language-to-encoding map. It assumes the usual character
     * encodings for most languages.
     * The previous content of the encoding map will be lost.
     * This default map currently contains the following mappings:
     * 
     * <table style="width: auto; border-collapse: collapse" border="1">
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
     * 
     * @see #clearEncodingMap()
     * @see #setEncoding(Locale, String)
     * @see #setDefaultEncoding(String)
     */
    public void loadBuiltInEncodingMap() {
        localeToCharsetMap.clear();
        localeToCharsetMap.put("ar", "ISO-8859-6");
        localeToCharsetMap.put("be", "ISO-8859-5");
        localeToCharsetMap.put("bg", "ISO-8859-5");
        localeToCharsetMap.put("ca", "ISO-8859-1");
        localeToCharsetMap.put("cs", "ISO-8859-2");
        localeToCharsetMap.put("da", "ISO-8859-1");
        localeToCharsetMap.put("de", "ISO-8859-1");
        localeToCharsetMap.put("el", "ISO-8859-7");
        localeToCharsetMap.put("en", "ISO-8859-1");
        localeToCharsetMap.put("es", "ISO-8859-1");
        localeToCharsetMap.put("et", "ISO-8859-1");
        localeToCharsetMap.put("fi", "ISO-8859-1");
        localeToCharsetMap.put("fr", "ISO-8859-1");
        localeToCharsetMap.put("hr", "ISO-8859-2");
        localeToCharsetMap.put("hu", "ISO-8859-2");
        localeToCharsetMap.put("is", "ISO-8859-1");
        localeToCharsetMap.put("it", "ISO-8859-1");
        localeToCharsetMap.put("iw", "ISO-8859-8");
        localeToCharsetMap.put("ja", "Shift_JIS");
        localeToCharsetMap.put("ko", "EUC-KR");    
        localeToCharsetMap.put("lt", "ISO-8859-2");
        localeToCharsetMap.put("lv", "ISO-8859-2");
        localeToCharsetMap.put("mk", "ISO-8859-5");
        localeToCharsetMap.put("nl", "ISO-8859-1");
        localeToCharsetMap.put("no", "ISO-8859-1");
        localeToCharsetMap.put("pl", "ISO-8859-2");
        localeToCharsetMap.put("pt", "ISO-8859-1");
        localeToCharsetMap.put("ro", "ISO-8859-2");
        localeToCharsetMap.put("ru", "ISO-8859-5");
        localeToCharsetMap.put("sh", "ISO-8859-5");
        localeToCharsetMap.put("sk", "ISO-8859-2");
        localeToCharsetMap.put("sl", "ISO-8859-2");
        localeToCharsetMap.put("sq", "ISO-8859-2");
        localeToCharsetMap.put("sr", "ISO-8859-5");
        localeToCharsetMap.put("sv", "ISO-8859-1");
        localeToCharsetMap.put("tr", "ISO-8859-9");
        localeToCharsetMap.put("uk", "ISO-8859-5");
        localeToCharsetMap.put("zh", "GB2312");
        localeToCharsetMap.put("zh_TW", "Big5");
    }

    /**
     * Clears language-to-encoding map.
     * @see #loadBuiltInEncodingMap
     * @see #setEncoding
     */
    public void clearEncodingMap() {
        localeToCharsetMap.clear();
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
     * Sets a {@link TemplateLoader} that is used to look up and load templates.
     * By providing your own {@link TemplateLoader} implementation, you can load templates from whatever kind of
     * storages, like from relational databases, NoSQL-storages, etc.
     * 
     * <p>Convenience methods exists to install commonly used loaders, instead of using this method:
     * {@link #setClassForTemplateLoading(Class, String)}, 
     * {@link #setDirectoryForTemplateLoading(File)}, and
     * {@link #setServletContextForTemplateLoading(Object, String)}.
     * 
     * <p>You can chain several {@link TemplateLoader}-s together with {@link MultiTemplateLoader}.
     * 
     * <p>Default value: You should always set the template loader instead of relying on the default value.
     * The a default value is there only for backward compatibility, and it will be probably
     * removed in the future. It's a multi-loader that first tries to load a
     * template from the file in the current directory, then from a resource on the classpath.
     * 
     * <p>Note that setting the template loader will re-create the template cache, so
     * all its content will be lost.
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
     * The getter pair of {@link #setTemplateLoader(TemplateLoader)}.
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
     * all, use {@link freemarker.cache.NullCacheStorage} (you can't use {@code null}).
     * 
     * <p>Note that setting the cache storage will re-create the template cache, so
     * all its content will be lost.
     */
    public synchronized void setCacheStorage(CacheStorage storage) {
        createTemplateCache(cache.getTemplateLoader(), storage);
    }
    
    /**
     * The getter pair of {@link #setCacheStorage(CacheStorage)}.
     * 
     * @since 2.3.20
     */
    public synchronized CacheStorage getCacheStorage() {
        return cache.getCacheStorage();
    }

    /**
     * Sets the file system directory from which to load templates.
     * This is equivalent to {@code setTemplateLoader(new FileTemplateLoader(dir))},
     * so see {@link FileTemplateLoader#FileTemplateLoader(File)} for more details.
     * 
     * Note that FreeMarker can load templates from non-file-system sources too. 
     * See {@link #setTemplateLoader(TemplateLoader)} from more details.
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
     * Sets the servlet context from which to load templates.
     * This is equivalent to {@code setTemplateLoader(new WebappTemplateLoader(sctxt, path))}
     * or {@code setTemplateLoader(new WebappTemplateLoader(sctxt))} if {@code path} was
     * {@code null}, so see {@link WebappTemplateLoader} for more details.
     * 
     * @param servletContext the {@link ServletContext} object. (The declared type is {@link Object}
     *        to prevent class loading error when using FreeMarker in an environment where
     *        there's no servlet classes available.)
     * @param path the path relative to the ServletContext.
     *
     * @see #setTemplateLoader(TemplateLoader)
     */
    public void setServletContextForTemplateLoading(Object servletContext, String path) {
        try {
            // Don't introduce linking-time dependency on servlets
            final Class webappTemplateLoaderClass = ClassUtil.forName("freemarker.cache.WebappTemplateLoader");
            
            // Don't introduce linking-time dependency on servlets
            final Class servletContextClass = ClassUtil.forName("javax.servlet.ServletContext");
            
            final Class[] constructorParamTypes;
            final Object[] constructorParams;
            if (path == null) {
                constructorParamTypes = new Class[] { servletContextClass };
                constructorParams = new Object[] { servletContext };
            } else {
                constructorParamTypes = new Class[] { servletContextClass, String.class };
                constructorParams = new Object[] { servletContext, path };
            }
            
            setTemplateLoader( (TemplateLoader)
                    webappTemplateLoaderClass
                            .getConstructor(constructorParamTypes)
                                    .newInstance(constructorParams));
        } catch (Exception exc) {
            throw new RuntimeException("Internal FreeMarker error: " + exc);
        }
    }

    /**
     * Sets a class relative to which we do the Class.getResource() call to load templates.
     * This is equivalent to {@code setTemplateLoader(new ClassTemplateLoader(clazz, pathPrefix))},
     * so see {@link ClassTemplateLoader#ClassTemplateLoader(Class, String)} for more details.
     * 
     * @see #setTemplateLoader(TemplateLoader)
     */
    public void setClassForTemplateLoading(Class clazz, String pathPrefix) {
        setTemplateLoader(new ClassTemplateLoader(clazz, pathPrefix));
    }

    /**
     * Sets the time in seconds that must elapse before checking whether there is a newer version of a template file
     * than the cached one.
     * This method is thread-safe and can be called while the engine works.
     */
    public void setTemplateUpdateDelay(int seconds) {
        cache.setDelay(1000L * seconds);
    }
    
    /**
     * Sets whether directives such as {@code if}, {@code else}, etc must be written as {@code #if}, {@code #else}, etc.
     * Defaults to {@code true}.
     * 
     * <p>When this is {@code true},
     * any tag not starting with &lt;# or &lt;/# or &lt;@ or &lt;/@ is considered as plain text
     * and will go to the output as is. Tag starting with &lt# or &lt/# must
     * be valid FTL tag, or else the template is invalid (i.e. &lt;#noSuchDirective>
     * is an error).
     * 
     * @deprecated Only {@code true} (the default) value will be supported sometimes in the future.
     */
    public void setStrictSyntaxMode(boolean b) {
        strictSyntax = b;
    }

    /**
     * The getter pair of {@link #setStrictSyntaxMode}.
     */
    public boolean getStrictSyntaxMode() {
        return strictSyntax;
    }

    /**
     * Sets which of the non-backward-compatible bugfixes/improvements should be enabled. The setting value is the
     * FreeMarker version number where the bugfixes/improvements to enable were already implemented (but wasn't
     * active by default, as that would break backward-compatibility).
     * 
     * <p>The default value is 2.3.0 for maximum backward-compatibility when upgrading {@code freemkarer.jar} under an
     * existing application. But if you develop a new application with, say, 2.3.20, it's probably a good idea to set
     * this from 2.3.0 to 2.3.20. As far as the 1st and 2nd version number remains, these changes are always very
     * low-risk changes, so usually they don't break anything in older applications either.
     * 
     * <p>This setting doesn't affect some important non-backward compatible security fixes; they are always
     * enabled, regardless of what you set here.
     * 
     * <p>Incrementing this setting is a good way of preparing for the next minor (2nd) or major (1st) version number
     * increases. When that happens, it's possible that some old behavior become unsupported, that is, even if you
     * set this setting to a low value, it might wont bring back the old behavior anymore.
     * 
     * <p>Currently the effects of this setting are:
     * <ul>
     *   <li><p>
     *     2.3.19 (or higher): Bug fix: Wrong {@code #} tags were printed as static text instead of
     *     causing parsing error when there was no correct {@code #} or {@code @} tag earlier in the
     *     same template.
     *   </li>
     *   <li><p>
     *     2.3.20 (or higher): {@code ?html} will escape apostrophe-quotes just like {@code ?xhtml} does. Utilizing
     *     this is highly recommended, because otherwise if interpolations are used inside attribute values that use
     *     apostrophe-quotation (<tt>&lt;foo bar='${val}'></tt>) instead of plain quotation mark
     *     (<tt>&lt;foo bar="${val}"></tt>), they might produce HTML/XML that's not well-formed. Note that
     *     {@code ?html} didn't do this because long ago there was no cross-browser way of doing this, but it's not a
     *     concern anymore.
     *   </li>
     * </ul>
     *
     * @since 2.3.20
     */
    public void setIncompatibleImprovements(Version version) {
        incompatibleImprovements = version;
    }

    /**
     * @see #setIncompatibleImprovements(Version) 
     * @since 2.3.20
     */
    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }
    
    /**
     * @deprecated Use {@link #setIncompatibleImprovements(Version)} instead.
     */
    public void setIncompatibleEnhancements(String version) {
        setIncompatibleImprovements(new Version(version));
    }
    
    /**
     * @deprecated Use {@link #getIncompatibleImprovements()} instead.
     */
    public String getIncompatibleEnhancements() {
        return incompatibleImprovements.toString();
    }
    
    /**
     * @deprecated Use {@link #getIncompatibleImprovements()} instead.
     */
    public int getParsedIncompatibleEnhancements() {
        return getIncompatibleImprovements().intValue();
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
     * that has no {@code #ftl} in it. The {@code tagSyntax}
     * parameter must be one of:
     * <ul>
     *   <li>{@link Configuration#AUTO_DETECT_TAG_SYNTAX}:
     *     use the syntax of the first FreeMarker tag (can be anything, like <tt>#list</tt>,
     *     <tt>#include</tt>, user defined, etc.)
     *   <li>{@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}:
     *     use the angle bracket syntax (the normal syntax)
     *   <li>{@link Configuration#SQUARE_BRACKET_TAG_SYNTAX}:
     *     use the square bracket syntax
     * </ul>
     *
     * <p>In FreeMarker 2.3.x {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX} is the
     * default for better backward compatibility. Starting from 2.4.x {@link
     * Configuration#AUTO_DETECT_TAG_SYNTAX} is the default, so it's recommended to use
     * that even for 2.3.x.
     * 
     * <p>This setting is ignored for the templates that have {@code ftl} directive in
     * it. For those templates the syntax used for the {@code ftl} directive determines
     * the syntax.
     */
    public void setTagSyntax(int tagSyntax) {
        if (tagSyntax != AUTO_DETECT_TAG_SYNTAX
            && tagSyntax != SQUARE_BRACKET_TAG_SYNTAX
            && tagSyntax != ANGLE_BRACKET_TAG_SYNTAX)
        {
            throw new IllegalArgumentException("\"tag_syntax\" can only be set to one of these: "
                    + "Configuration.AUTO_DETECT_TAG_SYNTAX, Configuration.ANGLE_BRACKET_SYNTAX, "
                    + "or Configuration.SQAUARE_BRACKET_SYNTAX");
        }
        this.tagSyntax = tagSyntax;
    }
    
    /**
     * The getter pair of {@link #setTagSyntax(int)}.
     */
    public int getTagSyntax() {
        return tagSyntax;
    }
    
    /**
     * Retrieves the template with the given name from the template cache, loading it into the cache first if it's
     * missing/staled.
     * 
     * <p>This is a shorthand for {@link #getTemplate(String, Locale, String, boolean)
     * getTemplate(name, getLocale(), getEncoding(getLocale()), true)}; see more details there.
     * 
     * <p>See {@link Configuration} for an example of basic usage.
     */
    public Template getTemplate(String name) throws IOException {
        Locale loc = getLocale();
        return getTemplate(name, loc, getEncoding(loc), true);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, String, boolean)
     * getTemplate(name, locale, getEncoding(locale), true)}.
     */
    public Template getTemplate(String name, Locale locale) throws IOException {
        return getTemplate(name, locale, getEncoding(locale), true);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, String, boolean)
     * getTemplate(name, getLocale(), encoding, true)}.
     */
    public Template getTemplate(String name, String encoding) throws IOException {
        return getTemplate(name, getLocale(), encoding, true);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, String, boolean)
     * getTemplate(name, locale, encoding, true)}.
     */
    public Template getTemplate(String name, Locale locale, String encoding) throws IOException {
        return getTemplate(name, locale, encoding, true);
    }

    /**
     * Retrieves the template with the given name (and according the specified further parameters) from the template
     * cache, loading it into the cache first if it's missing/staled.
     * 
     * <p>See {@link Configuration} for an example of basic usage.
     *
     * @param name The name of the template. Can't be {@code null}. The exact syntax of the name
     *     is interpreted by the underlying {@link TemplateLoader}, but the
     *     cache makes some assumptions. First, the name is expected to be
     *     a hierarchical path, with path components separated by a slash
     *     character (not with backslash!). The path (the name) given here must <em>not</em> begin with slash;
     *     it's always interpreted relative to the "template root directory".
     *     Then, the {@code ..} and {@code .} path meta-elements will be resolved.
     *     For example, if the name is {@code a/../b/./c.ftl}, then it will be
     *     simplified to {@code b/c.ftl}. The rules regarding this are same as with conventional
     *     UN*X paths. The path must not reach outside the template root directory, that is,
     *     it can't be something like {@code "../templates/my.ftl"} (not even if this path
     *     happens to be equivalent with {@code "/my.ftl"}).
     *     Further, the path is allowed to contain at most
     *     one path element whose name is {@code *} (asterisk). This path meta-element triggers the
     *     <i>acquisition mechanism</i>. If the template is not found in
     *     the location described by the concatenation of the path left to the
     *     asterisk (called base path) and the part to the right of the asterisk
     *     (called resource path), the cache will attempt to remove the rightmost
     *     path component from the base path ("go up one directory") and concatenate
     *     that with the resource path. The process is repeated until either a
     *     template is found, or the base path is completely exhausted.
     *
     * @param locale The requested locale of the template. Can't be {@code null}.
     *     Assuming you have specified {@code en_US} as the locale and
     *     {@code myTemplate.ftl} as the name of the template, the cache will
     *     first try to retrieve {@code myTemplate_en_US.html}, then
     *     {@code myTemplate.en.ftl}, and finally {@code myTemplate.ftl}.
     *
     * @param encoding The charset used to interpret the template source code bytes. Can't be {@code null}.
     *
     * @param parseAsFTL If {@code true}, the loaded template is parsed and interpreted normally,
     *     as a regular FreeMarker template. If {@code false}, the loaded template is
     *     treated as a static text, so <code>${...}</code>, {@code <#...>} etc. will not have special meaning
     *     in it.
     * 
     * @return the requested template; not {@code null}.
     * 
     * @throws FileNotFoundException if the template could not be found.
     * @throws IOException if there was a problem loading the template.
     * @throws ParseException (extends <code>IOException</code>) if the template is syntactically bad.
     */
    public Template getTemplate(String name, Locale locale, String encoding, boolean parseAsFTL) throws IOException {
        Template result = cache.getTemplate(name, locale, encoding, parseAsFTL);
        if (result == null) {
            throw new FileNotFoundException("Template " + StringUtil.jQuote(name) + " not found.");
        }
        return result;
    }

    /**
     * Sets the default encoding for converting bytes to characters when
     * reading template files in a locale for which no explicit encoding
     * was specified.
     * 
     * <p>Defaults to the default system encoding, which can change from one server to
     * another, so <b>you should always set this setting</b>.
     * 
     * @param encoding The name of the charset, such as {@code "UTF-8"} or {@code "ISO-8859-1"}
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
     */
    public String getEncoding(Locale locale) {
        if (localeToCharsetMap.isEmpty()) {
            return defaultEncoding;
        } else {
            // Try for a full name match (may include country and variant)
            String charset = (String) localeToCharsetMap.get(locale.toString());
            if (charset == null) {
                if (locale.getVariant().length() > 0) {
                    Locale l = new Locale(locale.getLanguage(), locale.getCountry());
                    charset = (String) localeToCharsetMap.get(l.toString());
                    if (charset != null) {
                        localeToCharsetMap.put(locale.toString(), charset);
                    }
                } 
                charset = (String) localeToCharsetMap.get(locale.getLanguage());
                if (charset != null) {
                    localeToCharsetMap.put(locale.toString(), charset);
                }
            }
            return charset != null ? charset : defaultEncoding;
        }
        
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
        localeToCharsetMap.put(locale.toString(), encoding);
    }

    /**
     * Adds a shared variable to the configuration.
     * Shared sharedVariables are sharedVariables that are visible
     * as top-level sharedVariables for all templates which use this
     * configuration, if the data model does not contain a
     * variable with the same name.
     *
     * <p>Never use <tt>TemplateModel</tt> implementation that is not thread-safe for shared sharedVariables,
     * if the configuration is used by multiple threads! It is the typical situation for Servlet based Web sites.
     *
     * @param name the name used to access the data object from your template.
     *     If a shared variable with this name already exists, it will replace
     *     that.
     * @see #setSharedVariable(String,Object)
     * @see #setAllSharedVariables
     */
    public void setSharedVariable(String name, TemplateModel tm) {
        sharedVariables.put(name, tm);
    }

    /**
     * Returns the set containing the names of all defined shared sharedVariables.
     * The method returns a new Set object on each call that is completely
     * disconnected from the Configuration. That is, modifying the set will have
     * no effect on the Configuration object.
     */
    public Set getSharedVariableNames() {
        return new HashSet(sharedVariables.keySet());
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
     * <p>Never use <tt>TemplateModel</tt> implementation that is not thread-safe for shared sharedVariables,
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
     * Gets a shared variable. Shared sharedVariables are sharedVariables that are 
     * available to all templates. When a template is processed, and an identifier
     * is undefined in the data model, a shared variable object with the same identifier
     * is then looked up in the configuration. There are several predefined sharedVariables
     * that are always available through this method, see the FreeMarker manual
     * for a comprehensive list of them.
     *
     * @see #setSharedVariable(String,Object)
     * @see #setSharedVariable(String,TemplateModel)
     * @see #setAllSharedVariables
     */
    public TemplateModel getSharedVariable(String name) {
        return (TemplateModel) sharedVariables.get(name);
    }
    
    /**
     * Removes all shared sharedVariables, except the predefined ones (compress, html_escape, etc.).
     */
    public void clearSharedVariables() {
        sharedVariables.clear();
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
     * Equivalent to <tt>removeTemplateFromCache(name, thisCfg.getLocale(), thisCfg.getEncoding(thisCfg.getLocale()), true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name) throws IOException {
        Locale loc = getLocale();
        removeTemplateFromCache(name, loc, getEncoding(loc), true);
    }

    /**
     * Equivalent to <tt>removeTemplateFromCache(name, locale, thisCfg.getEncoding(locale), true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, Locale locale) throws IOException {
        removeTemplateFromCache(name, locale, getEncoding(locale), true);
    }

    /**
     * Equivalent to <tt>removeTemplateFromCache(name, thisCfg.getLocale(), encoding, true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, String encoding) throws IOException {
        removeTemplateFromCache(name, getLocale(), encoding, true);
    }

    /**
     * Equivalent to <tt>removeTemplateFromCache(name, locale, encoding, true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, Locale locale, String encoding) throws IOException {
        removeTemplateFromCache(name, locale, encoding, true);
    }
    
    /**
     * Removes a template from the template cache, hence forcing the re-loading
     * of it when it's next time requested. This is to give the application
     * finer control over cache updating than {@link #setTemplateUpdateDelay(int)}
     * alone does.
     * 
     * For the meaning of the parameters, see
     * {@link #getTemplate(String, Locale, String, boolean)}.
     * 
     * @since 2.3.19
     */
    public void removeTemplateFromCache(
            String name, Locale locale, String encoding, boolean parse)
    throws IOException {
        cache.removeTemplate(name, locale, encoding, parse);
    }    
    
    /**
     * The getter pair of {@link #setLocalizedLookup(boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine works.
     */
    public boolean getLocalizedLookup() {
        return cache.getLocalizedLookup();
    }
    
    /**
     * Enables/disables localized template lookup. Enabled by default.
     * 
     * <p>Localized lookup works like this: Let's say your locale setting is "en_AU", and you call
     * {@link Configuration#getTemplate(String) cfg.getTemplate("foo.ftl")}. Then FreeMarker will look for the template
     * under names, stopping at the first that exists: {@code "foo_en_AU.ftl"}, {@code "foo_en.ftl"}, {@code "foo.ftl"}.
     * 
     * <p>This method is thread-safe and can be called while the engine works.
     */
    public void setLocalizedLookup(boolean localizedLookup) {
        this.localizedLookup = localizedLookup;
        cache.setLocalizedLookup(localizedLookup);
    }
    
    public void setSetting(String key, String value) throws TemplateException {
        try {
            if ("TemplateUpdateInterval".equalsIgnoreCase(key)) {
                key = TEMPLATE_UPDATE_DELAY_KEY;
            } else if ("DefaultEncoding".equalsIgnoreCase(key)) {
                key = DEFAULT_ENCODING_KEY;
            }
            
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
            } else if (INCOMPATIBLE_IMPROVEMENTS.equals(key)) {
                setIncompatibleImprovements(new Version(value));
            } else if (INCOMPATIBLE_ENHANCEMENTS.equals(key)) {
                setIncompatibleEnhancements(value);
            } else {
                super.setSetting(key, value);
            }
        } catch(Exception e) {
            throw new _MiscTemplateException(e, getEnvironment(), new Object[] {
                    "Failed to set setting ", new _DelayedJQuote(key),
                    " to value ", new _DelayedJQuote(value), "; see cause exception." });
        }
    }
    
    /**
     * Adds an invisible <code>#import <i>templateName</i> as <i>namespaceVarName</i></code> at the beginning of all
     * templates. The order of the imports will be the same as the order in which they were added with this method.
     */
    public synchronized void addAutoImport(String namespaceVarName, String templateName) {
        autoImports.remove(namespaceVarName);
        autoImports.add(namespaceVarName);
        autoImportNsToTmpMap.put(namespaceVarName, templateName);
    }
    
    /**
     * Removes an auto-import; see {@link #addAutoImport(String, String)}. Does nothing if the auto-import doesn't
     * exist.
     */
    public synchronized void removeAutoImport(String namespaceVarName) {
        autoImports.remove(namespaceVarName);
        autoImportNsToTmpMap.remove(namespaceVarName);
    }
    
    /**
     * Removes all auto-imports, then calls {@link #addAutoImport(String, String)} for each {@link Map}-entry (the entry
     * key is the {@code namespaceVarName}). The order of the auto-imports will be the same as {@link Map#keySet()}
     * returns the keys, thus, it's not the best idea to use a {@link HashMap} (although the order of imports doesn't
     * mater for properly designed libraries).
     */
    public synchronized void setAutoImports(Map map) {
        autoImports = new ArrayList(map.keySet());
        if (map instanceof HashMap) {
            autoImportNsToTmpMap = (Map) ((HashMap) map).clone();
        } 
        else if (map instanceof SortedMap) {
            autoImportNsToTmpMap = new TreeMap(map);             
        }
        else {
            autoImportNsToTmpMap = new HashMap(map);
        }
    }
    
    protected void doAutoImportsAndIncludes(Environment env)
    throws TemplateException, IOException
    {
        for (int i=0; i<autoImports.size(); i++) {
            String namespace = (String) autoImports.get(i);
            String templateName = (String) autoImportNsToTmpMap.get(namespace);
            env.importLib(templateName, namespace);
        }
        for (int i = 0; i < autoIncludes.size(); i++) {
            String templateName = (String) autoIncludes.get(i);
            Template template = getTemplate(templateName, env.getLocale());
            env.include(template);
        }
    }
    
    /**
     * Adds an invisible <code>#include <i>templateName</i> as <i>namespaceVarName</i></code> at the beginning of all
     * templates. The order of the inclusions will be the same as the order in which they were added with this method.
     */
    public synchronized void addAutoInclude(String templateName) {
        autoIncludes.remove(templateName);
        autoIncludes.add(templateName);
    }

    /**
     * Removes all auto-includes, then calls {@link #addAutoInclude(String)} for each {@link List} items.
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
     * Removes a template from the auto-include list; see {@link #addAutoInclude(String)}. Does nothing if the template
     * is not there.
     */
    public synchronized void removeAutoInclude(String templateName) {
        autoIncludes.remove(templateName);
    }

    /**
     * Returns FreeMarker version number string. 
     * 
     * @deprecated Use {@link #getVersion()} instead.
     */
    public static String getVersionNumber() {
        if (!versionPropertiesLoaded) loadVersionProperties();
        return versionNumber;
    }
    
    /**
     * Returns the FreeMarker version information, most importantly the major.minor.micro version numbers.
     * 
     * On FreeMarker version numbering rules:
     * <ul>
     *   <li>For final/stable releases the version number is like major.minor.micro, like 2.3.19. (Historically,
     *       when micro was 0 the version strings was like major.minor instead of the proper major.minor.0, but that's
     *       not like that anymore.)
     *   <li>When only the micro version is increased, compatibility with previous versions with the same
     *       major.minor is kept. Thus <tt>freemarker.jar</tt> can be replaced in an existing application without
     *       breaking it.</li>
     *   <li>For non-final/unstable versions (that almost nobody uses), the format is:
     *       <ul>
     *         <li>Starting from 2.3.20: major.minor.micro-extraInfo, like
     *             2.3.20-nightly_20130506T123456Z, 2.4.0-RC01. The major.minor.micro
     *             always indicates the target we move towards, so 2.3.20-nightly or 2.3.20-M01 is
     *             after 2.3.19 and will eventually become to 2.3.20. "PRE", "M" and "RC" (uppercase!) means
     *             "preview", "milestone" and "release candidate" respectively, and is always followed by a 2 digit
     *             0-padded counter, like M03 is the 3rd milestone release of a given major.minor.micro.</li> 
     *         <li>Before 2.3.20: The extraInfo wasn't preceded by a "-".
     *             Instead of "nightly" there was "mod", where the major.minor.micro part has indicated where
     *             are we coming from, so 2.3.19mod (read as: 2.3.19 modified) was after 2.3.19 but before 2.3.20.
     *             Also, "pre" and "rc" was lowercase, and was followd by a number without 0-padding.</li>
     *       </ul>
     * </ul>
     * 
     * @since 2.3.20
     */ 
    public static Version getVersion() {
        if (!versionPropertiesLoaded) loadVersionProperties();
        return version;
    }
    
	private static void loadVersionProperties() {
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
                
                String versionString  = getRequiredVersionProperty(vp, "version");
                versionNumber = versionString;
                
                Date buildDate;
                {
                    String buildDateStr = getRequiredVersionProperty(vp, "buildTimestamp");
                    if (buildDateStr.endsWith("Z")) {
                    	buildDateStr = buildDateStr.substring(0, buildDateStr.length() - 1) + "+0000";
                    }
                    try {
    					buildDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(buildDateStr);
    				} catch (java.text.ParseException e) {
    					buildDate = null;
    				}
                }
                
                final Boolean gaeCompliant = Boolean.valueOf(getRequiredVersionProperty(vp, "isGAECompliant"));
                
                version = new Version(versionString, gaeCompliant, buildDate);
                
                versionPropertiesLoaded = true;
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load version file: " + e);
        }
	}
	
    /**
     * Returns the names of the supported "built-ins". These are the ({@code expr?builtin_name}-like things). As of this
     * writing, this information doesn't depend on the configuration options, so it could be a static method, but
     * to be future-proof, it's an instance method. 
     * 
     * @return {@link Set} of {@link String}-s. 
     */
	public Set getSupportedBuiltInNames() {
	    return _CoreAPI.getSupportedBuiltInNames();
	}

	private static String getRequiredVersionProperty(Properties vp, String properyName) {
		String s = vp.getProperty(properyName);
		if (s == null) {
		    throw new RuntimeException(
		    		"Version file is corrupt: \"" + properyName + "\" property is missing.");
		}
		return s;
	}
}
