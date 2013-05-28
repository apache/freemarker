package freemarker.core;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.utility.StringUtil;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtil {

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

    static String encloseAsTag(Template t, String tagContent, boolean closeTag) {
        // TODO: detect the tag syntax of the template
        return t.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX
                ? (closeTag ? "[/" : "[") + tagContent + "]"
                : (closeTag ? "</" : "<") + tagContent + ">";
                
    }

    static String encloseAsTag(Expression exp, String tagContent, boolean closeTag) {
        return encloseAsTag(exp != null ? exp.getTemplate() : null, tagContent, closeTag);
    }

    /**
     * Returns the type description as it should be used in type-related error messages.
     * TODO: When this is complete and proven, move it to Environment. 
     */
    static String getFTLTypeName(TemplateModel tm) {
        if (tm == null) {
            return "Null";
        } else {
            //TODO: This is not understandable for users:
            return tm.getClass().getName();
        }
    }
    
}
