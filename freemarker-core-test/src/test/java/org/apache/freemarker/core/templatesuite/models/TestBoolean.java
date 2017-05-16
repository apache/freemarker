package org.apache.freemarker.core.templatesuite.models;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateScalarModel;

public class TestBoolean implements TemplateBooleanModel, TemplateScalarModel {
    @Override
    public boolean getAsBoolean() {
        return true;
    }

    @Override
    public String getAsString() {
        return "de";
    }
}
