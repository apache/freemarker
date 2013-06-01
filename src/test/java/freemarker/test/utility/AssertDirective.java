package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.utility.ClassUtil;

public class AssertDirective implements TemplateDirectiveModel {

    public static AssertDirective INSTANCE = new AssertDirective();
    
    private static final String TEST_PARAM = "test";
    
    private AssertDirective() { }
    
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        TemplateModel test = null;
        for (Object paramEnt  : params.entrySet()) {
            Map.Entry<String, TemplateModel> param = (Map.Entry) paramEnt;
            String paramName = param.getKey();
            if (paramName.equals(TEST_PARAM)) {
                test = param.getValue();
            } else {
                throw new UnsupportedParameterException(paramName, env);
            }
        }
        if (test == null) {
            throw new MissingRequiredParameterException(TEST_PARAM, env);
        }
        if (body != null) throw new NestedContentNotSupportedException(env);
        
        if (!(test instanceof TemplateBooleanModel)) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "The value had to be boolean, but it was of type" + ClassUtil.getFTLTypeDescription(test),
                    env);
        }
        if (!((TemplateBooleanModel) test).getAsBoolean()) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "the value was false.",
                    env);
        }
        
    }

}
