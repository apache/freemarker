package freemarker.core;


import freemarker.template.Template;
import freemarker.template.utility.StringUtil;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtil {

    static final String TYPES_AUTOMATICALLY_CONVERTIBLE_TO_STRING
            = "string or something automatically convertible to string (number, date or boolean)";

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
    
}
