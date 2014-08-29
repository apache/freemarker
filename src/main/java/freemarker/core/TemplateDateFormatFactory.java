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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;

/**
 * Creates {@link TemplateDateFormat}-s for a fixed time zone, and if it producers formatters that are sensitive
 * to locale, for a fixed locale. Thus, FreeMarker should maintain a separate instance for each time zone that's
 * frequently used, or if {@link #isLocaleBound()} is {@code true}, for each {@link TimeZone}-{@link Locale}
 * permutation that's frequently used. Reusing the factories is useful as some factories cache instances internally for
 * the {@code dateType}-{@code formatDescriptor} pairs.
 * 
 * <p>{@link TemplateDateFormatFactory}-es need not be thread-safe. Currently (2.3.21) they are (re)used only from
 * within a single {@link Environment} instance.
 */
abstract class TemplateDateFormatFactory {
    
    private final TimeZone timeZone;
    
    public TemplateDateFormatFactory(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Whether this factory is sensitive to {@link Locale}; if the created {@link TemplateDateFormat}-s are, then
     * the factory should be too {@code true}.   
     */
    public abstract boolean isLocaleBound();
    
    /**
     * Returns the {@link TemplateDateFormat} for the {@code dateType} and {@code formatDescriptor} given via the
     * arguments, and the {@code TimeZone} and {@code Locale} (if that's relevant) to which the
     * {@link TemplateDateFormatFactory} belongs to.
     * 
     * @param dateType {@line TemplateDateModel#DATE}, {@line TemplateDateModel#TIME},
     *         {@line TemplateDateModel#DATETIME} or {@line TemplateDateModel#UNKNOWN}. Supporting
     *         {@line TemplateDateModel#UNKNOWN} is not necessary, in which case the method should throw an 
     *         {@link UnknownDateTypeFormattingUnsupportedException} exception.
     *         
     * @param zonelessInput Indicates that the input Java {@link Date} is not from a time zone aware source.
     *         When this is {@code true}, the formatters shouldn't override the time zone provided to its
     *         constructor or factory method (most formatters don't do that anyway), and it shouldn't show the time
     *         zone, if it can hide it (like a {@link SimpleDateFormat} pattern-based formatter may can't do that, as
     *         the pattern prescribes what to show).
     *          
     *         <p>As of FreeMarker 2.3.21, this is {@code true} exactly when the date is an SQL "date
     *         without time of the day" (i.e., a {@link java.sql.Date java.sql.Date}) or an SQL "time of the day" value
     *         (i.e., a {@link java.sql.Time java.sql.Time}, although this rule can change in future, depending on
     *         configuration settings and such, so you should rely on this rule, just accept what this parameter says.
     *         
     * @param formatDescriptor The string used as {@code ..._format} the configuration setting value (among others),
     *         like {@code "iso m"} or {@code "dd.MM.yyyy HH:mm"}. The implementation is only supposed to
     *         understand a particular kind of format descriptor, for which FreeMarker routes to this factory.
     *         (Like, the {@link ISOTemplateDateFormatFactory} is only called for format descriptors that start with
     *         "iso".)
     *         
     * @throws ParseException if the {@code formatDescriptor} is malformed
     * @throws TemplateModelException if the {@code dateType} is unsupported by the formatter
     * @throws UnknownDateTypeFormattingUnsupportedException if {@code dateType} is {@line TemplateDateModel#UNKNOWN},
     *         and that's unsupported by the formatter implementation.
     */
    public abstract TemplateDateFormat get(int dateType, boolean zonelessInput, String formatDescriptor)
            throws java.text.ParseException, TemplateModelException, UnknownDateTypeFormattingUnsupportedException;
    
}
