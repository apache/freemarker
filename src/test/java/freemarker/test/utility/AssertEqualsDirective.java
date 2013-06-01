package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;

public class AssertEqualsDirective implements TemplateDirectiveModel {
    
    public static AssertEqualsDirective INSTANCE = new AssertEqualsDirective();

    private static final String ACTUAL_PARAM = "actual";
    private static final String EXPECTED_PARAM = "expected";

    private AssertEqualsDirective() { }
    
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        TemplateModel actual = null;
        TemplateModel expected = null;
        for (Object paramEnt  : params.entrySet()) {
            Map.Entry<String, TemplateModel> param = (Map.Entry) paramEnt;
            String paramName = param.getKey();
            if (paramName.equals(ACTUAL_PARAM)) {
                actual = param.getValue();
            } else if (paramName.equals(EXPECTED_PARAM)) {
                expected = param.getValue();
            } else {
                throw new UnsupportedParameterException(paramName, env);
            }
        }
        if (actual == null) {
            throw new MissingRequiredParameterException(ACTUAL_PARAM, env);
        }
        if (expected == null) {
            throw new MissingRequiredParameterException(EXPECTED_PARAM, env);
        }
        if (body != null) throw new NestedContentNotSupportedException(env);
        
        if (!env.applyEqualsOperatorLenient(actual, expected)) {
            throw new AssertationFailedInTemplateException("Assertion failed:\n"
                    + "Expected: " + tryUnwrapp(expected) + "\n"
                    + "Actual: " + tryUnwrapp(actual),
                    env);
        }
        
    }

    private String tryUnwrapp(TemplateModel value) throws TemplateModelException {
        if (value == null) return "null";
        // This is the same order as comparison goes:
        else if (value instanceof TemplateNumberModel) return ((TemplateNumberModel) value).getAsNumber().toString();
        else if (value instanceof TemplateDateModel) return ((TemplateDateModel) value).getAsDate().toString();
        else if (value instanceof TemplateScalarModel) return StringUtil.jQuote(((TemplateScalarModel) value).getAsString());
        else if (value instanceof TemplateBooleanModel) return String.valueOf(((TemplateBooleanModel) value).getAsBoolean());
        // This shouldn't be reached, as the comparison should have failed earlier:
        else return value.toString();
    }

}
