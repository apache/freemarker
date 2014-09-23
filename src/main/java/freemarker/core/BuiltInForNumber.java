package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

abstract class BuiltInForNumber extends BuiltIn {
    TemplateModel _eval(Environment env)
            throws TemplateException
    {
        TemplateModel model = target.eval(env);
        return calculateResult(target.modelToNumber(model, env), model);
    }
    
    abstract TemplateModel calculateResult(Number num, TemplateModel model)
    throws TemplateModelException;
}