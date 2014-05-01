package freemarker.test.templatesuite.models;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

public class BooleanAndScalarModel implements TemplateBooleanModel, TemplateScalarModel {

    public static final BooleanAndScalarModel INSTANCE = new BooleanAndScalarModel();

    public String getAsString() throws TemplateModelException {
        return "s";
    }

    public boolean getAsBoolean() throws TemplateModelException {
        return true;
    }

}
