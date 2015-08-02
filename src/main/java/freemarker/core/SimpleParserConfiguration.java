package freemarker.core;

import freemarker.template.Version;

/**
 * Used to work around that {@link FMParser} has constructors that have separate parameters for individual settings.
 * 
 * @since 2.3.24
 */
class SimpleParserConfiguration implements ParserConfiguration {

    private final int tagSyntax;
    private final int namingConvention;
    private final boolean whitespaceStripping;
    private final boolean strictSyntaxMode;
    private final String encoding;
    private final Version incompatibleImprovements;
    private final ArithmeticEngine arithmeticEngine;

    public SimpleParserConfiguration(boolean strictSyntaxMode, boolean whitespaceStripping, int tagSyntax,
            int namingConvention, Version incompatibleImprovements, ArithmeticEngine arithmeticEngine,
            String encoding) {
        this.tagSyntax = tagSyntax;
        this.namingConvention = namingConvention;
        this.whitespaceStripping = whitespaceStripping;
        this.strictSyntaxMode = strictSyntaxMode;
        this.encoding = encoding;
        this.incompatibleImprovements = incompatibleImprovements;
        this.arithmeticEngine = arithmeticEngine;
    }

    public int getTagSyntax() {
        return tagSyntax;
    }

    public int getNamingConvention() {
        return namingConvention;
    }

    public boolean getWhitespaceStripping() {
        return whitespaceStripping;
    }

    public boolean getStrictSyntaxMode() {
        return strictSyntaxMode;
    }

    public String getEncoding() {
        return encoding;
    }

    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }

    public ArithmeticEngine getArithmeticEngine() {
        return arithmeticEngine;
    }

}
