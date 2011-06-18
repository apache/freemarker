package freemarker.ext.script;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.utility.StringUtil;

/**
 * This is the implementation of JSR-223 (javax.script) script engine factory
 * for FreeMarker. You might want to cast a generic script engine factory 
 * reference to this class if you want to invoke {@link #setConfiguration(Configuration)}
 * to modify its configuration.
 * @author Attila Szegedi
 * @version $Id: $
 */
public class FreeMarkerScriptEngineFactory implements ScriptEngineFactory
{
    // FreeMarkerPermission is not backported yet.
    /*
    private static final Permission SET_CONFIGURATION = 
        new FreeMarkerPermission("setScriptEngineConfiguration");
    */

    private Configuration config;
    
    private static final TemplateLoader NOOP_TEMPLATE_LOADER = 
        new TemplateLoader()
        {
            public void closeTemplateSource(Object templateSource)
            {
            }

            public Object findTemplateSource(String name)
            {
                return null;
            }

            public long getLastModified(Object templateSource)
            {
                return 0;
            }

            public Reader getReader(Object templateSource, String encoding)
            {
                return null;
            }
        };
    
    public FreeMarkerScriptEngineFactory()
    {
        config = new Configuration();
        // No <#include> and <#import> support
        config.setTemplateLoader(NOOP_TEMPLATE_LOADER);
        // BeansWrapper is closest to the JSR-223 idea of handling bindings
        config.setObjectWrapper(new BeansWrapper());
    }
    
    /**
     * Sets a new FreeMarker configuration to use by all script engines created
     * by this script engine factory. By default, the engine uses a private
     * configuration instance with {@link BeansWrapper} and a no-op {@link
     * TemplateLoader}.
     * @param config the new FreeMarker configuration object
     * @throws IllegalArgumentException if config is null
     * @throws SecurityException if security manager is set in the JVM, and the
     * invoking code doesn't posess the
     * "freeMarker.script.setEngineFactoryConfiguration" runtime permission.
     */
    public void setConfiguration(Configuration config) 
    throws IllegalArgumentException, SecurityException
    {
        if(config == null)
        {
            throw new IllegalArgumentException("config == null");
        }
        // FreeMarkerPermission is not backported yet.
        /*
        SecurityManager sm = System.getSecurityManager();
        if(sm != null)
        {
            sm.checkPermission(SET_CONFIGURATION);
        }
        */
        synchronized(this)
        {
            this.config = config;
        }
    }
    
    public String getEngineName()
    {
        return "FreeMarker";
    }

    public String getEngineVersion()
    {
        return Configuration.getVersionNumber();
    }

    private static final List<String> extensions = 
        Collections.unmodifiableList(Arrays.asList(new String[] { 
                "ftl", 
                "fm"
        }));
    
    public List<String> getExtensions()
    {
        return extensions;
    }

    public String getLanguageName()
    {
        return "FreeMarker Template Language";
    }

    public String getLanguageVersion()
    {
        return Configuration.getVersionNumber();
    }

    public String getMethodCallSyntax(String object, String method, String... args)
    {
        StringBuilder b = new StringBuilder();
        b.append("[@").append(object).append('.').append(method).append('(');
        if(args.length > 0)
        {
            b.append(args[0]);
            for (int i = 1; i < args.length; i++)
            {
                b.append(", ").append(args[i]);
            }
        }
        return b.append(")/]").toString();
    }

    private static final List<String> mimeTypes = 
        Collections.unmodifiableList(Arrays.asList(new String[] {
                // NOTE: these aren't registered with IANA *yet*
                "application/freemarker", 
                "text/freemarker", 
                "application/vnd.freemarker", 
                "text/vnd.freemarker", 
        }));

    public List<String> getMimeTypes()
    {
        return mimeTypes;
    }

    private static final List<String> names = 
        Collections.unmodifiableList(Arrays.asList(new String[] { 
                "FreeMarker",
                "Freemarker",
                "freemarker",
                "ftl",
                "FTL"
        }));

    public List<String> getNames()
    {
        return names;
    }

    public String getOutputStatement(String string)
    {
        return "${\"" + StringUtil.FTLStringLiteralEnc(string) + "\"}";
    }

    public Object getParameter(String param)
    {
        if(ScriptEngine.ENGINE.equals(param))
        {
            return getEngineName();
        }
        if(ScriptEngine.ENGINE_VERSION.equals(param))
        {
            return getEngineVersion();
        }
        if(ScriptEngine.NAME.equals(param))
        {
            return "FreeMarker";
        }
        if(ScriptEngine.LANGUAGE.equals(param))
        {
            return getLanguageName();
        }
        if(ScriptEngine.LANGUAGE_VERSION.equals(param))
        {
            return getLanguageVersion();
        }
        if("THREADING".equals(param))
        {
            // Execution of a FreeMarker template doesn't modify variables in
            // the engine scopes
            return "STATELESS";
        }
        return null;
    }

    public String getProgram(String... statements)
    {
        StringBuilder b = new StringBuilder();
        for (String statement : statements)
        {
            b.append(statement).append('\n');
        }
        return b.toString();
    }

    public ScriptEngine getScriptEngine()
    {
        Configuration config;
        synchronized(this)
        {
            config = this.config;
        }
        return new FreeMarkerScriptEngine(this, config);
    }
}
