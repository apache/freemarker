package org.apache.freemarker.core.templatesuite.models;

import java.util.List;

import org.apache.freemarker.core.model.TemplateMethodModel;

public class TestMethod implements TemplateMethodModel {
    @Override
    public Object exec(List arguments) {
        return "x";
    }
}
