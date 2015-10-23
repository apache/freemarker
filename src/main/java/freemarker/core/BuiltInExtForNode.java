package freemarker.core;

import freemarker.template.*;

/**
 * Created by Pmuruge on 10/23/2015.
 */
public abstract class BuiltInExtForNode extends BuiltIn {
    @Override
    TemplateModel _eval(Environment env)
            throws TemplateException {
        TemplateModel model = target.eval(env);
        if (model instanceof TemplateNodeModelExt) {
            return calculateResult((TemplateNodeModelExt) model, env);
        } else {
            throw new NonNodeException(target, model, env);
        }
    }
    abstract TemplateModel calculateResult(TemplateNodeModelExt nodeModel, Environment env)
            throws TemplateModelException;
}
