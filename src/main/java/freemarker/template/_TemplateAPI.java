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

package freemarker.template;

import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import freemarker.cache.CacheStorage;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.cache.TemplateNameFormat;
import freemarker.core.CFormat;
import freemarker.core.Expression;
import freemarker.core.OutputFormat;
import freemarker.core.TemplateObject;
import freemarker.log.Logger;
import freemarker.template.utility.NullArgumentException;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't.
 */
public class _TemplateAPI {
    // ATTENTION! Don't refer to other classes in the static initializer of this class! Fields that need that must be
    // moved into a separate class, to avoid class init deadlocks.

    public static void checkVersionNotNullAndSupported(Version incompatibleImprovements) {
        NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
        int iciV = incompatibleImprovements.intValue();
        if (iciV > Configuration.getVersion().intValue()) {
            throw new IllegalArgumentException("The FreeMarker version requested by \"incompatibleImprovements\" was "
                    + incompatibleImprovements + ", but the installed FreeMarker version is only "
                    + Configuration.getVersion() + ". You may need to upgrade FreeMarker in your project.");
        }
        if (iciV < _VersionInts.V_2_3_0) {
            throw new IllegalArgumentException("\"incompatibleImprovements\" must be at least 2.3.0.");
        }
    }

    /**
     * Checks if the object return by {@link Configuration#getVersion()} was used for setting
     * "incompatibleImprovements", which shouldn't be done.
     *
     * @since 2.3.30
     */
    public static void checkCurrentVersionNotRecycled(
            Version incompatibleImprovements,
            String logCategory, String configuredClassShortName) {
        if (incompatibleImprovements == Configuration.getVersion()) {
            Logger.getLogger(logCategory)
                    .error(configuredClassShortName + ".incompatibleImprovements was set to the object returned by " +
                            "Configuration.getVersion(). That defeats the purpose of incompatibleImprovements, " +
                            "and makes upgrading FreeMarker a potentially breaking change. Also, this " +
                            "probably won't be allowed starting from 2.4.0. Instead, set incompatibleImprovements to " +
                            "the highest concrete version that's known to be compatible with your application.");
        }
    }

    public static int getTemplateLanguageVersionAsInt(TemplateObject to) {
        return getTemplateLanguageVersionAsInt(to.getTemplate());
    }

    public static int getTemplateLanguageVersionAsInt(Template t) {
        return t.getTemplateLanguageVersion().intValue();
    }
    
    /** For unit testing only */
    public static void DefaultObjectWrapperFactory_clearInstanceCache() {
        DefaultObjectWrapperBuilder.clearInstanceCache();
    }
    
    public static TemplateExceptionHandler getDefaultTemplateExceptionHandler(
            Version incompatibleImprovements) {
        return Configuration.getDefaultTemplateExceptionHandler(incompatibleImprovements);
    }
    
    public static AttemptExceptionReporter getDefaultAttemptExceptionReporter(
            Version incompatibleImprovements) {
        return Configuration.getDefaultAttemptExceptionReporter(incompatibleImprovements);
    }
    
    public static boolean getDefaultLogTemplateExceptions(Version incompatibleImprovements) {
        return Configuration.getDefaultLogTemplateExceptions(incompatibleImprovements);
    }

    public static boolean getDefaultWrapUncheckedExceptions(Version incompatibleImprovements) {
        return Configuration.getDefaultWrapUncheckedExceptions(incompatibleImprovements);
    }
    
    public static TemplateLoader createDefaultTemplateLoader(Version incompatibleImprovements) {
        return Configuration.createDefaultTemplateLoader(incompatibleImprovements);
    }

    public static CacheStorage createDefaultCacheStorage(Version incompatibleImprovements) {
        return Configuration.createDefaultCacheStorage(incompatibleImprovements);
    }
    
    public static TemplateLookupStrategy getDefaultTemplateLookupStrategy(Version incompatibleImprovements) {
        return Configuration.getDefaultTemplateLookupStrategy(incompatibleImprovements);
    }
    
    public static TemplateNameFormat getDefaultTemplateNameFormat(Version incompatibleImprovements) {
        return Configuration.getDefaultTemplateNameFormat(incompatibleImprovements);
    }
    
    /**
     * [2.4] getSettingNames() becomes to public; remove this.
     */
    public static Set/*<String>*/ getConfigurationSettingNames(Configuration cfg, boolean camelCase) {
        return cfg.getSettingNames(camelCase);
    }
    
    public static void setAutoEscaping(Template t, boolean autoEscaping) {
        t.setAutoEscaping(autoEscaping);
    }
    
    public static void setOutputFormat(Template t, OutputFormat outputFormat) {
        t.setOutputFormat(outputFormat);
    }

    public static void validateAutoEscapingPolicyValue(int autoEscaping) {
        if (autoEscaping != Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY
                && autoEscaping != Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY
                && autoEscaping != Configuration.DISABLE_AUTO_ESCAPING_POLICY) {
            throw new IllegalArgumentException("\"auto_escaping\" can only be set to one of these: "
                    + "Configuration.ENABLE_AUTO_ESCAPING_IF_DEFAULT, "
                    + "or Configuration.ENABLE_AUTO_ESCAPING_IF_SUPPORTED"
                    + "or Configuration.DISABLE_AUTO_ESCAPING");
        }
    }

    public static void validateNamingConventionValue(int namingConvention) {
        if (namingConvention != Configuration.AUTO_DETECT_NAMING_CONVENTION
            && namingConvention != Configuration.LEGACY_NAMING_CONVENTION
            && namingConvention != Configuration.CAMEL_CASE_NAMING_CONVENTION) {
            throw new IllegalArgumentException("\"naming_convention\" can only be set to one of these: "
                    + "Configuration.AUTO_DETECT_NAMING_CONVENTION, "
                    + "or Configuration.LEGACY_NAMING_CONVENTION"
                    + "or Configuration.CAMEL_CASE_NAMING_CONVENTION");
        }
    }

    public static void valideTagSyntaxValue(int tagSyntax) {
        if (tagSyntax != Configuration.AUTO_DETECT_TAG_SYNTAX
            && tagSyntax != Configuration.SQUARE_BRACKET_TAG_SYNTAX
            && tagSyntax != Configuration.ANGLE_BRACKET_TAG_SYNTAX) {
            throw new IllegalArgumentException("\"tag_syntax\" can only be set to one of these: "
                    + "Configuration.AUTO_DETECT_TAG_SYNTAX, Configuration.ANGLE_BRACKET_TAG_SYNTAX, "
                    + "or Configuration.SQUARE_BRACKET_TAG_SYNTAX");
        }
    }

    public static void valideInterpolationSyntaxValue(int interpolationSyntax) {
        if (interpolationSyntax != Configuration.LEGACY_INTERPOLATION_SYNTAX
            && interpolationSyntax != Configuration.DOLLAR_INTERPOLATION_SYNTAX
            && interpolationSyntax != Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX) {
            throw new IllegalArgumentException("\"interpolation_syntax\" can only be set to one of these: "
                    + "Configuration.LEGACY_INTERPOLATION_SYNTAX, Configuration.DOLLAR_INTERPOLATION_SYNTAX, "
                    + "or Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX");
        }
    }
    
    public static Expression getBlamedExpression(TemplateException e) {
        return e.getBlamedExpression();
    }
    
    public static Locale getDefaultLocale() {
        return Configuration.getDefaultLocale();
    }

    public static TimeZone getDefaultTimeZone() {
        return Configuration.getDefaultTimeZone();
    }

    public static CFormat getDefaultCFormat(Version incompatibleImprovements) {
        return Configuration.getDefaultCFormat(incompatibleImprovements);
    }

    public static void setPreventStrippings(Configuration conf, boolean preventStrippings) {
        conf.setPreventStrippings(preventStrippings);
    }
    
}
