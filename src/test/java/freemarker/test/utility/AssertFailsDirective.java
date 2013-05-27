package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;

import junit.framework.AssertionFailedError;
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

    private static final String MESSAGE_PARAM = "message";
    private static final String EXCEPTION_PARAM = "exception";

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
            try {
                body.render(NullWriter.INSTANCE);
                throw new AssertionFailedError("Block should have failed.");
            } catch (Throwable e) {
                if (message != null && e.getMessage().indexOf(message) == -1) {
                    throw new AssertationFailedInTemplateException(
                            "The exception message " + StringUtil.jQuote(e.getMessage())
                            + " doesn't contain " + StringUtil.jQuote(message) + ".",
                            env);
                }
                if (exception != null && e.getClass().getName().indexOf(exception) == -1) {
                    throw new AssertationFailedInTemplateException(
                            "The exception class name " + StringUtil.jQuote(e.getClass().getName())
                            + " doesn't contain " + StringUtil.jQuote(message) + ".",
                            env);
                }
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
