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
    
    static String formatLocationForEvaluationError(String templateName, int line, int column) {
        return formatLocation("at", templateName, line, column);
    }

    private static String formatLocation(String preposition, Template template, int line, int column) {
        return formatLocation(preposition, template != null ? template.getName() : null, line, column);
    }
    
    private static String formatLocation(String preposition, String templateName, int line, int column) {
        String templateDesc = templateName != null
                ? "template " + StringUtil.jQuoteNoXSS(templateName)
                : "nameless template";
        return "in " + templateDesc + " "
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
    
}
