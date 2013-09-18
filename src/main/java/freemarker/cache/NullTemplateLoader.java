package freemarker.cache;

import java.io.IOException;
import java.io.Reader;

import freemarker.template.Configuration;

/**
 * A {@link TemplateLoader} that doesn't load anything, saying it doesn't exist. This is used in case you want to
 * prevent loading templates, also as the default template loader if the <tt>incompatible_improvements</tt> setting of
 * the {@link Configuration} is at least 2.3.21.   
 * 
 * @since 2.3.21
 */
public class NullTemplateLoader implements TemplateLoader {
    
    /**
     * The only instance of this class.
     */
    public static final NullTemplateLoader INSTANCE = new NullTemplateLoader();  

    private static final String INVALID_TEMPLATE_SOURCE_MESSAGE
            = "A NullTemplateLoader couldn't issue the argument templateSource.";

    private NullTemplateLoader() { }  // Can't be instantiated from outside
    
    /**
     * Always returns {@code null} (means: template not found).
     */
    public Object findTemplateSource(String name) throws IOException {
        return null;
    }

    /**
     * Always throws {@link RuntimeException}, as the {@code templateSource} couldn't come from this
     * {@link TemplateLoader}.
     */
    public long getLastModified(Object templateSource) {
        throw new RuntimeException(INVALID_TEMPLATE_SOURCE_MESSAGE);
    }

    /**
     * Always throws {@link RuntimeException}, as the {@code templateSource} couldn't come from this
     * {@link TemplateLoader}.
     */
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        throw new RuntimeException(INVALID_TEMPLATE_SOURCE_MESSAGE);
    }

    /**
     * Always throws {@link RuntimeException}, as the {@code templateSource} couldn't come from this
     * {@link TemplateLoader}.
     */
    public void closeTemplateSource(Object templateSource) throws IOException {
        throw new RuntimeException(INVALID_TEMPLATE_SOURCE_MESSAGE);
    }

}
