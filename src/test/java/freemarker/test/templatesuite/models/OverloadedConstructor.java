package freemarker.test.templatesuite.models;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

public class OverloadedConstructor implements TemplateScalarModel {
    
    String value;
    
    public OverloadedConstructor(int i) {
        value = "int " + i;
    }
    
    public OverloadedConstructor(String s) {
        value = "String " + s;
    }

    public OverloadedConstructor(CharSequence s) {
        value = "CharSequence " + s;
    }
    
    public String getAsString() throws TemplateModelException {
        return value;
    }

}