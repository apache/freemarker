package org.apache.freemarker.core.templatesuite.models;

import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;

public class TestNode implements TemplateNodeModel {

    @Override
    public String getNodeName() {
        return "name";
    }

    @Override
    public TemplateNodeModel getParentNode() {
        return null;
    }

    @Override
    public String getNodeType() {
        return "element";
    }

    @Override
    public TemplateSequenceModel getChildNodes() {
        return null;
    }

    @Override
    public String getNodeNamespace() {
        return null;
    }
}
