package org.apache.freemarker.core.templatesuite.models;

import java.util.List;

import org.apache.freemarker.core.model.TemplateMethodModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;

public class TestMethod implements TemplateMethodModel {

    @Override
    public TemplateModel execute(List<? extends TemplateModel> args) {
        return new SimpleScalar("x");
    }

}
