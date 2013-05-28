package freemarker.core;

import java.util.HashSet;
import java.util.Set;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BooleanModel;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.DateModel;
import freemarker.ext.beans.EnumerationModel;
import freemarker.ext.beans.IteratorModel;
import freemarker.ext.beans.MapModel;
import freemarker.ext.beans.NumberModel;
import freemarker.ext.beans.StringModel;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;
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
            Set typeNamesAppended = new HashSet();
            
            StringBuffer sb = new StringBuffer();

            Class primaryInterface = getPrimaryTemplateModelInterface(tm);
            if (primaryInterface != null) {
                appendTemplateModelTypeName(sb, typeNamesAppended, primaryInterface);
            }

            if (tm instanceof Macro) {
                appendTypeName(sb, typeNamesAppended, ((Macro) tm).isFunction() ? "function" : "macro");
            }
            
            appendTemplateModelTypeName(sb, typeNamesAppended, tm.getClass());
            
            String javaClassName;
            Class unwrappedClass = getUnwrappedClass(tm);
            if (unwrappedClass != null) {
                javaClassName = shortenClassName(unwrappedClass.getName());
            } else {
                javaClassName = null;
            }
            
            sb.append(" (");
            String modelClassName = shortenClassName(tm.getClass().getName());
            if (javaClassName == null) {
                sb.append("wrapper: ");
                sb.append(modelClassName);
            } else {
                sb.append(javaClassName);
                sb.append(" wrapped into ");
                sb.append(modelClassName);
            }
            sb.append(")");

            return sb.toString();
        }
    }

    private static String shortenClassName(String name) {
        if (name == null) {
            return null;
        } else if (name.startsWith("java.lang.") || name.startsWith("java.util.")) {
            return name.substring(10);
        } else if (name.startsWith("freemarker.template.")) {
            return "f.t" + name.substring(19);
        } else if (name.startsWith("freemarker.ext.beans.")) {
            return "f.e.b" + name.substring(20);
        } else {
            return name;
        }
    }

    /**
     * Returns the {@link TemplateModel} interface that is the most characteristic of the object, or {@code null}.
     * TODO: When this is proven, move it into BeanModel as public.
     */
    private static Class getPrimaryTemplateModelInterface(TemplateModel tm) {
        if (tm instanceof BeanModel) {
            if (tm instanceof CollectionModel) {
                return TemplateSequenceModel.class;
            } else if (tm instanceof IteratorModel || tm instanceof EnumerationModel) {
                return TemplateCollectionModel.class;
            } else if (tm instanceof MapModel) {
                return TemplateHashModelEx.class;
            } else if (tm instanceof NumberModel) {
                return TemplateNumberModel.class;
            } else if (tm instanceof BooleanModel) {
                return TemplateBooleanModel.class;
            } else if (tm instanceof DateModel) {
                return TemplateDateModel.class;
            } else if (tm instanceof StringModel) {
                Object wrapped = ((BeanModel) tm).getWrappedObject();
                return wrapped instanceof String
                        ? TemplateScalarModel.class
                        : (tm instanceof TemplateHashModelEx ? TemplateHashModelEx.class : null);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static void appendTemplateModelTypeName(StringBuffer sb, Set typeNamesAppended, Class cl) {
        if (TemplateNodeModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "node");
        }
        
        if (TemplateDirectiveModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "directive");
        } else if (TemplateTransformModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "transform");
        }
        
        if (TemplateSequenceModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "sequence");
        } else if (TemplateCollectionModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "collection");
        } else if (TemplateModelIterator.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "iterator");
        }
        
        if (TemplateMethodModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "method");
        }
        
        if (TemplateHashModelEx.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "hash");
        }
        
        if (TemplateNumberModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "number");
        }
        
        if (TemplateDateModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "date");
        }
        
        if (TemplateBooleanModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "boolean");
        }
        
        if (TemplateScalarModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "string");
        }
    }
    
    private static Class getUnwrappedClass(TemplateModel tm) {
        Object unwrapped;
        try {
            if (tm instanceof WrapperTemplateModel) {
                unwrapped = ((WrapperTemplateModel) tm).getWrappedObject();
            } else if (tm instanceof AdapterTemplateModel) {
                unwrapped = ((AdapterTemplateModel) tm).getAdaptedObject(Object.class);
            } else {
                unwrapped = null;
            }
        } catch (Throwable e) {
            unwrapped = null;
        }
        return unwrapped != null ? unwrapped.getClass() : null;
    }

    private static void appendTypeName(StringBuffer sb, Set typeNamesAppended, String name) {
        if (!typeNamesAppended.contains(name)) {
            if (sb.length() != 0) sb.append("+");
            sb.append(name);
            typeNamesAppended.add(name);
        }
    }
    
}
