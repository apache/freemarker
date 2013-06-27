/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
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

package freemarker.log;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.utility.ClassUtil;

/**
 * The FreeMarker logging facility. This is a polymorphic implementation
 * that will use whatever logging package it can find on the system:
 * Apache Log4J, Apache Avalon LogKit, <tt>java.util.logging</tt> (in this
 * order). If it fails to find any of the above, logging will be suppressed
 * and a short notice will be printed to <tt>System.err</tt>.
 *
 * <p>You can use the {@link #selectLoggerLibrary(int)} static method to force
 * use of a specific logger package, or to turn off logging.
 * 
 * <p>SLF4J or Apache Commons Logging are supported (since FreeMarker 2.3.17)
 * and recommended, but are never selected automatically (until FreeMarker 2.4)
 * due to backward compatibility constraints; you have to  select them
 * explicitly with {@link #selectLoggerLibrary(int)}.
 * 
 * @author Attila Szegedi
 */
public abstract class Logger
{
	
    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should automatically lookup and use any available logger library.
     */
    public static final int LIBRARY_AUTO = -1;
    
    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should use no logger package (i.e. turn off logging).
     */
    public static final int LIBRARY_NONE = 0;
    
    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should use the <tt>java.util.logging</tt> logger package.
     */
    public static final int LIBRARY_JAVA = 1;
    
    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should use the Apache Jakarta Avalon LogKit logger package.
     */
    public static final int LIBRARY_AVALON = 2;
    
    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should use the Apache Jakarta Log4J logger package.
     */
    public static final int LIBRARY_LOG4J = 3;

    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should use the Apache commons-logging logger adapter package.
     */
    public static final int LIBRARY_COMMONS = 4;
    
    /**
     * Constant used with {@link #selectLoggerLibrary(int)} that indicates the
     * engine should use the SLF4J logger adapter package.
     */
    public static final int LIBRARY_SLF4J = 5;

    /**
     * Order matters! Starts with the lowest priority.
     */
    private static final String[] LIBINIT =
    {
        "freemarker.log.Logger", "_Null",
        "java.util.logging.Logger", "_JDK14",
        "org.apache.log.Logger",    "_Avalon",
        "org.apache.log4j.Logger",  "_Log4J",
        /* In 2.3.x this two is skipped by LIBRARY_AUTO: */
        "org.apache.commons.logging.Log",  "CommonsLogging",
        "org.slf4j.Logger",  "SLF4J",
    };

    private static int logLibrary;
    private static LoggerFactory factory;
    private static String categoryPrefix = "";

    private static final Map loggers = new HashMap();

    /**
     * Selects the logger library to use.
     * If you want to change the default setting, do it early in application
     * initialization phase, before calling any other FreeMarker API since once
     * various parts of the FreeMarker library bind to the logging subsystem,
     * the change in this value will have no effect on them.
     * @param library one of <tt>LIBRARY_XXX</tt> constants. By default,
     * {@link #LIBRARY_AUTO} is used.
     * @throws ClassNotFoundException if an explicit logging library is asked for
     * (that is, neither NONE, nor AUTO), and it's not found in the classpath.
     */
    public static void selectLoggerLibrary(int library)
    throws
        ClassNotFoundException
    {
        synchronized (Logger.class) {
            if(library < -1 || (library*2) >= LIBINIT.length)
            {
                throw new IllegalArgumentException();
            }
            logLibrary = library;
            factory = createFactory();
        }
    }

    /**
     * Sets a category prefix. This prefix is prepended to any logger category
     * name. This makes it possible to have different FreeMarker logger categories
     * on a per-application basis (better said, per-classloader basis). By default
     * the category prefix is the empty string. If you set a non-empty category
     * prefix, be sure to include the trailing separator dot (i.e. "MyApp.")
     * If you want to change the default setting, do it early in application
     * initialization phase, before calling any other FreeMarker API since once
     * various parts of the FreeMarker library bind to the logging subsystem,
     * the change in this value will have no effect on them.
     */
    public static void setCategoryPrefix(String prefix)
    {
        synchronized (Logger.class) {
            if(prefix == null)
            {
                throw new IllegalArgumentException();
            }
            categoryPrefix = prefix;
        }
    }

    /**
     * Logs a debugging message.
     */
    public abstract void debug(String message);

    /**
     * Logs a debugging message with accompanying throwable.
     */
    public abstract void debug(String message, Throwable t);

    /**
     * Logs an informational message.
     */
    public abstract void info(String message);

    /**
     * Logs an informational message with accompanying throwable.
     */
    public abstract void info(String message, Throwable t);

    /**
     * Logs a warning message.
     */
    public abstract void warn(String message);

    /**
     * Logs a warning message with accompanying throwable.
     */
    public abstract void warn(String message, Throwable t);

    /**
     * Logs an error message.
     */
    public abstract void error(String message);

    /**
     * Logs an error message with accompanying throwable.
     */
    public abstract void error(String message, Throwable t);

    /**
     * Returns true if this logger will log debug messages.
     */
    public abstract boolean isDebugEnabled();

    /**
     * Returns true if this logger will log informational messages.
     */
    public abstract boolean isInfoEnabled();

    /**
     * Returns true if this logger will log warning messages.
     */
    public abstract boolean isWarnEnabled();

    /**
     * Returns true if this logger will log error messages.
     */
    public abstract boolean isErrorEnabled();

    /**
     * Returns true if this logger will log fatal error messages.
     */
    public abstract boolean isFatalEnabled();

    /**
     * Returns a logger for the specified category.
     * @param category a dot separated hierarchical category name. If a category
     * prefix is in effect, it's prepended to the category name.
     */
    public static Logger getLogger(String category)
    {
        if (factory == null) {
            synchronized (Logger.class) {
                if (factory == null) {
                    try
                    {
                        selectLoggerLibrary(LIBRARY_AUTO);
                    }
                    catch(ClassNotFoundException e)
                    {
                        // This can't happen, really
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }

        category = categoryPrefix + category;

        synchronized(loggers)
        {
            Logger logger = (Logger)loggers.get(category);
            if(logger == null)
            {
                logger = factory.getLogger(category);
                loggers.put(category, logger);
            }
            return logger;
        }
    }

    private static LoggerFactory createFactory()
    throws
        ClassNotFoundException
    {
        if(logLibrary == LIBRARY_AUTO)
        {
            for(int i = LIBINIT.length / 2 - 1; i > 0; --i)
            {
            	// For backward-compatibility we skip these in 2.3.x:
            	if (i == LIBRARY_SLF4J || i == LIBRARY_COMMONS) continue;
            	
                try
                {
                    return createFactory(i);
                }
                catch(ClassNotFoundException e)
                {
                    ;//Intentionally ignored
                }
            }
            System.err.println("*** WARNING: FreeMarker logging suppressed.");
            return new _NullLoggerFactory();
        }
        else
        {
            return createFactory(logLibrary);
        }
    }

    private static LoggerFactory createFactory(int library)
    throws
        ClassNotFoundException
    {
        String loggerClassName = LIBINIT[library * 2];
        String factoryType = LIBINIT[library * 2 + 1];

        try
        {
            ClassUtil.forName(loggerClassName);
            return (LoggerFactory)Class.forName("freemarker.log." + factoryType + "LoggerFactory").newInstance();
        }
        catch(IllegalAccessException e)
        {
            // This can't happen, really
            throw new IllegalAccessError(e.getMessage());
        }
        catch(InstantiationException e)
        {
            // This can't happen, really
            throw new InstantiationError(e.getMessage());
        }
    }
}
