/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import freemarker.ext.beans._MethodUtil;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * Used internally only, might changes without notice!
 * Packs a structured from of the error description from which the error message can be rendered on-demand.
 * Note that this class isn't serializable, thus the containing exception should render the message before it's
 * serialized.
 */
public class _ErrorDescriptionBuilder {

    private static final Logger LOG = Logger.getLogger("freemarker.runtime");

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
        return toString(null, true);
    }
    
    public String toString(TemplateElement parentElement, boolean showTips) {
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
                LOG.error("Error when searching blamer for better error message.", e);
            }
        }
        
        if (description != null) {
            sb.append(description);
        } else {
            appendParts(sb, descriptionParts);
        }

        String extraTip = null;
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
            
            
            if (containsSingleInterpolatoinLiteral(blamed, 0)) {
                extraTip = "It has been noticed that you are using ${...} as the sole content of a quoted string. That "
                        + "does nothing but forcably converts the value inside ${...} to string (as it inserts it into "
                        + "the enclosing string). "
                        + "If that's not what you meant, just remove the quotation marks, ${ and }; you don't need "
                        + "them. If you indeed wanted to convert to string, use myExpression?string instead.";
            }
        }
        
        if (showTips) {
            int allTipsLen = (tips != null ? tips.length : 0) + (tip != null ? 1 : 0) + (extraTip != null ? 1 : 0);
            Object[] allTips;
            if (tips != null && allTipsLen == tips.length) {
                allTips = tips;
            } else {
                allTips = new Object[allTipsLen];
                int dst = 0;
                if (tip != null) allTips[dst++] = tip; 
                if (tips != null) {
                    for (int i = 0; i < tips.length; i++) {
                        allTips[dst++] = tips[i]; 
                    }
                }
                if (extraTip != null) allTips[dst++] = extraTip; 
            }
            if (allTips != null && allTips.length > 0) {
                sb.append("\n\n");
                for (int i = 0; i < allTips.length; i++) {
                    if (i != 0) sb.append('\n');
                    sb.append(_CoreAPI.ERROR_MESSAGE_HR).append('\n');
                    sb.append("Tip: ");
                    Object tip = allTips[i];
                    if (!(tip instanceof Object[])) {
                        sb.append(allTips[i]);
                    } else {
                        appendParts(sb, (Object[]) tip);
                    }
                }
                sb.append('\n').append(_CoreAPI.ERROR_MESSAGE_HR);
            }
        }
        
        return sb.toString();
    }

    private boolean containsSingleInterpolatoinLiteral(Expression exp, int recursionDepth) {
        if (exp == null) return false;
        
        // Just in case a loop ever gets into the AST somehow, try not fill the stack and such: 
        if (recursionDepth > 20) return false;
        
        if (exp instanceof StringLiteral && ((StringLiteral) exp).isSingleInterpolationLiteral()) return true;
        
        int paramCnt = exp.getParameterCount();
        for (int i = 0; i < paramCnt; i++) {
            Object paramValue = exp.getParameterValue(i);
            if (paramValue instanceof Expression) {
                boolean result = containsSingleInterpolatoinLiteral((Expression) paramValue, recursionDepth + 1);
                if (result) return true;
            }
        }
        
        return false;
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
                String partStr;
                partStr = tryToString(partObj);
                if (partStr == null) {
                    partStr = "null";
                }
                
                if (template != null) {
                    if (partStr.length() > 4
                            && partStr.charAt(0) == '<'
                            && (
                                    (partStr.charAt(1) == '#' || partStr.charAt(1) == '@')
                                    || (partStr.charAt(1) == '/') && (partStr.charAt(2) == '#' || partStr.charAt(2) == '@')
                               )
                            && partStr.charAt(partStr.length() - 1) == '>') {
                        if (template.getActualTagSyntax() == Configuration.SQUARE_BRACKET_TAG_SYNTAX) {
                            sb.append('[');
                            sb.append(partStr.substring(1, partStr.length() - 1));
                            sb.append(']');
                        } else {
                            sb.append(partStr);
                        }
                    } else {
                        sb.append(partStr);
                    }
                } else {
                    sb.append(partStr);
                }
            }
        }
    }

    /**
     * A twist on Java's toString that generates more appropriate results for generating error messages.
     */
    public static String toString(Object partObj) {
        return toString(partObj, false);
    }

    public static String tryToString(Object partObj) {
        return toString(partObj, true);
    }
    
    private static String toString(Object partObj, boolean suppressToStringException) {
        final String partStr;
        if (partObj == null) {
            return null;
        } else if (partObj instanceof Class) {
            partStr = ClassUtil.getShortClassName((Class) partObj);
        } else if (partObj instanceof Method || partObj instanceof Constructor) {
            partStr = _MethodUtil.toString((Member) partObj);
        } else {
            partStr = suppressToStringException ? StringUtil.tryToString(partObj) : partObj.toString();
        }
        return partStr;
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
        tip((Object) tip);
        return this;
    }
    
    public _ErrorDescriptionBuilder tip(Object tip[]) {
        tip((Object) tip);
        return this;
    }
    
    private _ErrorDescriptionBuilder tip(Object tip) {
        if (this.tip == null) {
            this.tip = tip;
        } else {
            if (tips == null) {
                tips = new Object[] { tip };
            } else {
                final int origTipsLen = tips.length;
                
                Object[] newTips = new Object[origTipsLen + 1];
                for (int i = 0; i < origTipsLen; i++) {
                    newTips[i] = tips[i];
                }
                newTips[origTipsLen] = tip;
                tips = newTips;
            }
        }
        return this;
    }
    
    public _ErrorDescriptionBuilder tips(Object[] tips) {
        if (this.tips == null) {
            this.tips = tips;
        } else {
            final int origTipsLen = this.tips.length;
            final int additionalTipsLen = tips.length;
            
            Object[] newTips = new Object[origTipsLen + additionalTipsLen];
            for (int i = 0; i < origTipsLen; i++) {
                newTips[i] = this.tips[i];
            }
            for (int i = 0; i < additionalTipsLen; i++) {
                newTips[origTipsLen + i] = tips[i];
            }
            this.tips = newTips;
        }
        return this;
    }
    
    private static class Blaming {
        TemplateObject blamer;
        ParameterRole roleOfblamed;
    }
    
}
