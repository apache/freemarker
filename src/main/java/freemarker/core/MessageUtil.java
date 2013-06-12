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

    static String decorateErrorDescription(String description) {
        return decorateErrorDescription(description, null, (String[]) null);
    }
    
    static String decorateErrorDescription(String description, String tip) {
        return decorateErrorDescription(description, null, tip);
    }

    static String decorateErrorDescription(String description, String[] tip) {
        return decorateErrorDescription(description, null, tip);
    }

    static String decorateErrorDescription(String description, Expression blamedExpr) {
        return decorateErrorDescription(description, blamedExpr, (String[]) null);
    }

    static String decorateErrorDescription(String description, Expression blamedExpr, String tip) {
        return decorateErrorDescription(description, blamedExpr, tip != null ? new String[] { tip } : null);
    }
    
    static String decorateErrorDescription(String description, Expression blamedExpr, String[] tips) {
        if (blamedExpr != null || tips != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(description);
            if (blamedExpr != null) {
                // Right-trim:
                for (int idx = sb.length() - 1; idx >= 0 && Character.isWhitespace(sb.charAt(idx)); idx --) {
                    sb.deleteCharAt(idx);
                }
                
                char lastChar = sb.length() > 0 ? (sb.charAt(sb.length() - 1)) : 0;
                if (lastChar != 0) {
                    sb.append('\n');
                }
                if (lastChar != ':') {
                    sb.append("The blamed expression:\n");
                }
                sb.append("==> ");
                sb.append(blamedExpr);
                sb.append("  [");
                sb.append(blamedExpr.getStartLocation());
                sb.append(']');
            }
            if (tips != null && tips.length > 0) {
                sb.append("\n");
                sb.append("\n");
                for (int i = 0; i < tips.length; i++) {
                    if (i != 0) sb.append('\n');
                    sb.append("Tip: ");
                    sb.append(tips[i]);
                }
                sb.append("");
            }
            return sb.toString();
        } else {
            return  description;
        }
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
                methodName + "(...) expects " + expectedType + " as argument #" + (argIdx + 1) + ", but received a(n) "
                + ClassUtil.getFTLTypeDescription(arg) + ".");
    }

    static String unexpectedTypeErrorDescription(String expectedType, TemplateModel model) {
        return MessageUtil.unexpectedTypeErrorDescription(expectedType, ClassUtil.getFTLTypeDescription(model));
    }

    static String unexpectedTypeErrorDescription(String expectedType, String actualType) {
        return "Expected a(n) " + expectedType + ", but this evaluated to a value of type " 
                + actualType + ":";
    }
    
}
