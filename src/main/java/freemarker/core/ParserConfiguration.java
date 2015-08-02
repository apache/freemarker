package freemarker.core;

import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * @since 2.3.24
 */
public interface ParserConfiguration {

    /**
     * See {@link Configuration#getTagSyntax()}.
     */
    int getTagSyntax();

    /**
     * See {@link Configuration#getNamingConvention()}.
     */
    int getNamingConvention();

    /**
     * See {@link Configuration#getWhitespaceStripping()}.
     */
    boolean getWhitespaceStripping();

    /**
     * Overlaps with {@link Configurable#getArithmeticEngine()}; the parser needs this for creating numerical literals.
     */
    ArithmeticEngine getArithmeticEngine();
    
    /**
     * See {@link Configuration#getStrictSyntaxMode()}.
     */
    boolean getStrictSyntaxMode();
    
    /**
     * See {@link Configuration#getIncompatibleImprovements()}.
     */
    Version getIncompatibleImprovements();

}
