package freemarker.cache;

import freemarker.template.utility.NullArgumentException;

/**
 * The return value of {@link TemplateLookupStrategy}. You usually get one from
 * {@link TemplateLookupContext#lookupWithAcquisitionStrategy(String)}.
 * 
 * @since 2.3.22
 */
public abstract class TemplateLookupResult {

    static TemplateLookupResult createNegativeResult() {
        return NegativeTemplateLookupResult.INSTANCE;
    }
    
    static TemplateLookupResult from(String templateSourceName, Object templateSource) {
        return templateSource != null
                ? new PositiveTemplateLookupResult(templateSourceName, templateSource)
                : createNegativeResult();
    }
    
    public abstract String getTemplateSourceName();

    public abstract boolean isPositive();

    abstract Object getTemplateSource();

    static final class PositiveTemplateLookupResult extends TemplateLookupResult {

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

    static final class NegativeTemplateLookupResult extends TemplateLookupResult {
        
        private static final NegativeTemplateLookupResult INSTANCE = new NegativeTemplateLookupResult();
                
        private NegativeTemplateLookupResult() {
            //
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
