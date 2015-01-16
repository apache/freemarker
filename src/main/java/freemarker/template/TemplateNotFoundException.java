package freemarker.template;

import java.io.FileNotFoundException;

/**
 * Thrown when {@link Configuration#getTemplate(String)} (or similar) doesn't find a template.
 * This extends {@link FileNotFoundException} for backward compatibility, but in fact has nothing to do with files, as
 * FreeMarker can load templates from many other sources.
 *
 * @since 2.3.22
 */
public final class TemplateNotFoundException extends FileNotFoundException {
    
    private final String templateName;
    private final Object customLookupCondition;

    public TemplateNotFoundException(String templateName, Object customLookupCondition, String message) {
        super(message);
        this.templateName = templateName;
        this.customLookupCondition = customLookupCondition;
    }

    /**
     * The name (path) of the template that wasn't found.
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * The custom lookup condition with which the template was requested, or {@code null} if there's no such condition.
     * See the {@code customLookupCondition} parameter of
     * {@link Configuration#getTemplate(String, java.util.Locale, Object, String, boolean, boolean)}.
     */
    public Object getCustomLookupCondition() {
        return customLookupCondition;
    }

}
