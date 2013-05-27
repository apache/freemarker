package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a named directive/function parameter is missing.  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
class MissingRequiredParameterException extends ParameterException {

    public MissingRequiredParameterException(String parameterName, Environment env) {
        this(parameterName, null, null, env);
    }

    public MissingRequiredParameterException(String parameterName, Exception cause, Environment env) {
        this(parameterName, null, cause, env);
    }

    public MissingRequiredParameterException(String parameterName, String description, Environment env) {
        this(parameterName, description, null, env);
    }

    public MissingRequiredParameterException(String parameterName, String description, Exception cause, Environment env) {
        super(parameterName,
                "Required parameter " + StringUtil.jQuote(parameterName) + " is missing, "
                + "or the parameter value was null."
                + (description != null ? " " + StringUtil.jQuote(description) : ""),
                cause, env);
    }

}
