package freemarker.core;


import freemarker.template.Template;
import freemarker.template.utility.StringUtil;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtil {

    static final String TYPES_USABLE_WHERE_STRING_IS_EXPECTED
            = "string or something automatically convertible to string (number, date or boolean)";
    
    public static final String[] UNKNOWN_DATE_TYPE_ERROR_TIPS = new String[] {
            "Use ?time, ?date or ?datetime to tell FreeMarker which parts of the date is used.",
            "For programmers: Use java.sql.Date/Time/Timestamp instead of java.util.Date in the "
            + "data-model to avoid this ambiguity."
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
    
}
