package freemarker.core;

import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

abstract class BuiltInForDate extends BuiltIn {
    TemplateModel _eval(Environment env)
            throws TemplateException
    {
        TemplateModel model = target.eval(env);
        if (model instanceof TemplateDateModel) {
            TemplateDateModel tdm = (TemplateDateModel) model;
            return calculateResult(EvalUtil.modelToDate(tdm, target), tdm.getDateType(), env);
        } else {
            throw newNonDateException(env, model, target);
        }
    }

    /** Override this to implement the built-in. */
    protected abstract TemplateModel calculateResult(
            Date date, int dateType, Environment env)
    throws TemplateException;
    
    static TemplateException newNonDateException(Environment env, TemplateModel model, Expression target)
            throws InvalidReferenceException {
        TemplateException e;
        if(model == null) {
            e = InvalidReferenceException.getInstance(target, env);
        } else {
            e = new NonDateException(target, model, "date", env);
        }
        return e;
    }
    
}