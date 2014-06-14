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

package freemarker.template;

import java.util.Locale;

import freemarker.core.Environment;

/**
 * An abstract base class for scalars that vary by locale.
 * Here is a silly usage example.
 * <code>
 *    TemplateScalarModel localizedYes = new LocalizedString() {
 *        public String getLocalizedString(java.util.Locale locale) {
 *            String lang = locale.getLanguage();
 *            if "fr".equals(lang)
 *               return "oui";
 *            else if "de".equals(lang)
 *               return "sí";
 *            else
 *               return "yes";
 *        }  
 *    };
 * </code>
 */

abstract public class LocalizedString implements TemplateScalarModel {
	
	
	public String getAsString() throws TemplateModelException {
		Environment env = Environment.getCurrentEnvironment();
		Locale locale = env.getLocale();
		return getLocalizedString(locale);
	}
	
	abstract public String getLocalizedString(Locale locale) throws TemplateModelException;
}
