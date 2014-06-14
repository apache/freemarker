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
import freemarker.cache._CacheAPI;
import freemarker.core.BugException;
import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.core.ParseException;
import freemarker.core._ConcurrentMapFactory;
import freemarker.core._CoreAPI;
import freemarker.core._ObjectBuilderSettingEvaluator;
import freemarker.core._SettingEvaluationEnvironment;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.utility.CaptureOutput;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.NormalizeNewlines;
import freemarker.template.utility.NullArgumentException;
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
 * setter methods like {@link #setTemplateLoader(TemplateLoader)} or by loading a {@code .properties} file), and then
 * use that single instance everywhere in your application. Frequently re-creating {@link Configuration} is a typical
 * and grave mistake from performance standpoint, as the {@link Configuration} holds the template cache, and often also
 * the class introspection cache, which then will be lost. (Note that, naturally, having multiple long-lived instances,
 * like one per component that internally uses FreeMarker is fine.)  
 * 
 * <p>The basic usage pattern is like:
 * 
 * <pre>
 *  // Where the application is initialized; in general you do this ONLY ONCE in the application life-cycle!
 *  Configuration cfg = new Configuration(new Version(X, Y, Z));
 *  // Where X, Y, Z enables the not-100%-backward-compatible fixes introduced in
 *  // FreeMarker version X.Y.Z  and earlier (see {@link #Configuration(Version)}).
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
 *   <li>{@link #setTemplateExceptionHandler(TemplateExceptionHandler) template_exception_handler}: For developing
 *       HTML pages, the most convenient value is {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}. For production,
 *       {@link TemplateExceptionHandler#RETHROW_HANDLER} is safer to use.
 *   <!-- 2.4: recommend the new object wrapper here -->
 * </ul>
 * 
 * <p>A {@link Configuration} object is thread-safe only after you have stopped modifying the configuration settings,
 * and you have <b>safely published</b> it (see JSR 133 and related literature) to other threads. Generally, you set
 * everything directly after you have instantiated the {@link Configuration} object, then you don't change the settings
 * anymore, so then it's safe to make it accessible (again, via a "safe publication" technique) from multiple threads.
 * The methods that aren't about modifying setting, like {@link #getTemplate(String)}, are thread-safe.
 */
public class Configuration extends Configurable implements Cloneable {
    private static final String VERSION_PROPERTIES_PATH = "freemarker/version.properties";
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
    public static final Version DEFAULT_INCOMPATIBLE_IMPROVEMENTS = _TemplateAPI.VERSION_2_3_0;
    /** @deprecated Use {@link #DEFAULT_INCOMPATIBLE_IMPROVEMENTS} instead. */
    public static final String DEFAULT_INCOMPATIBLE_ENHANCEMENTS = DEFAULT_INCOMPATIBLE_IMPROVEMENTS.toString();
    /** @deprecated Use {@link #DEFAULT_INCOMPATIBLE_IMPROVEMENTS} instead. */
    public static final int PARSED_DEFAULT_INCOMPATIBLE_ENHANCEMENTS = DEFAULT_INCOMPATIBLE_IMPROVEMENTS.intValue(); 
    
    private static final Version version;
    static {
        try {
            Properties vp = new Properties();
            InputStream ins = Configuration.class.getClassLoader()
                    .getResourceAsStream(VERSION_PROPERTIES_PATH);
            if (ins == null) {
                throw new RuntimeException("Version file is missing.");
            } else {
                try {
                    vp.load(ins);
                } finally {
                    ins.close();
                }
                
                String versionString  = getRequiredVersionProperty(vp, "version");
                
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
            }
        } catch (IOException e) {
            // Java 5: use cause
            throw new RuntimeException("Failed to load and parse " + VERSION_PROPERTIES_PATH + ": " + e);
        }
    }
    
    private final static Object defaultConfigLock = new Object();
    private static Configuration defaultConfig;

    private boolean strictSyntax = true;
    private volatile boolean localizedLookup = true;
    private boolean whitespaceStripping = true;
    private Version incompatibleImprovements;
    private int tagSyntax = ANGLE_BRACKET_TAG_SYNTAX;

    private TemplateCache cache;
    private boolean templateLoaderWasSet;

    private boolean objectWrapperWasSet;
    
    private HashMap sharedVariables = new HashMap();
    
    private String defaultEncoding = SecurityUtilities.getSystemProperty("file.encoding");
    private Map localeToCharsetMap = _ConcurrentMapFactory.newThreadSafeMap();
    
    private ArrayList autoImports = new ArrayList(), autoIncludes = new ArrayList(); 
    private Map autoImportNsToTmpMap = new HashMap();   // TODO No need for this, instead use List<NamespaceToTemplate> below.

    /**
     * @deprecated Use {@link Configuration} instead.
     */
    public Configuration() {
        this(DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
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
     *     2.3.0: This is just the starting point, the version used in older projects.
     *   </li>
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
     *   <li><p>
     *     2.3.21 (or higher):
     *     <ul>
     *       <li><p>
     *         The <em>default</em> of the {@code object_wrapper} setting ({@link #getObjectWrapper()}) changes from
     *         {@link ObjectWrapper#DEFAULT_WRAPPER} to another almost identical {@link DefaultObjectWrapper} singleton,
     *         returned by {@link DefaultObjectWrapperBuilder#getResult()}. The new default object wrapper's
     *         "incompatible improvements" version is set to the same as of the {@link Configuration}.
     *         See {@link BeansWrapper#BeansWrapper(Version)} for further details. Furthermore, the new default
     *         object wrapper doesn't allow changing its settings; setter methods throw {@link IllegalStateException}).
     *         (If anything tries to call setters on the old default in your application, that's a dangerous bug that
     *         won't remain hidden now. As the old default is a singleton too, potentially shared by independently
     *         developed components, most of them expects the out-of-the-box behavior from it (and the others are
     *         necessarily buggy). Also, then concurrency glitches can occur (and even pollute the class introspection
     *         cache) because the singleton is modified after publishing.)
     *         Furthermore the new default object wrapper shares class introspection cache with other
     *         {@link BeansWrapper}-s created with {@link BeansWrapperBuilder}, which has an impact as
     *         {@link BeansWrapper#clearClassIntrospecitonCache()} will be disallowed; see more about it there.
     *       </li>
     *       <li><p>
     *         The default of the {@code template_loader} setting ({@link Configuration#getTemplateLoader()}) changes
     *         to {@code null}, which means that FreeMarker will not find any templates. Earlier
     *         the default was a {@link FileTemplateLoader} that used the current directory as the root. This was
     *         dangerous and fragile as you usually don't have good control over what the current directory will be.
     *         Luckily, the old default almost never looked for the templates at the right place
     *         anyway, so pretty much all applications had to set a {@code template_loader} setting, so it's unlikely
     *         that changing the default breaks your application.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * @throws IllegalArgumentException if {@code incompatibleImmprovements} is greater than the current FreeMarker
     *     version, or less than 2.3.0.
     * 
     * @since 2.3.21
     */
    public Configuration(Version incompatibleImprovements) {
        super(incompatibleImprovements);
        
        NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
        this.incompatibleImprovements = incompatibleImprovements;
        
        createTemplateCache();
        loadBuiltInSharedVariables();
    }

    private void createTemplateCache() {
        cache = new TemplateCache(getDefaultTemplateLoader());
        cache.setConfiguration(this);
        cache.setDelay(5000);
    }
    
    private void recreateTemplateCacheWith(TemplateLoader loader, CacheStorage storage) {
        TemplateCache oldCache = cache;
        cache = new TemplateCache(loader, storage);
        cache.setDelay(oldCache.getDelay());
        cache.setConfiguration(this);
        cache.setLocalizedLookup(localizedLookup);
    }
    
    private TemplateLoader getDefaultTemplateLoader() {
        return incompatibleImprovements.intValue() < _CoreAPI.DEFAULT_TL_AND_OW_CHANGE_VERSION
                ? _CacheAPI.createLegacyDefaultTemplateLoader()
                : null;
    }
    
    public Object clone() {
        try {
            Configuration copy = (Configuration)super.clone();
            copy.sharedVariables = new HashMap(sharedVariables);
            copy.localeToCharsetMap = new HashMap(localeToCharsetMap);
            copy.autoImportNsToTmpMap = new HashMap(autoImportNsToTmpMap);
            copy.autoImports = (ArrayList) autoImports.clone();
            copy.autoIncludes = (ArrayList) autoIncludes.clone();
            copy.recreateTemplateCacheWith(cache.getTemplateLoader(), cache.getCacheStorage());
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new BugException(e.getMessage());  // Java 5: use cause exc.
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
     * Loads a preset language-to-encoding map, similarly as if you have called
     * {@link #clearEncodingMap()} and then did multiple {@link #setEncoding(Locale, String)} calls.
     * It assumes the usual character encodings for most languages.
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
        // Java 5: use volatile + double check
        synchronized (defaultConfigLock) {
            if (defaultConfig == null) {
                defaultConfig = new Configuration();
            }
            return defaultConfig;
        }
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
        synchronized (defaultConfigLock) {
            defaultConfig = config;
        }
    }
    
    /**
     * Sets a {@link TemplateLoader} that is used to look up and load templates;
     * as a side effect the template cache will be emptied.
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
     * (But if you still care what it is, before "incompatible improvements" 2.3.21 it's a {@link FileTemplateLoader}
     * that uses the current directory as its root; as it's hard tell what that directory will be, it's not very useful
     * and dangerous. Starting with "incompatible improvements" 2.3.21 the default is {@code null}.)   
     * 
     * <p>Note that setting the template loader will re-create the template cache, so
     * all its content will be lost.
     */
    public void setTemplateLoader(TemplateLoader loader) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            recreateTemplateCacheWith(loader, cache.getCacheStorage());
            templateLoaderWasSet = true;
        }
    }

    /**
     * The getter pair of {@link #setTemplateLoader(TemplateLoader)}.
     */
    public TemplateLoader getTemplateLoader()
    {
        return cache.getTemplateLoader();
    }

    /**
     * Sets the {@link CacheStorage} used for caching {@link Template}-s;
     * the earlier content of the template cache will be dropt.
     * 
     * The default is a {@link SoftCacheStorage}. If the total size of the {@link Template}
     * objects is significant but most templates are used rarely, using a
     * {@link MruCacheStorage} instead might be advisable. If you don't want caching at
     * all, use {@link freemarker.cache.NullCacheStorage} (you can't use {@code null}).
     * 
     * <p>Note that setting the cache storage will re-create the template cache, so
     * all its content will be lost.
     */
    public void setCacheStorage(CacheStorage storage) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            recreateTemplateCacheWith(cache.getTemplateLoader(), storage);
        }
    }
    
    /**
     * The getter pair of {@link #setCacheStorage(CacheStorage)}.
     * 
     * @since 2.3.20
     */
    public CacheStorage getCacheStorage() {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            return cache.getCacheStorage();
        }
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
            throw new BugException(exc.toString());  // Java 5: use cause exc.
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

    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        super.setObjectWrapper(objectWrapper);
        objectWrapperWasSet = true;
    }

    /**
     * The getter pair of {@link #setStrictSyntaxMode}.
     */
    public boolean getStrictSyntaxMode() {
        return strictSyntax;
    }

    /**
     * Use {@link #Configuration(Version)} instead if possible; see the meaning of the parameter there.
     * If the default value of a setting depends on the {@code incompatibleImprovements} and the value of that setting
     * was never set in this {@link Configuration} object through the public API, its value will be set to the default
     * value appropriate for the new {@code incompatibleImprovements}. (This adjustment of a setting value doesn't
     * count as setting that setting, so setting {@code incompatibleImprovements} for multiple times also works as
     * expected.) Note that if the {@code template_loader} have to be changed because of this, the template cache will
     * be emptied.
     * 
     * @throws IllegalArgumentException if {@code incompatibleImmprovements} is greater than the current FreeMarker
     *     version, or less than 2.3.0.
     * 
     * @since 2.3.20
     */
    public void setIncompatibleImprovements(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionSupported(incompatibleImprovements);
        boolean hadLegacyTLOWDefaults
                = this.incompatibleImprovements.intValue() < _CoreAPI.DEFAULT_TL_AND_OW_CHANGE_VERSION; 
        this.incompatibleImprovements = incompatibleImprovements;
        if (hadLegacyTLOWDefaults != incompatibleImprovements.intValue() < _CoreAPI.DEFAULT_TL_AND_OW_CHANGE_VERSION) {
            if (!templateLoaderWasSet) {
                recreateTemplateCacheWith(getDefaultTemplateLoader(), cache.getCacheStorage());
            }
            if (!objectWrapperWasSet) {
                // We use `super.` so that `objectWrapperWasSet` will not be set to `true`. 
                super.setObjectWrapper(_CoreAPI.getDefaultObjectWrapper(incompatibleImprovements));
            }
        }
    }

    /**
     * @see #setIncompatibleImprovements(Version) 
     * @since 2.3.20
     */
    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }
    
    /**
     * @deprecated Use {@link #Configuration(Version)}, or
     *    as last chance, {@link #setIncompatibleImprovements(Version)} instead.
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
     * <p>This method is thread-safe. 
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
            TemplateLoader tl = getTemplateLoader();  
            String msg; 
            if (tl == null) {
                msg = "Don't know from where to load template " + StringUtil.jQuote(name)
                      + " because the \"template_loader\" FreeMarker setting wasn't set.";
            } else {
                msg = "Template " + StringUtil.jQuote(name) + " not found.";
                if (!templateLoaderWasSet) {
                    msg += " Note that the \"template_loader\" FreeMarker setting wasn't set, so it's on its "
                            + "default value, which is most certainly not intended and the cause of this problem."; 
                }
                if (tl instanceof FileTemplateLoader) {            
                    msg += " The template directory used was: " + ((FileTemplateLoader) tl).getBaseDirectory();
                }
            }
            throw new FileNotFoundException(msg);
        }
        return result;
    }

    /**
     * Sets the charset used for decoding byte sequences to character sequences when
     * reading template files in a locale for which no explicit encoding
     * was specified via {@link #setEncoding(Locale, String)}. Note that by default there is no locale specified for
     * any locale, so the default encoding is always in effect.
     * 
     * <p>Defaults to the default system encoding, which can change from one server to
     * another, so <b>you should always set this setting</b>. If you don't know what charset your should chose,
     * {@code "UTF-8"} is usually a good choice.
     * 
     * <p>Note that individual templates may specify their own charset by starting with
     * <tt>&lt;#ftl encoding="..."></tt>
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
     * <p>This method is <b>not</b> thread safe; use it with the same restrictions as those that modify setting values. 
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
     * 
     * <p>This method is <b>not</b> thread safe; use it with the same restrictions as those that modify setting values. 
     * 
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
     * <p>This method is <b>not</b> thread safe; use it with the same restrictions as those that modify setting values. 
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
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
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
     * <p>For the meaning of the parameters, see
     * {@link #getTemplate(String, Locale, String, boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
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
    
    public void setSetting(String name, String value) throws TemplateException {
        boolean unknown = false;
        try {
            if ("TemplateUpdateInterval".equalsIgnoreCase(name)) {
                name = TEMPLATE_UPDATE_DELAY_KEY;
            } else if ("DefaultEncoding".equalsIgnoreCase(name)) {
                name = DEFAULT_ENCODING_KEY;
            }
            
            if (DEFAULT_ENCODING_KEY.equals(name)) {
                setDefaultEncoding(value);
            } else if (LOCALIZED_LOOKUP_KEY.equals(name)) {
                setLocalizedLookup(StringUtil.getYesNo(value));
            } else if (STRICT_SYNTAX_KEY.equals(name)) {
                setStrictSyntaxMode(StringUtil.getYesNo(value));
            } else if (WHITESPACE_STRIPPING_KEY.equals(name)) {
                setWhitespaceStripping(StringUtil.getYesNo(value));
            } else if (CACHE_STORAGE_KEY.equals(name)) {
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
                            throw invalidSettingValueException(name, value);
                        }
                        if ("soft".equalsIgnoreCase(pname)) {
                            softSize = pvalue;
                        } else if ("strong".equalsIgnoreCase(pname)) {
                            strongSize = pvalue;
                        } else {
                            throw invalidSettingValueException(name, value);
                        }
                    }
                    if (softSize == 0 && strongSize == 0) {
                        throw invalidSettingValueException(name, value);
                    }
                    setCacheStorage(new MruCacheStorage(strongSize, softSize));
                } else {
                    setCacheStorage((CacheStorage) _ObjectBuilderSettingEvaluator.eval(
                            value, CacheStorage.class, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (TEMPLATE_UPDATE_DELAY_KEY.equals(name)) {
                setTemplateUpdateDelay(Integer.parseInt(value));
            } else if (AUTO_INCLUDE_KEY.equals(name)) {
                setAutoIncludes(parseAsList(value));
            } else if (AUTO_IMPORT_KEY.equals(name)) {
                setAutoImports(parseAsImportList(value));
            } else if (TAG_SYNTAX_KEY.equals(name)) {
                if ("auto_detect".equals(value)) {
                    setTagSyntax(AUTO_DETECT_TAG_SYNTAX);
                } else if ("angle_bracket".equals(value)) {
                    setTagSyntax(ANGLE_BRACKET_TAG_SYNTAX);
                } else if ("square_bracket".equals(value)) {
                    setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (INCOMPATIBLE_IMPROVEMENTS.equals(name)) {
                setIncompatibleImprovements(new Version(value));
            } else if (INCOMPATIBLE_ENHANCEMENTS.equals(name)) {
                setIncompatibleEnhancements(value);
            } else {
                unknown = true;
            }
        } catch(Exception e) {
            throw settingValueAssignmentException(name, value, e);
        }
        if (unknown) {
            super.setSetting(name, value);
        }
    }

    protected String getCorrectedNameForUnknownSetting(String name) {
        if ("encoding".equals(name) || "charset".equals(name) || "default_charset".equals(name)) {
            return DEFAULT_ENCODING_KEY;
        }
        return super.getCorrectedNameForUnknownSetting(name);
    }
    
    /**
     * Adds an invisible <code>#import <i>templateName</i> as <i>namespaceVarName</i></code> at the beginning of all
     * templates. The order of the imports will be the same as the order in which they were added with this method.
     */
    public void addAutoImport(String namespaceVarName, String templateName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            autoImports.remove(namespaceVarName);
            autoImports.add(namespaceVarName);
            autoImportNsToTmpMap.put(namespaceVarName, templateName);
        }
    }
    
    /**
     * Removes an auto-import; see {@link #addAutoImport(String, String)}. Does nothing if the auto-import doesn't
     * exist.
     */
    public void removeAutoImport(String namespaceVarName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            autoImports.remove(namespaceVarName);
            autoImportNsToTmpMap.remove(namespaceVarName);
        }
    }
    
    /**
     * Removes all auto-imports, then calls {@link #addAutoImport(String, String)} for each {@link Map}-entry (the entry
     * key is the {@code namespaceVarName}). The order of the auto-imports will be the same as {@link Map#keySet()}
     * returns the keys, thus, it's not the best idea to use a {@link HashMap} (although the order of imports doesn't
     * mater for properly designed libraries).
     */
    public void setAutoImports(Map map) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
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
    public void addAutoInclude(String templateName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            autoIncludes.remove(templateName);
            autoIncludes.add(templateName);
        }
    }

    /**
     * Removes all auto-includes, then calls {@link #addAutoInclude(String)} for each {@link List} items.
     */
    public void setAutoIncludes(List templateNames) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
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
    }
    
    /**
     * Removes a template from the auto-include list; see {@link #addAutoInclude(String)}. Does nothing if the template
     * is not there.
     */
    public void removeAutoInclude(String templateName) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            autoIncludes.remove(templateName);
        }
    }

    /**
     * Returns FreeMarker version number string. 
     * 
     * @deprecated Use {@link #getVersion()} instead.
     */
    public static String getVersionNumber() {
        return version.toString();
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
        return version;
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
