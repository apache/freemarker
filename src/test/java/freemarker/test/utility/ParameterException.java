package freemarker.test.utility;

import freemarker.core.Environment;
import freemarker.template.TemplateException;

/**
 * An exception that is related to a named parameter of a directive or function.
 * This is will be public and go into the freemarker.core when the method/directive stuff was reworked.
 */
abstract class ParameterException extends TemplateException {
    
    private final String parameterName;
    
    public ParameterException(String parameterName, Environment env) {
        this(parameterName, null, null, env);
    }

    public ParameterException(String parameterName, Exception cause, Environment env) {
        this(parameterName, null, cause, env);
    }

    public ParameterException(String parameterName, String description, Environment env) {
        this(parameterName, description, null, env);
    }

    public ParameterException(String parameterName, String description, Exception cause, Environment env) {
        super(description, cause, env);
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }
    
}
