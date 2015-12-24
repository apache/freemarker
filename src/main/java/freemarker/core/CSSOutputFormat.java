package freemarker.core;

/**
 * Represents the CSS output format (MIME type "text/css", name "CSS"). This format doesn't support escaping.
 * 
 * @since 2.3.24
 */
public class CSSOutputFormat extends OutputFormat {

    /**
     * The only instance (singleton) of this {@link OutputFormat}.
     */
    public static final CSSOutputFormat INSTANCE = new CSSOutputFormat();
    
    private CSSOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public String getName() {
        return "CSS";
    }

    @Override
    public String getMimeType() {
        return "text/css";
    }

    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }

}
