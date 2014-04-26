package freemarker.test.templatesuite.models;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

public class HashAndScalarModel implements TemplateHashModel, TemplateScalarModel {

    public String getAsString() throws TemplateModelException {
        return "scalarValue";
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return new SimpleScalar("mapValue for " + key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

}
