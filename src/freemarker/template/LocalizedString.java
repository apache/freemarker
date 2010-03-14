package freemarker.template;

import freemarker.core.Environment;
import java.util.Locale;

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
 * @author Jonathan Revusky
 */

abstract public class LocalizedString implements TemplateScalarModel {
	
	
	public String getAsString() throws TemplateModelException {
		Environment env = Environment.getCurrentEnvironment();
		Locale locale = env.getLocale();
		return getLocalizedString(locale);
	}
	
	abstract public String getLocalizedString(Locale locale) throws TemplateModelException;
}
