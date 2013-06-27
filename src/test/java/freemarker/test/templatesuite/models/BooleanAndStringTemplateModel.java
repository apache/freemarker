package freemarker.test.templatesuite.models;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

public class BooleanAndStringTemplateModel implements TemplateBooleanModel, TemplateScalarModel {

    public String getAsString() throws TemplateModelException {
        return "theStringValue";
    }

    public boolean getAsBoolean() throws TemplateModelException {
        return true;
    }

}
