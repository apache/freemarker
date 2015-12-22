package freemarker.core;

/**
 * Represents the JSON output format (MIME type "application/json", name "JSON"). This format doesn't support escaping.
 * 
 * @since 2.3.24
 */
public class JSONOutputFormat extends OutputFormat {

    /**
     * The only instance (singleton) of this {@link OutputFormat}.
     */
    public static final JSONOutputFormat INSTANCE = new JSONOutputFormat();
    
    private JSONOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public String getName() {
        return "JSON";
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }

}
