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

package freemarker.log;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate logger creation to whatever logging library is available on the system. It looks for logger libraries in
 * this order (in FreeMarker 2.3.x): Log4J, Avalon LogKit, JUL (i.e., <tt>java.util.logging</tt>).
 * 
 * <p>
 * In FreeMarker 2.3.x, SLF4J and commons-logging is not searched by the auto detection for backward compatibility. It's
 * very likely that starting from FreeMarker 2.4.x first SLF4J and then commons-logging will be searched. Until that,
 * you can use the {@value #PROPERTY_NAME_LOGGER_LIBRARY} system property to select SLF4J, like
 * {@code java ... -Dorg.freemarker.loggerLibrary=slf4j}. (Don't set this system property from Java code; it's not
 * reliable that way, also it can cause confusion if you override the value set by others by {@code -D}.)
 */
public abstract class Logger {

    /**
     * System property that tells which logger library to use for logging. This system property should be set when
     * starting Java (like {@code java ... -Dorg.freemarker.loggerLibrary=slf4j}), not later from Java
     * code. If the requested logging library is not available, an error will be printed to the stderr, then the logging
     * will be disabled.
     * 
     * <p>The supported values are:
     * <ul>
     *   <li>{@value #LIBRARY_NAME_SLF4J}: Use SLF4J. This is the recommended value.</li>
     *   <li>{@value #LIBRARY_NAME_COMMONS_LOGGING}: Use Apache commons-logging.</li>
     *   <li>{@value #LIBRARY_NAME_LOG4J}: Use Log4j.</li>
     *   <li>{@value #LIBRARY_NAME_JUL}: Use {@code java.util.logging}.</li>
     *   <li>{@value #LIBRARY_NAME_AVALON}: Use Avalon LogKit. deprecated!</li>
     *   <li>{@value #LIBRARY_NAME_AUTO}: Use auto-detection. This is the default behavior. See {@link Logger} class
     *       documentation for more.</li>
     *   <li>{@value #LIBRARY_NAME_NONE}: Don't log.</li>
     * </ul>
     */
    public static final String PROPERTY_NAME_LOGGER_LIBRARY = "org.freemarker.loggerLibrary";

    /**
     * Enum value used for {@link #selectLoggerLibrary(int)}; indicates auto-selection as described in the class
     * documentation of {@link #Logger()}.
     */
    public static final int LIBRARY_AUTO = -1;
    private static final int MIN_LIBRARY_ENUM = LIBRARY_AUTO;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates auto-selection as described in the class
     * documentation of {@link #Logger()}. @since 2.3.22
     */
    public static final String LIBRARY_NAME_AUTO = "auto";

    /**
     * Enum value used for {@link #selectLoggerLibrary(int)}; indicates that no logging should occur.
     */
    public static final int LIBRARY_NONE = 0;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates that no logging should occur. @since 2.3.22
     */
    public static final String LIBRARY_NAME_NONE = "none";

    /**
     * Enum value used for {@link #selectLoggerLibrary(int)}; indicates that {@code java.util.logging} should be used.
     */
    public static final int LIBRARY_JAVA = 1;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates that {@code java.util.logging} should be used. @since
     * 2.3.22
     */
    public static final String LIBRARY_NAME_JUL = "JUL";

    /**
     * Enum value used for {@link #selectLoggerLibrary(int)}; indicates that Avalon LogKit should be used.
     * @deprecated Avalon LogKit support will be removed sometimes in the future.
     */
    public static final int LIBRARY_AVALON = 2;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates that Avalon LogKit should be used. @since 2.3.22
     * @deprecated Avalon LogKit support will be removed sometimes in the future.
     */
    public static final String LIBRARY_NAME_AVALON = "Avalon";

    /**
     * Enum value used for {@link #selectLoggerLibrary(int)}; indicates that Log4J should be used.
     */
    public static final int LIBRARY_LOG4J = 3;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates that Log4J should be used. @since 2.3.22
     */
    public static final String LIBRARY_NAME_LOG4J = "Log4j";

    /**
     * Enum value used for {@link #selectLoggerLibrary(int)}; indicates that Apache commons-logging should be used.
     */
    public static final int LIBRARY_COMMONS = 4;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates that Apache commons-logging should be used. @since
     * 2.3.22
     */
    public static final String LIBRARY_NAME_COMMONS_LOGGING = "CommonsLogging";

    /**
     * Constant used with {@link #selectLoggerLibrary(int)}; indicates that SLF4J should be used.
     */
    public static final int LIBRARY_SLF4J = 5;

    /**
     * {@value #PROPERTY_NAME_LOGGER_LIBRARY} property value; indicates that SLF4J should be used. @since 2.3.22
     */
    public static final String LIBRARY_NAME_SLF4J = "SLF4J";
    private static final int MAX_LIBRARY_ENUM = LIBRARY_SLF4J;
    
    /**
     * Order matters! Starts with the lowest priority.
     */
    private static final String[] LIBRARIES_BY_PRIORITY = {
            "java.util.logging.Logger", LIBRARY_NAME_JUL,
            "org.apache.log.Logger", LIBRARY_NAME_AVALON,
            "org.apache.log4j.Logger", LIBRARY_NAME_LOG4J,
            /* In 2.3.x this two is skipped by LIBRARY_AUTO: */
            "org.apache.commons.logging.Log", LIBRARY_NAME_COMMONS_LOGGING,
            "org.slf4j.Logger", LIBRARY_NAME_SLF4J,
    };
    
    private static String getAvailabilityCheckClassName(int libraryEnum) {
        if (libraryEnum == LIBRARY_AUTO || libraryEnum == LIBRARY_NONE) {
            // Statically linked
            return null;
        }
        return LIBRARIES_BY_PRIORITY[(libraryEnum - 1) * 2];
    }
    static {
        if (LIBRARIES_BY_PRIORITY.length / 2 != MAX_LIBRARY_ENUM) {
            throw new AssertionError();
        }
    }

    private static String getLibraryName(int libraryEnum) {
        if (libraryEnum == LIBRARY_AUTO) {
            return LIBRARY_NAME_AUTO;
        }
        if (libraryEnum == LIBRARY_NONE) {
            return LIBRARY_NAME_NONE;
        }
        return LIBRARIES_BY_PRIORITY[(libraryEnum - 1) * 2 + 1];
    }

    private static boolean isAutoDetected(int libraryEnum) {
        // 2.4: Remove libraryEnum == LIBRARY_SLF4J || libraryEnum == LIBRARY_COMMONS
        return !(libraryEnum == LIBRARY_AUTO || libraryEnum == LIBRARY_NONE
                || libraryEnum == LIBRARY_SLF4J || libraryEnum == LIBRARY_COMMONS);
    }

    private static int libraryEnum;
    private static LoggerFactory loggerFactory;
    private static boolean initializedFromSystemProperty;

    private static String categoryPrefix = "";

    private static final Map loggersByCategory = new HashMap();

    /**
     * Selects the logger library to use, unless it's already specified by the {@value #PROPERTY_NAME_LOGGER_LIBRARY}
     * system property. Call this early in application initialization phase, before calling any other FreeMarker API-s,
     * since once parts of the FreeMarker library bind to the logging subsystem, the change in this value will have no
     * effect on them.
     * 
     * @param libraryEnum
     *            One of <tt>LIBRARY_...</tt> constants. By default, {@link #LIBRARY_AUTO} is used.
     *            
     * @throws ClassNotFoundException
     *             if an explicit logging library is asked for (that is, not {@value #LIBRARY_AUTO}
     *             or {@value #LIBRARY_NONE}) and it's not found in the classpath.
     * 
     * @deprecated This method isn't reliable, unless you can somehow ensure that you access the FreeMarker classes
     *             first; use the {@value #PROPERTY_NAME_LOGGER_LIBRARY} Java system property instead, like
     *             {@code java ... -Dorg.freemarker.loggerLibrary=slf4j}. See
     *             {@link #PROPERTY_NAME_LOGGER_LIBRARY} for more.
     */
    public static void selectLoggerLibrary(int libraryEnum) throws ClassNotFoundException {
        if (libraryEnum < MIN_LIBRARY_ENUM || libraryEnum > MAX_LIBRARY_ENUM) {
            throw new IllegalArgumentException("Library enum value out of range");
        }
        
        synchronized (Logger.class) {
            final boolean loggerFactoryAlreadySet = loggerFactory != null;
            if (!loggerFactoryAlreadySet || libraryEnum != Logger.libraryEnum) {
                // Creates the factory only if it can be done based on system property:
                ensureLoggerFactorySet(true);
    
                // The system property has precedence because this method was deprecated by it:
                if (!initializedFromSystemProperty || loggerFactory == null) {
                    setLibrary(libraryEnum);
                    loggersByCategory.clear();
                    if (loggerFactoryAlreadySet) {
                        logWarnInLogger("Logger libraray was already set earlier; "
                                + "change to \"" + getLibraryName(libraryEnum) + "\" won't effect loggers created "
                                + "earlier.");
                    }
                } else {
                    logWarnInLogger(
                            "Ignored " + Logger.class.getName() + ".selectLoggerLibrary(int) call, because the "
                            + PROPERTY_NAME_LOGGER_LIBRARY + " system property is set.");
                }
            }
        }
    }

    /**
     * Sets a category prefix. This prefix is prepended to any logger category name. This makes it possible to have
     * different FreeMarker logger categories on a per-application basis (better said, per-classloader basis). By
     * default the category prefix is the empty string. If you set a non-empty category prefix, be sure to include the
     * trailing separator dot (i.e. "MyApp.") If you want to change the default setting, do it early in application
     * initialization phase, before calling any other FreeMarker API since once various parts of the FreeMarker library
     * bind to the logging subsystem, the change in this value will have no effect on them.
     * 
     * @deprecated This wasn't reliable, unless you can somehow ensure that you access the FreeMarker classes first. As
     *             it's not known to be useful for users, consider it removed.
     */
    public static void setCategoryPrefix(String prefix)
    {
        synchronized (Logger.class) {
            if (prefix == null)
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
     * 
     * @param category
     *            a dot separated hierarchical category name. If a category prefix is in effect, it's prepended to the
     *            category name.
     */
    public static Logger getLogger(String category) {
        if (categoryPrefix.length() != 0) {
            category = categoryPrefix + category;
        }
        synchronized (loggersByCategory) {
            Logger logger = (Logger) loggersByCategory.get(category);
            if (logger == null) {
                ensureLoggerFactorySet(false);
                logger = loggerFactory.getLogger(category);
                loggersByCategory.put(category, logger);
            }
            return logger;
        }
    }

    private static void ensureLoggerFactorySet(boolean onlyIfCanBeSetFromSysProp) {
        if (loggerFactory != null) return;
        synchronized (Logger.class) {
            if (loggerFactory != null) return;

            String sysPropVal = getSystemProperty(PROPERTY_NAME_LOGGER_LIBRARY);
            if (onlyIfCanBeSetFromSysProp && sysPropVal == null) {
                return;
            }

            final int libraryEnum;
            final boolean invalidSysPropValWarning;
            if (sysPropVal != null) {
                sysPropVal = sysPropVal.trim();

                boolean foundMatch = false;
                int matchedEnum = MIN_LIBRARY_ENUM;
                do {
                    if (sysPropVal.equalsIgnoreCase(getLibraryName(matchedEnum))) {
                        foundMatch = true;
                    } else {
                        matchedEnum++;
                    }
                } while (matchedEnum <= MAX_LIBRARY_ENUM && !foundMatch);
                
                libraryEnum = foundMatch ? matchedEnum : LIBRARY_AUTO;
                invalidSysPropValWarning = !foundMatch;
            } else {
                libraryEnum = LIBRARY_AUTO;
                invalidSysPropValWarning = false;
            }
            
            try {
                setLibrary(libraryEnum);
            } catch (Throwable e) {
                logErrorInLogger(
                        "Couldn't set up logger for \"" + getLibraryName(libraryEnum) + "\"; logging disabled", e);
                try {
                    setLibrary(LIBRARY_NONE);
                } catch (ClassNotFoundException e2) {
                    throw new RuntimeException("Bug", e2);
                }
            }
            
            if (invalidSysPropValWarning) {
                logWarnInLogger("Ignored invalid " + PROPERTY_NAME_LOGGER_LIBRARY
                        + " system property value: " + sysPropVal);
            } else if (sysPropVal != null) {
                initializedFromSystemProperty = true;
            }
        }
    }

    /**
     * @throws ClassNotFoundException
     *             If the requested logger library is not available. Never thrown for {@link #LIBRARY_AUTO}.
     */
    private static LoggerFactory createLoggerFactory(int libraryEnum) throws ClassNotFoundException {
        if (libraryEnum == LIBRARY_AUTO) {
            for (int libraryEnumToTry = MAX_LIBRARY_ENUM; libraryEnumToTry >= MIN_LIBRARY_ENUM; libraryEnumToTry--) {
                if (!isAutoDetected(libraryEnumToTry)) continue;
                
                try {
                    return createLoggerFactoryForNonAuto(libraryEnumToTry);
                } catch (ClassNotFoundException e) {
                    // Expected, intentionally suppressed.
                } catch (Throwable e) {
                    logErrorInLogger(
                            "Unexpected error when initializing logging for \""
                            + getLibraryName(libraryEnumToTry) + "\".",
                            e);
                }
            }
            logWarnInLogger("Auto detecton haven't found any logger libraries; FreeMarker logging suppressed.");
            return new _NullLoggerFactory();
        } else {
            return createLoggerFactoryForNonAuto(libraryEnum);
        }
    }

    /**
     * @throws ClassNotFoundException If the required logger library is not available. 
     */
    private static LoggerFactory createLoggerFactoryForNonAuto(int libraryEnum) throws ClassNotFoundException {
        final String availabilityCheckClassName = getAvailabilityCheckClassName(libraryEnum);
        if (availabilityCheckClassName != null) {  // Dynamically created factory
            Class.forName(availabilityCheckClassName);
            String libraryName = getLibraryName(libraryEnum);
            try {
                return (LoggerFactory) Class.forName(
                        "freemarker.log._" + libraryName + "LoggerFactory").newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create logger factory for " + libraryName, e);
            }
        } else {  // Non-dynamically created factory
            if (libraryEnum == LIBRARY_JAVA) {
                return new _JULLoggerFactory();
            } else if (libraryEnum == LIBRARY_NONE) {
                return new _NullLoggerFactory();
            } else {
                throw new RuntimeException("Bug");
            }
        }
    }

    private synchronized static void setLibrary(int libraryEnum) throws ClassNotFoundException {
        loggerFactory = createLoggerFactory(libraryEnum);
        Logger.libraryEnum = libraryEnum;
    }

    private static void logWarnInLogger(String message) {
        logInLogger(false, message, null);
    }

    private static void logErrorInLogger(String message, Throwable exception) {
        logInLogger(true, message, exception);
    }

    private static void logInLogger(boolean error, String message, Throwable exception) {
        boolean canUseRealLogger;
        synchronized (Logger.class) {
            canUseRealLogger = loggerFactory != null && !(loggerFactory instanceof _NullLoggerFactory);
        }
    
        if (canUseRealLogger) {
            try {
                final Logger logger = Logger.getLogger("freemarker.logger");
                if (error) {
                    logger.error(message);
                } else {
                    logger.warn(message);
                }
            } catch (Throwable e) {
                canUseRealLogger = false;
            }
        }
    
        if (!canUseRealLogger) {
            System.err.println("*** " + (error ? "ERROR" : "WARN") + " "
                    + LoggerFactory.class.getName() + ": " + message);
            if (exception != null) {
                System.err.println("*** \tException: " + tryToString(exception));
                while (exception.getCause() != null) {
                    exception = exception.getCause();
                    System.err.println("*** \tCaused by: " + tryToString(exception));
                }
            }
        }
    }
    
    /**
     * Don't use {@link freemarker.template.utility.SecurityUtilities#getSystemProperty(String, String)} here, as it
     * (might) depends on the logger, hence interfering with the initialization.
     */
    public static String getSystemProperty(final String key) {
        try {
            return (String) AccessController.doPrivileged(
                new PrivilegedAction() {
                    public Object run() {
                        return System.getProperty(key, null);
                    }
                });
        } catch(Throwable e) {
            logErrorInLogger("Failed to read system property: " + key, e);
            return null;
        }
    }
    
    /**
     * Don't use {@link freemarker.template.utility.StringUtil#tryToString(Object)} here, as it
     * might depends on the logger, hence interfering with the initialization.
     */
    public static String tryToString(Object object) {
        if (object == null) return null;
        try {
            return object.toString();
        } catch (Throwable e) {
            return object.getClass().getName();
        }
    }
    
}
