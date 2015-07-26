package freemarker.core;

import freemarker.template.Configuration;

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
    
}
