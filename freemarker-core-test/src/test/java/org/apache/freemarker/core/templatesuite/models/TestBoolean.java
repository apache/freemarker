package org.apache.freemarker.core.templatesuite.models;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateStringModel;

public class TestBoolean implements TemplateBooleanModel, TemplateStringModel {
    @Override
    public boolean getAsBoolean() {
        return true;
    }

    @Override
    public String getAsString() {
        return "de";
    }
}
