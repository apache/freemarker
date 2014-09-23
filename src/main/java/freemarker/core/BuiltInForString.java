package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

abstract class BuiltInForString extends BuiltIn {
    TemplateModel _eval(Environment env)
    throws TemplateException
    {
        return calculateResult(target.evalAndCoerceToString(env), env);
    }
    abstract TemplateModel calculateResult(String s, Environment env) throws TemplateException;
}