package freemarker.core;

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateModelException;

/**
 * Represents the output format that doesn't have escaping and MIME type.
 * 
 * @since 2.3.24
 */
public final class RawOutputFormat extends OutputFormat<RawTemplateOutputModel> {

    public static final RawOutputFormat INSTANCE = new RawOutputFormat();
    
    private RawOutputFormat() {
        // Only to decrease visibility
    }

    @Override
    public void output(RawTemplateOutputModel tom, Writer out) throws IOException, TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RawTemplateOutputModel escapePlainText(String textToEsc) throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourcePlainText(RawTemplateOutputModel tom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RawTemplateOutputModel fromMarkup(String markupText) throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMarkup(RawTemplateOutputModel tom) throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RawTemplateOutputModel concat(RawTemplateOutputModel tom1, RawTemplateOutputModel tom2)
            throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEscaping() {
        return false;
    }

}
