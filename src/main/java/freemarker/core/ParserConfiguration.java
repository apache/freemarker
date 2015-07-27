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
     * See {@link Configuration#setTagSyntax(int)}.
     */
    void setTagSyntax(int tagSyntax);

    /**
     * See {@link Configuration#setNamingConvention(int)}.
     */
    void setNamingConvention(int namingConvention);

    /**
     * See {@link Configuration#getNamingConvention()}.
     */
    int getNamingConvention();

    /**
     * See {@link Configuration#setWhitespaceStripping(boolean)}.
     */
    void setWhitespaceStripping(boolean b);

    /**
     * See {@link Configuration#getWhitespaceStripping()}.
     */
    boolean getWhitespaceStripping();

    /**
     * Overlaps with {@link Configurable#getArithmeticEngine()}; the parser needs this for creating numerical literals.
     */
    ArithmeticEngine getArithmeticEngine();

    /**
     * Overlaps with {@link Configurable#setArithmeticEngine(ArithmeticEngine)}; the parser needs this for creating
     * numerical literals.
     */
    void setArithmeticEngine(ArithmeticEngine arithmeticEngine);
    
    /**
     * See {@link Configuration#getStrictSyntaxMode()}.
     */
    boolean getStrictSyntaxMode();
    
    /**
     * See {@link Configuration#setStrictSyntaxMode(boolean)}.
     * 
     * @deprecated Only the default {@code false} value will be supported sometimes in the future.
     */
    @Deprecated
    void setStrictSyntaxMode(boolean strictSyntaxMode);

    /**
     * See {@link Configuration#getIncompatibleImprovements()}.
     */
    Version getIncompatibleImprovements();

}
