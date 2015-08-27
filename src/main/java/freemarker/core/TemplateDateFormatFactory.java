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

import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.Configuration;

/**
 * Factory for a certain type of date/time/dateTime formatting ({@link TemplateDateFormat}). Usually a singleton
 * (one-per-VM or one-per-{@link Configuration}), and so must be thread-safe. It doesn't create
 * {@link TemplateNumberFormat} directly, instead it creates {@link LocalizedTemplateDateFormatFactory}-s which are
 * single-thread locale-bound objects that provide the actual {@link TemplateDateFormat}-s based on the provided
 * parameters (like possibly a pattern string).
 * 
 * @since 2.3.24
 */
public abstract class TemplateDateFormatFactory {
    
    public abstract LocalizedTemplateDateFormatFactory getLocalizedFactory(Environment env, Locale locale, TimeZone tz);

}
