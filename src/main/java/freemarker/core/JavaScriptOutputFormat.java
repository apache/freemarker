package freemarker.core;

/**
 * Represents the JavaScript output format (MIME type "application/javascript", name "JavaScript"). This format doesn't
 * support escaping.
 * 
 * @since 2.3.24
 */
public class JavaScriptOutputFormat extends OutputFormat {

    /**
     * The only instance (singleton) of this {@link OutputFormat}.
     */
    public static final JavaScriptOutputFormat INSTANCE = new JavaScriptOutputFormat();
    
    private JavaScriptOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public String getName() {
        return "JavaScript";
    }

    @Override
    public String getMimeType() {
        return "application/javascript";
    }

    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }

}
