package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.TemplateModel;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a named directive/function parameter is not of the expected type.  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
public class BadParameterTypeException extends ParameterException {

    public BadParameterTypeException(String parameterName, String expectedType, TemplateModel value, Environment env) {
        this(parameterName, expectedType, value, null, null, env);
    }

    public BadParameterTypeException(String parameterName, String expectedType, TemplateModel value,
            Exception cause, Environment env) {
        this(parameterName, expectedType, value, null, cause, env);
    }

    public BadParameterTypeException(String parameterName, String expectedType, TemplateModel value,
            String description, Environment env) {
        this(parameterName, expectedType, value, description, null, env);
    }

    public BadParameterTypeException(
            String parameterName, String expectedType, TemplateModel value, String description, Exception cause, Environment env) {
        super(parameterName,
                "The type of the parameter " + StringUtil.jQuote(parameterName) + " should be " + expectedType
                + ", but the actual value was " + getTypeDescription(value) + "."
                + (description != null ? " " + StringUtil.jQuote(description) : ""),
                cause, env);
    }

    private static String getTypeDescription(TemplateModel value) {
        //FIXME: This should call EvaluationUtil.getTypeDescriptionForDebugging, but that's not visible from here yet.
        return value == null ? "Null" : value.getClass().getName();
    }
    
}
