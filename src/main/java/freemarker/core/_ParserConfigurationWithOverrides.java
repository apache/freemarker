package freemarker.core;

import freemarker.template.Version;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public final class _ParserConfigurationWithOverrides implements ParserConfiguration {

    private final OutputFormat outputFormat;
    private final Boolean autoEscaping;
    private final ParserConfiguration wrappedPCfg;

    public _ParserConfigurationWithOverrides(ParserConfiguration wrappedPCfg, OutputFormat outputFormat,
            Boolean autoEscaping) {
        this.outputFormat = outputFormat;
        this.autoEscaping = autoEscaping;
        this.wrappedPCfg = wrappedPCfg;
    }

    public boolean getWhitespaceStripping() {
        return wrappedPCfg.getWhitespaceStripping();
    }

    public int getTagSyntax() {
        return wrappedPCfg.getTagSyntax();
    }

    public boolean getStrictSyntaxMode() {
        return wrappedPCfg.getStrictSyntaxMode();
    }

    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : wrappedPCfg.getOutputFormat();
    }

    public int getNamingConvention() {
        return wrappedPCfg.getNamingConvention();
    }

    public Version getIncompatibleImprovements() {
        return wrappedPCfg.getIncompatibleImprovements();
    }

    public boolean getAutoEscaping() {
        return autoEscaping != null ? autoEscaping.booleanValue() : wrappedPCfg.getAutoEscaping();
    }

    public ArithmeticEngine getArithmeticEngine() {
        return wrappedPCfg.getArithmeticEngine();
    }
}