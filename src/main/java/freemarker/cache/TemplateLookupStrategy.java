package freemarker.cache;

import java.io.IOException;
import java.util.Locale;

import freemarker.template.Configuration;

/**
 * Finds the {@link TemplateLoader}-level (storage-level) template source for a template name. This usually means trying
 * various {@link TemplateLoader}-level template names that were deduced from the requested name by delegating to
 * {@link TemplateLookupContext#lookupWithAcquisitionStrategy(String)}. See {@link #DEFAULT} as an example.
 * The lookup strategy meant to operate solely with template names, not with {@link TemplateLoader}-s directly.
 * 
 * @since 2.3.22
 */
public interface TemplateLookupStrategy {

    /**
     * The default template lookup strategy. Assuming localized lookup is enabled (see
     * {@link Configuration#setLocalizedLookup(boolean)}), and that a template is requested for the name
     * {@code example.ftl} and {@code Locale("es", "ES", "Traditional_WIN")}, it will try the following template names,
     * in this order: {@code "foo_en_AU_Traditional_WIN.ftl"}, {@code "foo_en_AU_Traditional.ftl"},
     * {@code "foo_en_AU.ftl"}, {@code "foo_en.ftl"}, {@code "foo.ftl"}
     * 
     * <p>
     * More precisely, it removes the file extension (the part starting with the <em>last</em> dot), then appends
     * {@link Locale#toString()} after it, and puts back the extension. Then it starts to remove the parts from the end
     * of the locale, considering {@code "_"} as the parts separator.
     * 
     * <p>
     * If localized lookup is disabled, it simply will not try to add any locale strings, so it just looks for
     * {@code "foo.ftl"}.
     * 
     * @since 2.3.22
     */
    public static final TemplateLookupStrategy DEFAULT = new TemplateLookupStrategy() {

        private static final String LOCALE_SEPARATOR = "_";
        
        public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
            final String templateName = ctx.getTemplateName();
            
            if (ctx.getTemplateLocale() == null) {
                return ctx.lookupWithAcquisitionStrategy(templateName);
            }
            
            // Localized lookup:
            int lastDot = templateName.lastIndexOf('.');
            String prefix = lastDot == -1 ? templateName : templateName.substring(0, lastDot);
            String suffix = lastDot == -1 ? "" : templateName.substring(lastDot);
            String localeName = LOCALE_SEPARATOR + ctx.getTemplateLocale().toString();
            StringBuffer buf = new StringBuffer(templateName.length() + localeName.length());
            buf.append(prefix);
            tryLocaleNameVariations: while (true) {
                buf.setLength(prefix.length());
                String path = buf.append(localeName).append(suffix).toString();
                TemplateLookupResult lookupResult = ctx.lookupWithAcquisitionStrategy(path);
                if (lookupResult != null) {
                    return lookupResult;
                }
                
                int lastUnderscore = localeName.lastIndexOf('_');
                if (lastUnderscore == -1) {
                    break tryLocaleNameVariations;
                }
                localeName = localeName.substring(0, lastUnderscore);
            }
            return null;
        }
    };

    /**
     * Finds the template source that matches the template name, locale (if not {@code null}) and other parameters
     * specified in the {@link TemplateLookupContext}.
     * 
     * @param ctx
     *            Contains the parameters for which the matching template source need to be found, and operations that
     *            are needed to implement the strategy. Some of the important input methods are:
     *            {@link TemplateLookupContext#getTemplateName()}, {@link TemplateLookupContext#getTemplateLocale()}.
     *            The most important operation is
     *            {@link TemplateLookupContext#lookupWithAcquisitionStrategy(String)}.
     * 
     * @return Usually the return value of
     *         {@link TemplateLookupContext#lookupWithAcquisitionStrategy(String)}, or {@code null} if no
     *         matching template exists.
     */
    TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException;

}
