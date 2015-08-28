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

package freemarker.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;

/**
 * Creates {@link TemplateDateFormat}-s for a fixed {@link Environment}, and thus for as single thread. Typically, the
 * same factory will be used to create all the {@link TemplateDateFormat}-s of the same formatter type. Thus factories
 * might want to cache instances internally with the {@code #get(int, boolean, String)} parameters as the key.
 * 
 * <p>
 * Note that currently (2.3.24) FreeMarker maintains a separate factory instance for the normal and the SQL time zone,
 * if they differ. As SQL and non-SQL values my occur mixed, keeping both factories can avoid cache flushes
 * do to {@link #setTimeZone(TimeZone)} calls inside the factory.
 * 
 * <p>
 * {@link LocalTemplateDateFormatFactory}-es need not be thread-safe as they are bound to a a single {@link Environment}
 * instance.
 * 
 * @since 2.3.24
 */
public abstract class LocalTemplateDateFormatFactory {
    
    private final Environment env;
    private TimeZone timeZone;
    private Locale locale;
    
    /**
     * @param env
     *            Can be {@code null} if the extending factory class doesn't care about the {@link Environment}.
     */
    public LocalTemplateDateFormatFactory(Environment env) {
        this.env = env;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        onLocaleChanged();
    }
    
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        onTimeZoneChanged();
    }
    
    /**
     * Called after the locale was changed, or was initially set. This method should execute very fast; it's primarily
     * for invalidating caches. If anything long is needed, it should be postponed until a formatter is actually
     * requested. 
     */
    protected abstract void onLocaleChanged();
    
    /**
     * Called after the time zone was changed, or was initially set. This method should execute very fast; it's
     * primarily for invalidating caches. If anything long is needed, it should be postponed until a formatter is
     * actually requested.
     */
    protected abstract void onTimeZoneChanged();
    
    public Environment getEnvironment() {
        return env;
    }

    /**
     * @return When {@link #get} is called, it must be already non-{@code null}.
     */
    public final TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * @return When {@link #get} is called, it must be already non-{@code null}.
     */
    public final Locale getLocale() {
        return locale;
    }
    
    /**
     * Currently always returns {@code true}; this method exists to reserve the place for a non-final version later.
     */
    public final boolean isTimeZoneBound() {
        return true;
    }

    /**
     * Whether this the {@link TemplateDateFormat} created by this factory are sensitive about {@link Locale}. Note that
     * even if it's not sensitive to it, it can be still sensitive to the time-zone, so this factory is still maybe
     * "localized".
     */
    public abstract boolean isLocaleBound();
    
    /**
     * Returns the {@link TemplateDateFormat} for the {@code dateType} and {@code formatDescriptor} given via the
     * arguments. The returned formatter can be a new instance or a reused (cached) instance.
     * 
     * <p>
     * The locale and time zone must be already set to non-{@code null} with {@link #setLocale(Locale)} and
     * {@link #setTimeZone(TimeZone)} before calling this method. The returned formatter, if the locale or time zone
     * matters for it, should be bound to the locale and time zone that was in effect when this method was called.
     * 
     * @param dateType
     *            {@line TemplateDateModel#DATE}, {@line TemplateDateModel#TIME}, {@line TemplateDateModel#DATETIME} or
     *            {@line TemplateDateModel#UNKNOWN}. Supporting {@line TemplateDateModel#UNKNOWN} is not necessary, in
     *            which case the method should throw an {@link UnknownDateTypeFormattingUnsupportedException} exception.
     * 
     * @param zonelessInput
     *            Indicates that the input Java {@link Date} is not from a time zone aware source. When this is
     *            {@code true}, the formatters shouldn't override the time zone provided to its constructor (most
     *            formatters don't do that anyway), and it shouldn't show the time zone, if it can hide it (like a
     *            {@link SimpleDateFormat} pattern-based formatter may can't do that, as the pattern prescribes what to
     *            show).
     * 
     *            <p>
     *            As of FreeMarker 2.3.21, this is {@code true} exactly when the date is an SQL "date without time of
     *            the day" (i.e., a {@link java.sql.Date java.sql.Date}) or an SQL "time of the day" value (i.e., a
     *            {@link java.sql.Time java.sql.Time}, although this rule can change in future, depending on
     *            configuration settings and such, so you should rely on this rule, just accept what this parameter
     *            says.
     * 
     * @param formatDescriptor
     *            The string used as the {@code ..._format} configuration setting value (among others), like
     *            {@code "iso m"} or {@code "dd.MM.yyyy HH:mm"}. The implementation is only supposed to understand a
     *            particular kind of format descriptor, for which FreeMarker routes to this factory. (Like, the
     *            {@link ISOLocalTemplateDateFormatFactory} is only called for format descriptors that start with
     *            "iso".)
     * 
     * @throws InvalidFormatDescriptorException
     *             if the {@code formatDescriptor} is malformed
     * @throws TemplateModelException
     *             if the {@code dateType} is unsupported by the formatter
     * @throws UnknownDateTypeFormattingUnsupportedException
     *             if {@code dateType} is {@line TemplateDateModel#UNKNOWN}, and that's unsupported by the formatter
     *             implementation.
     */
    public abstract TemplateDateFormat get(int dateType, boolean zonelessInput, String formatDescriptor)
            throws TemplateModelException, UnknownDateTypeFormattingUnsupportedException,
            InvalidFormatDescriptorException;
    
}
