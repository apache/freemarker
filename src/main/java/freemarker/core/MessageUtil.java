package freemarker.core;


import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtil {

    static final String TYPES_USABLE_WHERE_STRING_IS_EXPECTED
            = "string or something automatically convertible to string (number, date or boolean)";
    
    static final String[] UNKNOWN_DATE_TYPE_ERROR_TIPS = new String[] {
            "Use ?time, ?date or ?datetime to tell FreeMarker which parts of the date is used.",
            "For programmers: Use java.sql.Date/Time/Timestamp instead of java.util.Date in the "
            + "data-model to avoid this ambiguity."
    };
    
    static final String[] UNKNOWN_DATE_TO_STRING_TIPS = new String[] {
        "Use ?string(format) to specify which parts to display.",
        UNKNOWN_DATE_TYPE_ERROR_TIPS[0],
        UNKNOWN_DATE_TYPE_ERROR_TIPS[1]
    };

    static final String[] INVALID_REFERENCE_EXCEPTION_TIP = new String[] {
        "If the failing expression is known to be legally null/missing, either specify a "
        + "default value with myOptionalVar!myDefault, or use ",
        "<#if myOptionalVar??>", "when-present", "<#else>", "when-missing", "</#if>",
        ". (These only cover the last step of the expression; to cover the whole expression, "
        + "use parenthessis: (myOptionVar.foo)!myDefault, (myOptionVar.foo)??"
    };

    private MessageUtil() { }
        
    static String formatLocationForSimpleParsingError(Template template, int line, int column) {
        return formatLocation("in", template, line, column);
    }

    static String formatLocationForSimpleParsingError(String templateName, int line, int column) {
        return formatLocation("in", templateName, line, column);
    }

    static String formatLocationForDependentParsingError(Template template, int line, int column) {
        return formatLocation("on", template, line, column);
    }

    static String formatLocationForDependentParsingError(String templateName, int line, int column) {
        return formatLocation("on", templateName, line, column);
    }

    static String formatLocationForEvaluationError(Template template, int line, int column) {
        return formatLocation("at", template, line, column);
    }

    static String formatLocationForEvaluationError(Macro macro, int line, int column) {
        Template t = macro.getTemplate();
        return formatLocation("at", t != null ? t.getName() : null, macro.getName(), macro.isFunction(), line, column);
    }
    
    static String formatLocationForEvaluationError(String templateName, int line, int column) {
        return formatLocation("at", templateName, line, column);
    }

    private static String formatLocation(String preposition, Template template, int line, int column) {
        return formatLocation(preposition, template != null ? template.getName() : null, line, column);
    }
    
    private static String formatLocation(String preposition, String templateName, int line, int column) {
        return formatLocation(
                preposition, templateName,
                null, false,
                line, column);
    }

    private static String formatLocation(
            String preposition, String templateName,
            String macroOrFuncName, boolean isFunction,
            int line, int column) {
        String templateDesc = templateName != null
                ? "template " + StringUtil.jQuoteNoXSS(templateName)
                : "nameless template";
        return "in " + templateDesc
              + (macroOrFuncName != null
                      ? " in " + (isFunction ? "function " : "macro ") + StringUtil.jQuote(macroOrFuncName)
                      : "")
              + " "
              + preposition + " line " + line + ", column " + column;
    }

    /**
     * Returns a single line string that is no longer than {@code maxLength}.
     * If will truncate the string at line-breaks too.
     * The truncation is always signaled with a a {@code "..."} at the end of the result string.  
     */
    static String shorten(String s, int maxLength) {
        if (maxLength < 5) maxLength = 5;
        
        boolean isTruncated = false;
        
        int brIdx = s.indexOf('\n');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        };
        brIdx = s.indexOf('\r');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        }
        
        if (s.length() > maxLength) {
            s = s.substring(0, maxLength - 3);
            isTruncated = true;
        }
        
        if (!isTruncated) {
            return s;
        } else {
            if (s.endsWith(".")) {
                if (s.endsWith("..")) {
                    if (s.endsWith("...")) {
                        return s;
                    } else {
                        return s + ".";
                    }
                } else {
                    return s + "..";
                }
            } else {
                return s + "...";
            }
        }
    }
    
    static StringBuffer appendExpressionAsUntearable(StringBuffer sb, Expression argExp) {
        boolean needParen =
                !(argExp instanceof NumberLiteral)
                && !(argExp instanceof StringLiteral)
                && !(argExp instanceof BooleanLiteral)
                && !(argExp instanceof ListLiteral)
                && !(argExp instanceof HashLiteral)
                && !(argExp instanceof Identifier)
                && !(argExp instanceof Dot)
                && !(argExp instanceof DynamicKeyName)
                && !(argExp instanceof MethodCall)
                && !(argExp instanceof BuiltIn);
        if (needParen) sb.append('(');
        sb.append(argExp.getCanonicalForm());
        if (needParen) sb.append(')');
        return sb;
    }

    static String buildModelHasStoredNullMessage(Class expected, TemplateModel model) {
        String msg = "The FreeMarker value exists, but has nothing inside it; the TemplateModel object (class: "
                +  model.getClass().getName() + ") has returned a null instead of a "
                + ClassUtil.getShortClassName(expected) + ". "
                + "This is probably a bug in the non-FreeMarker code that builds the data-model.";
        return msg;
    }

    static TemplateModelException newArgCntError(String methodName, int argCnt, int expectedCnt) {
        return newArgCntError(methodName, argCnt, expectedCnt, expectedCnt);
    }
    
    static TemplateModelException newArgCntError(String methodName, int argCnt, int minCnt, int maxCnt) {
        StringBuffer sb = new StringBuffer();
        
        sb.append(methodName);
        
        sb.append('(');
        if (maxCnt != 0) sb.append("...");
        sb.append(") expects ");
        
        if (minCnt == maxCnt) {
            if (maxCnt == 0) {
                sb.append("no");
            } else {
                sb.append(maxCnt);
            }
        } else if (maxCnt - minCnt == 1) {
            sb.append(minCnt);
            sb.append(" or ");
            sb.append(maxCnt);
        } else {
            sb.append(minCnt);
            if (maxCnt != Integer.MAX_VALUE) {
                sb.append(" to ");
                sb.append(maxCnt);
            } else {
                sb.append(" or more (unlimited)");
            }
        }
        sb.append(" argument");
        if (maxCnt > 1) sb.append('s');
        
        sb.append(" but has received ");
        if (argCnt == 0) {
            sb.append("none");
        } else {
            sb.append(argCnt);
        }
        sb.append(".");
        
        return new TemplateModelException(sb.toString());
    }

    static TemplateModelException newMethodArgMustBeStringException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "string", arg);
    }

    static TemplateModelException newMethodArgMustBeNumberException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "number", arg);
    }
    
    static TemplateModelException newMethodArgMustBeBooleanException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "boolean", arg);
    }
    
    static TemplateModelException newMethodArgMustBeExtendedHashException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "extended hash", arg);
    }
    
    static TemplateModelException newMethodArgMustBeSequenceException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence", arg);
    }
    
    static TemplateModelException newMethodArgMustBeSequenceOrCollectionException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence or collection", arg);
    }
    
    static TemplateModelException newMethodArgUnexpectedTypeException(
            String methodName, int argIdx, String expectedType, TemplateModel arg) {
        return new TemplateModelException(
                new Internal_ErrorDescriptionBuilder(
                        new Object[] {
                            methodName, "(...) expects ", expectedType, " as argument #", new Integer(argIdx + 1),
                            ", but received a(n) ", new Internal_DelayedFTLTypeDescription(arg), "."
                        }),
                        true);
    }

    static Object[] unexpectedTypeErrorDescription(String expectedType, TemplateModel model) {
        return MessageUtil.unexpectedTypeErrorDescription(expectedType, new Internal_DelayedFTLTypeDescription(model));
    }

    static Object[] unexpectedTypeErrorDescription(String expectedType, Internal_DelayedFTLTypeDescription actualType) {
        return new Object[] {
                "Expected a(n) ", expectedType, ", but this evaluated to a value of type ", actualType, ":"};
    }
    
}
