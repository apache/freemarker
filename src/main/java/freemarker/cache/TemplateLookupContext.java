package freemarker.cache;

import java.io.IOException;
import java.util.Locale;

import freemarker.template.Configuration;

/**
 * Used as the parameter of {@link TemplateLookupStrategy#lookup(TemplateLookupContext)}.
 * 
 * @since 2.3.22
 */
public abstract class TemplateLookupContext {
    
    private final String templateName;
    private final Locale templateLocale;

    /**
     * Finds the template source by considering {@code *} steps (so called acquisition) in the parameter name; otherwise
     * it just calls {@link TemplateLoader#findTemplateSource(String)}.
     * 
     * @return The template source or {@code null} if the template doesn't exist.
     */
    public abstract TemplateLookupResult lookupWithAcquisitionStrategy(String name) throws IOException;

    /** Default visibility to prevent extending the class from outside this package. */
    TemplateLookupContext(String templateName, Locale templateLocale) {
        this.templateName = templateName;
        this.templateLocale = templateLocale;
    }

    /**
     * The name (path) of the template (relatively to the {@link TemplateLoader}). Not {@code null}. 
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * {@code null} if {@link Configuration#getLocalizedLookup()} is {@code false}, otherwise the locale requested.
     */
    public Locale getTemplateLocale() {
        return templateLocale;
    }
    
}
