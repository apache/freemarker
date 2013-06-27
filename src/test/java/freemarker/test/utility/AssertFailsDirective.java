package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;

public class AssertFailsDirective implements TemplateDirectiveModel {
    
    public static AssertFailsDirective INSTANCE = new AssertFailsDirective();

    private static final String MESSAGE_PARAM = "message";
    private static final String EXCEPTION_PARAM = "exception";
    
    private AssertFailsDirective() { }

    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        String message = null;
        String exception = null;
        for (Object paramEnt  : params.entrySet()) {
            Map.Entry<String, TemplateModel> param = (Map.Entry) paramEnt;
            String paramName = param.getKey();
            if (paramName.equals(MESSAGE_PARAM)) {
                message = getAsString(param.getValue(), MESSAGE_PARAM, env);
            } else if (paramName.equals(EXCEPTION_PARAM)) {
                exception = getAsString(param.getValue(), EXCEPTION_PARAM, env);
            } else {
                throw new UnsupportedParameterException(paramName, env);
            }
        }
        
        if (body != null) {
            boolean blockFailed;
            try {
                body.render(NullWriter.INSTANCE);
                blockFailed = false;
            } catch (Throwable e) {
                blockFailed = true;
                if (message != null && e.getMessage().toLowerCase().indexOf(message.toLowerCase()) == -1) {
                    throw new AssertationFailedInTemplateException(
                            "Failure is not like expected. The exception message:\n" + StringUtil.jQuote(e.getMessage())
                            + "\ndoesn't contain:\n" + StringUtil.jQuote(message) + ".",
                            env);
                }
                if (exception != null && e.getClass().getName().indexOf(exception) == -1) {
                    throw new AssertationFailedInTemplateException(
                            "Failure is not like expected. The exception class name " + StringUtil.jQuote(e.getClass().getName())
                            + " doesn't contain " + StringUtil.jQuote(message) + ".",
                            env);
                }
            }
            if (!blockFailed) {
                throw new AssertationFailedInTemplateException(
                        "Block was expected to fail, but it didn't.",
                        env);
            }
        }
    }

    private String getAsString(TemplateModel value, String paramName, Environment env)
            throws BadParameterTypeException, TemplateModelException {
        if (value instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) value).getAsString(); 
        } else {
            throw new BadParameterTypeException(paramName, "string", value, env);
        }
    }

}
