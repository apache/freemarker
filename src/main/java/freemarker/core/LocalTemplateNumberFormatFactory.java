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

/**
 * Creates {@link TemplateNumberFormat}-s for a {@link Environment}. The same factory is used to create all the
 * {@link TemplateNumberFormat}-s of the same formatter type. Thus factories might want to cache instances internally
 * with the {@code params} as key.
 * 
 * <p>
 * {@link LocalTemplateDateFormatFactory}-es need not be thread-safe. Currently (2.3.24) they are (re)used only from
 * within a single {@link Environment} instance.
 * 
 * @since 2.3.24
 */
public abstract class LocalTemplateNumberFormatFactory {

    private final Environment env;
    private Locale locale;
    
    /**
     * @param env
     *            Can be {@code null} if the extending factory class doesn't care about the {@link Environment}.
     */
    public LocalTemplateNumberFormatFactory(Environment env) {
        this.env = env;
    }

    /**
     * @return When {@link #get(String)} is called, it must be already non-{@code null}.
     */
    public Locale getLocale() {
        return locale;
    }
    
    public void setLocale(Locale locale) {
        this.locale = locale;
        onLocaleChanged();
    }
    
    /**
     * Called after the locale was changed, or was initially set. This method should execute very fast; it's primarily
     * for invalidating caches. If anything long is needed, it should be postponed until a formatter is actually
     * requested. 
     */
    protected abstract void onLocaleChanged();

    public Environment getEnvironment() {
        return env;
    }
    
    /**
     * Returns a formatter for the given parameter. The returned formatter can be a new instance or a reused (cached)
     * instance.
     * 
     * <p>
     * The locale must be already set to non-{@code null} with {@link #setLocale(Locale)} before calling this method.
     * The returned formatter, if the locale matters for it, should be bound to the locale that was in effect when this
     * method was called.
     * 
     * @param params
     *            The string that further describes how the format should look. The format of this string is up to the
     *            {@link LocalTemplateDateFormatFactory} implementation. Note {@code null}, often an empty string.
     */
    public abstract TemplateNumberFormat get(String params)
            throws InvalidFormatParametersException;
    
}
