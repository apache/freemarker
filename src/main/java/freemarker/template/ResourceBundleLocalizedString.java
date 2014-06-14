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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A concrete implementation of {@link LocalizedString} that gets 
 * a localized string from a {@link java.util.ResourceBundle}  
 */

public class ResourceBundleLocalizedString extends LocalizedString {
	
	private String resourceKey, resourceBundleLookupKey;
	
	/**
	 * @param resourceBundleLookupKey The lookup key for the resource bundle
	 * @param resourceKey the specific resource (assumed to be a string) to fish out of the resource bundle
	 */
	
	public ResourceBundleLocalizedString(String resourceBundleLookupKey, String resourceKey) { 
		this.resourceBundleLookupKey = resourceBundleLookupKey;
		this.resourceKey = resourceKey;
	}

	public String getLocalizedString(Locale locale) throws TemplateModelException {
		try {
			ResourceBundle rb = ResourceBundle.getBundle(resourceBundleLookupKey, locale);
			return rb.getString(resourceKey);
		}
		catch (MissingResourceException mre) {
			throw new TemplateModelException("missing resource", mre);
		}
	}
}
