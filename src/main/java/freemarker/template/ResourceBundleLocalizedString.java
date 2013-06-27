package freemarker.template;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A concrete implementation of {@link LocalizedString} that gets 
 * a localized string from a {@link java.util.ResourceBundle}  
 * @author Jonathan Revusky
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
