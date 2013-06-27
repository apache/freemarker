package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.utility.StringUtil;

/**
 * Indicates that a named directive/function parameter is unsupported (like a typo).  
 * This is will be public and go into the freemarker.core when the directive/method stuff was reworked.
 */
class UnsupportedParameterException extends ParameterException {

    public UnsupportedParameterException(String parameterName, Environment env) {
        this(parameterName, null, null, env);
    }

    public UnsupportedParameterException(String parameterName, Exception cause, Environment env) {
        this(parameterName, null, cause, env);
    }

    public UnsupportedParameterException(String parameterName, String description, Environment env) {
        this(parameterName, description, null, env);
    }

    public UnsupportedParameterException(String parameterName, String description, Exception cause, Environment env) {
        super(parameterName,
                "Unsuppored parameter: " + StringUtil.jQuote(parameterName)
                + (description == null ? "." : ". " + StringUtil.jQuote(description)),
                cause, env);
    }

}
