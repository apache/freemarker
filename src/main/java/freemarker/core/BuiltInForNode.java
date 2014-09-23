package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;

abstract class BuiltInForNode extends BuiltIn {
    TemplateModel _eval(Environment env)
            throws TemplateException
    {
        TemplateModel model = target.eval(env);
        if (model instanceof TemplateNodeModel) {
            return calculateResult((TemplateNodeModel) model, env);
        } else {
            throw new NonNodeException(target, model, env);
        }
    }
    abstract TemplateModel calculateResult(TemplateNodeModel nodeModel, Environment env)
            throws TemplateModelException;
}