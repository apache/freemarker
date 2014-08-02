package freemarker.core;

import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.TemplateModelException;

/**
 * Creates {@link TemplateDateFormat}-s for a fixed time zone, and if it producers formatters that are sensitive
 * to locale, for a fixed locale. Thus, FreeMarker should maintain a separate instance for each time zone that's
 * frequently used, or if {@link #isLocaleBound()} is {@code true}, for each {@link TimeZone}-{@link Locale}
 * permutation that's frequently used. Reusing the factories is useful as some factories cache instances internally for
 * the {@code dateType}-{@code formatDescriptor} pairs.
 * 
 * <p>{@link TemplateDateFormatFactory}-es need not be thread-safe. Currently (2.3.21) they are (re)used only from
 * within a single {@link Environment} instance.
 */
abstract class TemplateDateFormatFactory {
    
    private final TimeZone timeZone;
    
    public TemplateDateFormatFactory(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Whether this factory is sensitive to {@link Locale}; if the created {@link TemplateDateFormat}-s are, then
     * the factory should be too {@code true}.   
     */
    public abstract boolean isLocaleBound();
    
    /**
     * Returns the {@link TemplateDateFormat} for the {@code dateType} and {@code formatDescriptor} given via the
     * arguments, and the {@code TimeZone} and {@code Locale} (if that's relevant) to which the
     * {@link TemplateDateFormatFactory} belongs to.
     * 
     * @param dateType {@line TemplateDateModel#DATE}, {@line TemplateDateModel#TIME},
     *         {@line TemplateDateModel#DATETIME} or {@line TemplateDateModel#UNKNOWN}. Supporting
     *         {@line TemplateDateModel#UNKNOWN} is not necessary, in which case the method should throw an 
     *         {@link UnknownDateTypeFormattingUnsupportedException} exception.  
     * @param formatDescriptor The string used as {@code ..._format} the configuration setting value (among others),
     *         like {@code "iso m"} or {@code "dd.MM.yyyy HH:mm"}. The implementation is only supposed to
     *         understand a particular kind of format descriptor, for which FreeMarker routes to this factory.
     *         (Like, the {@link ISOTemplateDateFormatFactory} is only called for format descriptors that start with
     *         "iso".)
     *         
     * @throws ParseException if the {@code formatDescriptor} is malformed
     * @throws TemplateModelException if the {@code dateType} is unsupported by the formatter
     * @throws UnknownDateTypeFormattingUnsupportedException if {@code dateType} is {@line TemplateDateModel#UNKNOWN},
     *           and that's unsupported by the formatter implementation.
     */
    public abstract TemplateDateFormat get(int dateType, String formatDescriptor)
            throws java.text.ParseException, TemplateModelException, UnknownDateTypeFormattingUnsupportedException;
    
}
