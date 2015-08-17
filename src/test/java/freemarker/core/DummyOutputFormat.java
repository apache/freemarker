package freemarker.core;

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateModelException;

public class DummyOutputFormat extends CommonEscapingOutputFormat<DummyTemplateOutputModel> {
    
    public static final DummyOutputFormat INSTANCE = new DummyOutputFormat();
    
    private DummyOutputFormat() {
        // hide
    }

    @Override
    protected String escapePlainTextToString(String plainTextContent) {
        return plainTextContent.replaceAll("(\\.|\\\\)", "\\\\$1");
    }

    @Override
    protected DummyTemplateOutputModel newTOM(String plainTextContent, String markupContent) {
        return new DummyTemplateOutputModel(plainTextContent, markupContent);
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        out.write(escapePlainTextToString(textToEsc));
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return false;
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String getMimeType() {
        return "text/dummy";
    }
    
}