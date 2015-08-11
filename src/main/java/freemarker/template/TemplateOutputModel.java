package freemarker.template;

import freemarker.core.OutputFormat;

/**
 * "template output" template language data-type; stores "markup" (some kind of "rich text" / structured format), as
 * opposed to plain text. This type is related to the {@link Configuration#setOutputFormat(String)}/
 * {@link Configuration#setAutoEscaping(boolean)} mechanism. Values of this type are exempt from automatic escaping with
 * that mechanism.
 * 
 * @param <TOM>
 *            Refers to the interface's own type, which is useful in interfaces that extend {@link TemplateOutputModel}
 *            (Java Generics trick).
 * 
 * @since 2.3.24
 */
public interface TemplateOutputModel<TOM extends TemplateOutputModel<TOM>>  {

    OutputFormat<TOM> getOutputFormat();
    
}
