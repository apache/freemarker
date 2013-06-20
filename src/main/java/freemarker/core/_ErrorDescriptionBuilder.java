package freemarker.core;

import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.StringUtil;

/**
 * Used internally only, might changes without notice!
 * Packs a structured from of the error description from which the error message can be rendered on-demand.
 */
public class _ErrorDescriptionBuilder {

    private static final Logger logger = Logger.getLogger("freemarker.runtime");
    
    private final String description;
    private final Object[] descriptionParts;
    private Expression blamed;
    private boolean showBlamer;
    private Object/*String|Object[]*/ tip;
    private Object[]/*String[]|Object[][]*/ tips;
    private Template template;

    public _ErrorDescriptionBuilder(String description) {
        this.description = description;
        this.descriptionParts = null;
    }

    /**
     * @param descriptionParts These will be concatenated to a single {@link String} in {@link #toString()}.
     *      {@link String} array items that look like FTL tag (must start with {@code "&lt;"} and end with {@code ">"})
     *      will be converted to the actual template syntax if {@link #blamed} or {@link #template} was set.
     */
    public _ErrorDescriptionBuilder(Object[] descriptionParts) {
        this.descriptionParts = descriptionParts;
        this.description = null;
    }

    public String toString() {
        return toString(null);
    }
    
    public String toString(TemplateElement parentElement) {
        if (blamed == null && tips == null && tip == null && descriptionParts == null) return description;

        StringBuffer sb = new StringBuffer(200);
        
        if (parentElement != null && blamed != null && showBlamer) {
            try {
                Blaming blaming = findBlaming(parentElement, blamed, 0);
                if (blaming != null) {
                    sb.append("For ");
                    String nss = blaming.blamer.getNodeTypeSymbol();
                    char q = nss.indexOf('"') == -1 ? '\"' : '`';
                    sb.append(q).append(nss).append(q);
                    sb.append(" ").append(blaming.roleOfblamed).append(": ");
                }
            } catch (Throwable e) {
                // Should not happen. But we rather give a not-so-good error message than replace it with another...
                // So we ignore this.
                logger.error("Error when searching blamer for better error message.", e);
            }
        }
        
        
        
        if (description != null) {
            sb.append(description);
        } else {
            appendParts(sb, descriptionParts);
        }
        
        if (blamed != null) {
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
            
            String[] lines = splitToLines(blamed.toString());
            for (int i = 0; i < lines.length; i++) {
                sb.append(i == 0 ? "==> " : "\n    ");
                sb.append(lines[i]);
            }
            
            sb.append("  [");
            sb.append(blamed.getStartLocation());
            sb.append(']');
        }
        
        Object[] tips = this.tips != null ? this.tips : (tip != null ? new Object[] { tip } : null);
        if (tips != null && tips.length > 0) {
            sb.append("\n\n");
            for (int i = 0; i < tips.length; i++) {
                if (i != 0) sb.append('\n');
                sb.append("Tip: ");
                Object tip = tips[i];
                if (!(tip instanceof Object[])) {
                    sb.append(tips[i]);
                } else {
                    appendParts(sb, (Object[]) tip);
                }
            }
        }
        
        return sb.toString();
    }

    private Blaming findBlaming(TemplateObject parent, Expression blamed, int recursionDepth) {
        // Just in case a loop ever gets into the AST somehow, try not fill the stack and such: 
        if (recursionDepth > 50) return null;
        
        int paramCnt = parent.getParameterCount();
        for (int i = 0; i < paramCnt; i++) {
            Object paramValue = parent.getParameterValue(i);
            if (paramValue == blamed) {
                Blaming blaming = new Blaming();
                blaming.blamer = parent;
                blaming.roleOfblamed = parent.getParameterRole(i);
                return blaming;
            } else if (paramValue instanceof TemplateObject) {
                Blaming blaming = findBlaming((TemplateObject) paramValue, blamed, recursionDepth + 1);
                if (blaming != null) return blaming;
            }
        }
        return null;
    }

    private void appendParts(StringBuffer sb, Object[] parts) {
        Template template = this.template != null ? this.template : (blamed != null ? blamed.getTemplate() : null); 
        for (int i = 0; i < parts.length; i++) {
            Object partObj = parts[i];
            if (partObj instanceof Object[]) {
                appendParts(sb, (Object[]) partObj);
            } else {
                String part = parts[i].toString();
                if (template != null) {
                    if (part.length() > 4
                            && part.charAt(0) == '<'
                            && (
                                    (part.charAt(1) == '#' || part.charAt(1) == '@')
                                    || (part.charAt(1) == '/') && (part.charAt(2) == '#' || part.charAt(2) == '@')
                               )
                            && part.charAt(part.length() - 1) == '>') {
                        if (template.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX) {
                            sb.append('[');
                            sb.append(part.substring(1, part.length() - 1));
                            sb.append(']');
                        } else {
                            sb.append(part);
                        }
                    } else {
                        sb.append(part);
                    }
                } else {
                    sb.append(part);
                }
            }
        }
    }

    private String[] splitToLines(String s) {
        s = StringUtil.replace(s, "\r\n", "\n");
        s = StringUtil.replace(s, "\r", "\n");
        String[] lines = StringUtil.split(s, '\n');
        return lines;
    }
    
    /**
     * Needed for description <em>parts</em> that look like an FTL tag to be converted, if there's no {@link #blamed}.
     */
    public _ErrorDescriptionBuilder template(Template template) {
        this.template = template;
        return this;
    }

    public _ErrorDescriptionBuilder blame(Expression blamedExpr) {
        this.blamed = blamedExpr;
        return this;
    }
    
    public _ErrorDescriptionBuilder showBlamer(boolean showBlamer) {
        this.showBlamer = showBlamer;
        return this;
    }
    
    public _ErrorDescriptionBuilder tip(String tip) {
        this.tip = tip;
        return this;
    }
    
    public _ErrorDescriptionBuilder tip(Object tip[]) {
        this.tip = tip;
        return this;
    }
    
    public _ErrorDescriptionBuilder tips(Object[] tips) {
        this.tips = tips;
        return this;
    }

    public _ErrorDescriptionBuilder tips(String[][] tips) {
        this.tips = tips;
        return this;
    }
    
    private static class Blaming {
        TemplateObject blamer;
        ParameterRole roleOfblamed;
    }
    
}
