package org.apache.freemarker.core;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;

public class TemplateCallableModelUtils {

    public static final TemplateModel[] EMPTY_TEMPLATE_MODEL_ARRAY = new TemplateModel[0];

    // TODO [FM3][CF] Add this to the other exception classes too
    public static TemplateNumberModel castArgumentToNumber(TemplateModel[] args, int argIndex, boolean allowNull,
            Environment env) throws TemplateException {
        return getTemplateNumberModel(args[argIndex], argIndex, allowNull, env);
    }

    // TODO [FM3][CF] Add this to the other exception classes too
    private static TemplateNumberModel getTemplateNumberModel(TemplateModel argValue, int argIndex, boolean allowNull,
            Environment env) throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return (TemplateNumberModel) argValue;
        }
        if (argValue == null) {
            if (allowNull) {
                return null;
            }
            throw new _MiscTemplateException(env,
                    "The ", new _DelayedOrdinal(argIndex + 1), " argument can't be null.");
        }
        throw new NonNumericalException(argIndex, argValue, null, env);
    }

    // TODO [FM3][CF] Add this to the other exception classes too
    public static TemplateNumberModel castArgumentToNumber(TemplateModel argValue, String argName, boolean allowNull,
            Environment env) throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return (TemplateNumberModel) argValue;
        }
        if (argValue == null) {
            if (allowNull) {
                return null;
            }
            throw new _MiscTemplateException(env,
                    "The ", new _DelayedJQuote(argName), " argument can't be null.");
        }
        throw new NonNumericalException(argName, argValue, null, env);
    }

}
