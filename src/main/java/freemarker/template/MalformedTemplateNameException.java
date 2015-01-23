package freemarker.template;

import java.io.IOException;

import freemarker.cache.TemplateNameFormat;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that the template name given was malformed according the {@link TemplateNameFormat} in use. Note that for
 * backward compatibility, {@link TemplateNameFormat#DEFAULT_2_3_0} doesn't throw this error,
 * {@link TemplateNameFormat#DEFAULT_2_4_0} does. This exception extends {@link IOException} for backward compatibility.
 * 
 * @since 2.3.22
 */
public class MalformedTemplateNameException extends IOException {
    
    private final String templateName;
    private final String malformednessDescription;

    public MalformedTemplateNameException(String templateName, String malformednessDescription) {
        super("Malformed template name, " + StringUtil.jQuote(templateName) + ": " + malformednessDescription);
        this.templateName = templateName;
        this.malformednessDescription = malformednessDescription;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getMalformednessDescription() {
        return malformednessDescription;
    }

}
