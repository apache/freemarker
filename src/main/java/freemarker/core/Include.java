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

import freemarker.cache.TemplateCache;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.UndeclaredThrowableException;


/**
 * An instruction that gets another template
 * and processes it within the current template.
 */
final class Include extends TemplateElement {

    private Expression templateName, encodingExp, parseExp;
    private String encoding;
    private boolean parse;
    private final String templatePath;

    /**
     * @param template the template that this <tt>Include</tt> is a part of.
     * @param includedTemplateName the name of the template to be included.
     * @param encodingExp the encoding to be used or null, if it's a default.
     * @param parseExp whether the template should be parsed (or is raw text)
     */
    Include(Template template,
            Expression includedTemplateName,
            Expression encodingExp,
            Expression parseExp) throws ParseException
    {
        String templatePath1 = template.getName();
        if (templatePath1 == null) {
            // This can be the case if the template wasn't created throuh a TemplateLoader. 
            templatePath1 = "";
        }
        int lastSlash = templatePath1.lastIndexOf('/');
        templatePath = lastSlash == -1 ? "" : templatePath1.substring(0, lastSlash + 1);
        this.templateName = includedTemplateName;
        if (encodingExp instanceof StringLiteral) {
            encoding = encodingExp.toString();
            encoding = encoding.substring(1, encoding.length() -1);
        }
        else {
            this.encodingExp = encodingExp;
        }
        if(parseExp == null) {
            parse = true;
        }
        else if(parseExp.isLiteral()) {
            try {
                if (parseExp instanceof StringLiteral) {
                    parse = StringUtil.getYesNo(parseExp.evalAndCoerceToString(null));
                }
                else {
                    try {
                        parse = parseExp.evalToBoolean(null);
                    }
                    catch(NonBooleanException e) {
                        throw new ParseException("Expected a boolean or string as the value of the parse attribute", parseExp);
                    }
                }
            }
            catch(TemplateException e) {
                // evaluation of literals must not throw a TemplateException
                throw new UndeclaredThrowableException(e);
            }
        }
        else {
            this.parseExp = parseExp;
        }
    }

    void accept(Environment env) throws TemplateException, IOException {
        String templateNameString = templateName.evalAndCoerceToString(env);
        String enc = encoding;
        if (encoding == null && encodingExp != null) {
            enc = encodingExp.evalAndCoerceToString(env);
        }
        
        boolean parse = this.parse;
        if (parseExp != null) {
            TemplateModel tm = parseExp.eval(env);
            if(tm == null) {
                if(env.isClassicCompatible()) {
                    parse = false;
                }
                else {
                    parseExp.assertNonNull(tm, env);
                }
            }
            if (tm instanceof TemplateScalarModel) {
                parse = getYesNo(EvalUtil.modelToString((TemplateScalarModel)tm, parseExp, env));
            }
            else {
                parse = parseExp.evalToBoolean(env);
            }
        }
        
        Template includedTemplate;
        try {
            templateNameString = TemplateCache.getFullTemplatePath(env, templatePath, templateNameString);
            includedTemplate = env.getTemplateForInclusion(templateNameString, enc, parse);
        }
        catch (ParseException pe) {
            throw new _MiscTemplateException(pe, env, new Object[] {
                    "Error parsing included template ",
                    new _DelayedJQuote(templateNameString), ":\n",
                    new _DelayedGetMessage(pe) });
        }
        catch (IOException ioe) {
            throw new _MiscTemplateException(ioe, env, new Object[] {
                    "Error reading included file ", new _DelayedJQuote(templateNameString), ":\n",
                    new _DelayedGetMessage(ioe) });
        }
        env.include(includedTemplate);
    }
    
    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(templateName.getCanonicalForm());
        if (encoding != null) {
            buf.append(" encoding=\"");
            buf.append(encodingExp.getCanonicalForm());
            buf.append('"');
        }
        if(parseExp != null) {
            buf.append(" parse=" + parseExp.getCanonicalForm());
        } else if (!parse) {
            buf.append(" parse=false");
        }
        if (canonical) buf.append("/>");
        return buf.toString();
    }

    String getNodeTypeSymbol() {
        return "#include";
    }
    
    private boolean getYesNo(String s) throws TemplateException {
        try {
           return StringUtil.getYesNo(s);
        }
        catch (IllegalArgumentException iae) {
            throw new _MiscTemplateException(parseExp, new Object[] {
                     "Value of include parse parameter must be boolean (or one of these strings: "
                     + "\"n\", \"no\", \"f\", \"false\", \"y\", \"yes\", \"t\", \"true\"), but it was ",
                     new _DelayedJQuote(s), "." });
        }
    }

    int getParameterCount() {
        return 3;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return templateName;
        case 1: return new Boolean(parse);
        case 2: return encoding;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.TEMPLATE_NAME;
        case 1: return ParameterRole.PARSE_PARAMETER;
        case 2: return ParameterRole.ENCODING_PARAMETER;
        default: throw new IndexOutOfBoundsException();
        }
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
