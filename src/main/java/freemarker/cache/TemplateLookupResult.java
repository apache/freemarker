package freemarker.cache;

import freemarker.template.Template;
import freemarker.template.utility.NullArgumentException;

/**
 * The return value of {@link TemplateLookupStrategy#lookup(TemplateLookupContext)} and similar lookup methods. You
 * usually get one from {@link TemplateLookupContext#lookupWithAcquisitionStrategy(String)} or
 * {@link TemplateLookupContext#createNegativeLookupResult()}; you can't create instances of this directly.
 * 
 * @since 2.3.22
 */
public abstract class TemplateLookupResult {

    /** Used internally to get a not-found result (currently just a static singleton). */
    static TemplateLookupResult createNegativeResult() {
        return NegativeTemplateLookupResult.INSTANCE;
    }
    
    /** Used internally to create the appropriate kind of result from the parameters. */
    static TemplateLookupResult from(String templateSourceName, Object templateSource) {
        return templateSource != null
                ? new PositiveTemplateLookupResult(templateSourceName, templateSource)
                : createNegativeResult();
    }
    
    private TemplateLookupResult() {
        // nop
    }
    
    /**
     * The source name of the template found (see {@link Template#getSourceName()}), or {@code null} if
     * {@link #isPositive()} is {@code false}.
     */
    public abstract String getTemplateSourceName();

    /**
     * Tells if the lookup has found a matching template.
     */
    public abstract boolean isPositive();

    /**
     * Used internally to extract the {@link TemplateLoader} source; {@code null} if
     * {@link #isPositive()} is {@code false}.
     */
    abstract Object getTemplateSource();

    private static final class PositiveTemplateLookupResult extends TemplateLookupResult {

        private final String templateSourceName;
        private final Object templateSource;

        /**
         * @param templateSourceName
         *            The name of the matching template found. This is not necessarily the same as the template name
         *            with which the template was originally requested. For example, one may gets a template for the
         *            {@code "foo.ftl"} name, but due to localized lookup the template is actually loaded from
         *            {@code "foo_de.ftl"}. Then this parameter must be {@code "foo_de.ftl"}, not {@code "foo.ftl"}. Not
         *            {@code null}.
         * 
         * @param templateSource
         *            See {@link TemplateLoader#findTemplateSource(String)} to understand what that means. Not
         *            {@code null}.
         */
        private PositiveTemplateLookupResult(String templateSourceName, Object templateSource) {
            NullArgumentException.check("templateName", templateSourceName);
            NullArgumentException.check("templateSource", templateSource);

            if (templateSource instanceof TemplateLookupResult) {
                throw new IllegalArgumentException();
            }

            this.templateSourceName = templateSourceName;
            this.templateSource = templateSource;
        }

        public String getTemplateSourceName() {
            return templateSourceName;
        }

        Object getTemplateSource() {
            return templateSource;
        }

        public boolean isPositive() {
            return true;
        }
    }

    private static final class NegativeTemplateLookupResult extends TemplateLookupResult {
        
        private static final NegativeTemplateLookupResult INSTANCE = new NegativeTemplateLookupResult();
                
        private NegativeTemplateLookupResult() {
            // nop
        }

        public String getTemplateSourceName() {
            return null;
        }

        Object getTemplateSource() {
            return null;
        }

        public boolean isPositive() {
            return false;
        }
        
    }
    
}
