package freemarker.core;

public class DummyTemplateOutputModel extends EscapingTemplateOutputModel<DummyTemplateOutputModel> {

    DummyTemplateOutputModel(String plainTextContent, String markupContet) {
        super(plainTextContent, markupContet);
    }

    @Override
    public OutputFormat<DummyTemplateOutputModel> getOutputFormat() {
        return DummyOutputFormat.INSTANCE;
    }
    
}