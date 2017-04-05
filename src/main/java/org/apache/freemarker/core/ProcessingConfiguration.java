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

package org.apache.freemarker.core;

import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

/**
 * <b>Don't implement this interface yourself</b>; use the existing implementation(s). This interface is implemented by
 * classes that hold settings that affect {@linkplain Template#process(Object, Writer) template processing} (as opposed
 * to template parsing). New parser settings can be added in new FreeMarker versions, which will break your
 * implementation.
 *
 * @see ParserConfiguration
 */
// TODO [FM3] JavaDoc
public interface ProcessingConfiguration {

     /**
     * Getter pair of {@link MutableProcessingConfiguration#setLocale(Locale)}.
     */
    Locale getLocale();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isLocaleSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setTimeZone(TimeZone)}.
     */
    TimeZone getTimeZone();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isTimeZoneSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setSQLDateAndTimeTimeZone(TimeZone)}.
     *
     * @return {@code null} if the value of {@link #getTimeZone()} should be used for formatting {@link java.sql.Date
     * java.sql.Date} and {@link java.sql.Time java.sql.Time} values, otherwise the time zone that should be used to
     * format the values of those two types.
     */
    TimeZone getSQLDateAndTimeTimeZone();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isSQLDateAndTimeTimeZoneSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setNumberFormat(String)}.
     */
    String getNumberFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isNumberFormatSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setCustomNumberFormats(Map)}.
     */
    Map<String, TemplateNumberFormatFactory> getCustomNumberFormats();

    TemplateNumberFormatFactory getCustomNumberFormat(String name);

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isCustomNumberFormatsSet();

    boolean hasCustomFormats();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setBooleanFormat(String)}.
     */
    String getBooleanFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isBooleanFormatSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setTimeFormat(String)}.
     */
    String getTimeFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isTimeFormatSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setDateFormat(String)}.
     */
    String getDateFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isDateFormatSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setDateTimeFormat(String)}.
     */
    String getDateTimeFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isDateTimeFormatSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setCustomDateFormats(Map)}.
     */
    Map<String, TemplateDateFormatFactory> getCustomDateFormats();

    TemplateDateFormatFactory getCustomDateFormat(String name);

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isCustomDateFormatsSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setTemplateExceptionHandler(TemplateExceptionHandler)}.
     */
    TemplateExceptionHandler getTemplateExceptionHandler();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isTemplateExceptionHandlerSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setArithmeticEngine(ArithmeticEngine)}.
     */
    ArithmeticEngine getArithmeticEngine();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isArithmeticEngineSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setObjectWrapper(ObjectWrapper)}.
     */
    ObjectWrapper getObjectWrapper();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isObjectWrapperSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setOutputEncoding(Charset)}.
     */
    Charset getOutputEncoding();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isOutputEncodingSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setURLEscapingCharset(Charset)}.
     */
    Charset getURLEscapingCharset();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isURLEscapingCharsetSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setNewBuiltinClassResolver(TemplateClassResolver)}.
     */
    TemplateClassResolver getNewBuiltinClassResolver();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isNewBuiltinClassResolverSet();

    boolean getAPIBuiltinEnabled();

    boolean isAPIBuiltinEnabledSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setAutoFlush(boolean)}.
     */
    boolean getAutoFlush();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isAutoFlushSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setShowErrorTips(boolean)}.
     */
    boolean getShowErrorTips();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isShowErrorTipsSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setLogTemplateExceptions(boolean)}.
     */
    boolean getLogTemplateExceptions();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isLogTemplateExceptionsSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setLazyImports(boolean)}.
     */
    boolean getLazyImports();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isLazyImportsSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setLazyAutoImports(Boolean)}.
     */
    Boolean getLazyAutoImports();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isLazyAutoImportsSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setAutoImports(Map)}.
     */
    Map<String, String> getAutoImports();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isAutoImportsSet();

    /**
     * Getter pair of {@link MutableProcessingConfiguration#setAutoIncludes(List)}.
     */
    List<String> getAutoIncludes();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isAutoIncludesSet();

    Map<Object, Object> getCustomAttributes();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isCustomAttributesSet();

    Object getCustomAttribute(Object name);

}
