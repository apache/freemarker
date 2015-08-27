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

/**
 * Creates {@link TemplateNumberFormat}-s for a fixed locale (if it producers formatters that are sensitive to locale).
 * Typically, within the same {@link Environment}, the same factory is used to create all the
 * {@link TemplateNumberFormat}-s of the same formatter type, as far as the locale remains the same. Thus factories
 * might want to cache instances internally with the {@code formatDescriptor} as key.
 * 
 * <p>
 * {@link LocalizedTemplateDateFormatFactory}-es need not be thread-safe. Currently (2.3.24) they are (re)used only from
 * within a single {@link Environment} instance.
 * 
 * @since 2.3.24
 */
public abstract class LocalizedTemplateNumberFormatFactory {

}
