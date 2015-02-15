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

import java.io.IOException;

import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;


/**
 * An instruction that gets another template
 * and processes it within the current template.
 */
final class Include extends TemplateElement {

    private final Expression includedTemplateNameExp, encodingExp, parseExp, ignoreMissingExp;
    private final String encoding;
    private final Boolean parse;
    private final Boolean ignoreMissing;

    /**
     * @param template the template that this <tt>#include</tt> is a part of.
     * @param includedTemplatePathExp the path of the template to be included.
     * @param encodingExp the encoding to be used or null, if it's the default.
     * @param parseExp whether the template should be parsed (or is raw text)
     */
    Include(Template template,
            Expression includedTemplatePathExp,
            Expression encodingExp, Expression parseExp, Expression ignoreMissingExp) throws ParseException {
        this.includedTemplateNameExp = includedTemplatePathExp;
        
        this.encodingExp = encodingExp;
        if (encodingExp == null) {
            encoding = null;
        } else {
            if (encodingExp.isLiteral()) {
                try {
                    TemplateModel tm = encodingExp.eval(null);
                    if (!(tm instanceof TemplateScalarModel)) {
                        throw new ParseException("Expected a string as the value of the \"encoding\" argument",
                                encodingExp);
                    }
                    encoding = ((TemplateScalarModel) tm).getAsString();
                } catch (TemplateException e) {
                    // evaluation of literals must not throw a TemplateException
                    throw new BugException(e);
                }
            } else {
                encoding = null;
            }
        }
        
        this.parseExp = parseExp;
        if (parseExp == null) {
            parse = Boolean.TRUE;
        } else {
            if (parseExp.isLiteral()) {
                try {
                    if (parseExp instanceof StringLiteral) {
                        // Legacy
                        parse = Boolean.valueOf(StringUtil.getYesNo(parseExp.evalAndCoerceToString(null)));
                    } else {
                        try {
                            parse = Boolean.valueOf(parseExp.evalToBoolean(template.getConfiguration()));
                        } catch(NonBooleanException e) {
                            throw new ParseException("Expected a boolean or string as the value of the parse attribute",
                                    parseExp, e);
                        }
                    }
                } catch (TemplateException e) {
                    // evaluation of literals must not throw a TemplateException
                    throw new BugException(e);
                }
            } else {
                parse = null;
            }
        }
        
        this.ignoreMissingExp = ignoreMissingExp;
        if (ignoreMissingExp == null) {
            ignoreMissing = Boolean.FALSE;
        } else {
            if (ignoreMissingExp.isLiteral()) {
                try {
                    try {
                        ignoreMissing = Boolean.valueOf(
                                ignoreMissingExp.evalToBoolean(template.getConfiguration()));
                    } catch(NonBooleanException e) {
                        throw new ParseException("Expected a boolean as the value of the \"ignore_missing\" attribute",
                                ignoreMissingExp, e);
                    }
                } catch(TemplateException e) {
                    // evaluation of literals must not throw a TemplateException
                    throw new BugException(e);
                }
            } else {
                ignoreMissing = null;
            }
        }
    }

    void accept(Environment env) throws TemplateException, IOException {
        final String includedTemplateName = includedTemplateNameExp.evalAndCoerceToString(env);
        final String fullIncludedTemplateName;
        try {
            fullIncludedTemplateName = env.toFullTemplateName(getTemplate().getName(), includedTemplateName);
        } catch (MalformedTemplateNameException e) {
            throw new _MiscTemplateException(e, env, new Object[] {
                    "Malformed template name ", new _DelayedJQuote(e.getTemplateName()), ":\n",
                    e.getMalformednessDescription() });
        }
        
        final String encoding = this.encoding != null
                ? this.encoding
                : (encodingExp != null
                        ? encodingExp.evalAndCoerceToString(env)
                        : null);
        
        final boolean parse;
        if (this.parse != null) {
            parse = this.parse.booleanValue();
        } else {
            TemplateModel tm = parseExp.eval(env);
            if (tm instanceof TemplateScalarModel) {
                // Legacy
                parse = getYesNo(parseExp, EvalUtil.modelToString((TemplateScalarModel) tm, parseExp, env));
            } else {
                parse = parseExp.modelToBoolean(tm, env);
            }
        }
        
        final boolean ignoreMissing = this.ignoreMissing != null
                ? this.ignoreMissing.booleanValue()
                : ignoreMissingExp.evalToBoolean(env);
        
        final Template includedTemplate;
        try {
            includedTemplate = env.getTemplateForInclusion(fullIncludedTemplateName, encoding, parse, ignoreMissing);
        } catch (IOException e) {
            throw new _MiscTemplateException(e, env, new Object[] {
                    "Template inclusion failed (for parameter value ",
                    new _DelayedJQuote(includedTemplateName),
                    "):\n", new _DelayedGetMessage(e) });
        }
        if (includedTemplate != null) {
            env.include(includedTemplate);
        }
    }
    
    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(includedTemplateNameExp.getCanonicalForm());
        if (encodingExp != null) {
            buf.append(" encoding=").append(encodingExp.getCanonicalForm());
        }
        if (parseExp != null) {
            buf.append(" parse=").append(parseExp.getCanonicalForm());
        }
        if (ignoreMissingExp != null) {
            buf.append(" ignore_missing=").append(ignoreMissingExp.getCanonicalForm());
        }
        if (canonical) buf.append("/>");
        return buf.toString();
    }

    String getNodeTypeSymbol() {
        return "#include";
    }
    
    private boolean getYesNo(Expression exp, String s) throws TemplateException {
        try {
           return StringUtil.getYesNo(s);
        }
        catch (IllegalArgumentException iae) {
            throw new _MiscTemplateException(exp, new Object[] {
                     "Value must be boolean (or one of these strings: "
                     + "\"n\", \"no\", \"f\", \"false\", \"y\", \"yes\", \"t\", \"true\"), but it was ",
                     new _DelayedJQuote(s), "." });
        }
    }

    int getParameterCount() {
        return 3;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return includedTemplateNameExp;
        case 1: return parseExp;
        case 2: return encodingExp;
        case 3: return ignoreMissingExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.TEMPLATE_NAME;
        case 1: return ParameterRole.PARSE_PARAMETER;
        case 2: return ParameterRole.ENCODING_PARAMETER;
        case 3: return ParameterRole.IGNORE_MISSING_PARAMETER;
        default: throw new IndexOutOfBoundsException();
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }

/*
    boolean heedsOpeningWhitespace() {
        return true;
    }

    boolean heedsTrailingWhitespace() {
        return true;
    }
*/
}
