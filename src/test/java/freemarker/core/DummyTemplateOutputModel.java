package freemarker.core;

public class DummyTemplateOutputModel extends CommonEscapingTemplateOutputModel<DummyTemplateOutputModel> {

    DummyTemplateOutputModel(String plainTextContent, String markupContet) {
        super(plainTextContent, markupContet);
    }

    @Override
    public DummyOutputFormat getOutputFormat() {
        return DummyOutputFormat.INSTANCE;
    }
    
}