package freemarker.core;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Used internally only, might changes without notice!
 * Packs a structured from of the error description from which the error message can be rendered on-demand.
 */
public class Internal_ErrorDescriptionBuilder {

    private final String description;
    private final Object[] descriptionParts;
    private Expression blame;
    private Object/*String|Object[]*/ tip;
    private Object[]/*String[]|Object[][]*/ tips;
    private Template template;

    public Internal_ErrorDescriptionBuilder(String description) {
        this.description = description;
        this.descriptionParts = null;
    }

    /**
     * @param descriptionParts These will be concatenated to a single {@link String} in {@link #toString()}.
     *      {@link String} array items that look like FTL tag (must start with {@code "&lt;"} and end with {@code ">"})
     *      will be converted to the actual template syntax if {@link #blame} or {@link #template} was set.
     */
    public Internal_ErrorDescriptionBuilder(Object[] descriptionParts) {
        this.descriptionParts = descriptionParts;
        this.description = null;
    }

    public String toString() {
        if (blame == null && tips == null && tip == null && descriptionParts == null) return description;
        
        StringBuffer sb;
        if (description != null) {
            sb = new StringBuffer(description);
        } else {
            sb = new StringBuffer(200);
            appendParts(sb, descriptionParts);
        }
        
        if (blame != null) {
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
            sb.append(blame);
            sb.append("  [");
            sb.append(blame.getStartLocation());
            sb.append(']');
        }
        
        Object[] tips = this.tips != null ? this.tips : (tip != null ? new Object[] { tip } : null);
        if (tips != null && tips.length > 0) {
            sb.append("\n\n");
            for (int i = 0; i < tips.length; i++) {
                if (i != 0) sb.append('\n');
                sb.append("Tip: ");
                Object tip = tips[i];
                if (tip instanceof String) {
                    sb.append(tips[i]);
                } else {
                    appendParts(sb, (String[]) tip);
                }
            }
        }
        
        return sb.toString();
    }

    private void appendParts(StringBuffer sb, Object[] parts) {
        Template template =
                this.template != null ? this.template : (blame != null ? blame.getTemplate() : null); 
        for (int i = 0; i < parts.length; i++) {
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
    
    /**
     * Needed for description <em>parts</em> that look like an FTL tag to be converted, if there's no {@link #blame}.
     */
    public Internal_ErrorDescriptionBuilder template(Template template) {
        this.template = template;
        return this;
    }

    public Internal_ErrorDescriptionBuilder blame(Expression blamedExpr) {
        this.blame = blamedExpr;
        return this;
    }

    public Internal_ErrorDescriptionBuilder tip(String tip) {
        this.tip = tip;
        return this;
    }
    
    public Internal_ErrorDescriptionBuilder tip(String tip[]) {
        this.tip = tip;
        return this;
    }
    
    public Internal_ErrorDescriptionBuilder tips(String[] tips) {
        this.tips = tips;
        return this;
    }

    public Internal_ErrorDescriptionBuilder tips(String[][] tips) {
        this.tips = tips;
        return this;
    }
    
}
