package freemarker.core;

import freemarker.template.TemplateOutputModel;

/**
 * Stores HTML output. Thread-safe after proper publishing. Calculated fields (typically, the markup calculated from
 * plain text) might be re-calculated for multiple times if accessed from multiple threads (this only affects
 * performance, not functionality).
 * 
 * @since 2.3.24
 */
public final class HTMLTemplateOutputModel implements TemplateOutputModel<HTMLTemplateOutputModel> {
    
    private String plainTextContent;
    private String markupContet;

    /**
     * A least one of the parameters must be non-{@code null}! 
     */
    HTMLTemplateOutputModel(String plainTextContent, String markupContet) {
        this.plainTextContent = plainTextContent;
        this.markupContet = markupContet;
    }

    public HTMLOutputFormat getOutputFormat() {
        return HTMLOutputFormat.INSTANCE;
    }

    /** Maybe {@code null}, but then the other field isn't {@code null}. */
    String getPlainTextContent() {
        return plainTextContent;
    }

    /** Maybe {@code null}, but then the other field isn't {@code null}. */
    String getMarkupContent() {
        return markupContet;
    }
    
    /** Use only to set {@code null} field to the value calculated from the other field! */
    public void setPlainTextContent(String plainTextContent) {
        this.plainTextContent = plainTextContent;
    }
    
    /** Use only to set {@code null} field to the value calculated from the other field! */
    public void setMarkupContet(String markupContet) {
        this.markupContet = markupContet;
    }

}
