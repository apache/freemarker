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

import java.text.DateFormat;
import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

/**
 * Represents a date/time/dateTime format; used in templates for formatting and parsing with that format.
 * This is similar to Java's {@link DateFormat}, but made to fit the requirements of FreeMarker. Also, it makes
 * easier to define formats that can't be represented with Java's existing {@link DateFormat} implementations.
 * 
 * <p>Implementations need not be thread-safe. Usually, instances are bound to a single {@link Environment}, and
 * {@link Environment}-s are thread-local objects. As the {@link Environment} is recreated for each top-level template
 * processing, constructing these object should be cheap, or else the factory of the instances should do some caching.
 */
// [Advanced formatting: planned public]
abstract class TemplateDateFormat {
    
    /**
     * @param dateModel The date/time/dateTime to format. Most implementations will just work with the return value of
     *          {@link TemplateDateModel#getAsDate()}, but some may format differently depending on the properties of
     *          a custom {@link TemplateDateModel} implementation.
     *          
     * @return The date/time/dateTime as text, with no escaping (like no HTML escaping). Can't be {@code null}.
     * 
     * @throws UnformattableDateException When a {@link TemplateDateModel} can't be formatted because of the
     *           value/properties of the {@link TemplateDateModel}. The most often used subclass is
     *           {@link UnknownDateTypeFormattingUnsupportedException}. 
     * @throws TemplateModelException Exception thrown by the {@code dateModel} object when calling its methods.  
     */
    public abstract String format(TemplateDateModel dateModel)
            throws UnformattableDateException, TemplateModelException;

    /**
     * @return The interpretation of the text as {@link Date}. Can't be {@code null}.
     */
    public abstract Date parse(String s) throws java.text.ParseException;

    /**
     * Meant to be used in error messages to tell what format the parsed string didn't fit.
     */
    public abstract String getDescription();
    
    // This isn't used yet, as we don't have markup formatting in the template language.
    ///**
    // * Formats the date to markup instead of to plain text, or return {@code false} that will make FreeMarker call
    // * {@link #format(TemplateDateModel)} and escape its result. It must not write into {@code out} when it returns
    // * {@code false}!
    // * It should only write to {@code out} and return {@code true} if the markup format is not the same as the
    // * {@link #format(TemplateDateModel)} escaped.
    // */
    //public boolean formatAsMarkup(TemplateDateModel dateModel, boolean zonelessInput, Writer out) {
    //    return false;
    //}
    
    /**
     * Tells if this formatter should be re-created if the locale changes.
     */
    public abstract boolean isLocaleBound();

}
