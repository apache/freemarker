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

    public TemplateNotFoundException(String templateName, String message) {
        super(message);
        this.templateName = templateName;
    }

    /**
     * The name (path) of the template that wasn't found.
     */
    public String getTemplateName() {
        return templateName;
    }

}
