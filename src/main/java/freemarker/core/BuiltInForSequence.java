package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

abstract class BuiltInForSequence extends BuiltIn {
    TemplateModel _eval(Environment env)
            throws TemplateException
    {
        TemplateModel model = target.eval(env);
        if (!(model instanceof TemplateSequenceModel)) {
            throw new NonSequenceException(target, model, env);
        }
        return calculateResult((TemplateSequenceModel) model);
    }
    abstract TemplateModel calculateResult(TemplateSequenceModel tsm)
    throws
        TemplateModelException;
}