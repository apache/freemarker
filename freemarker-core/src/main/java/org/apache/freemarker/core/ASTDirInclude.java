/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import java.io.IOException;

import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST directive node: {@code #include} 
 */
final class ASTDirInclude extends ASTDirective {

    private final ASTExpression includedTemplateNameExp, ignoreMissingExp;
    private final Boolean ignoreMissingExpPrecalcedValue;

    /**
     * @param template the template that this <tt>#include</tt> is a part of.
     * @param includedTemplateNameExp the path of the template to be included.
     */
    ASTDirInclude(Template template,
            ASTExpression includedTemplateNameExp,
            ASTExpression ignoreMissingExp) throws ParseException {
        this.includedTemplateNameExp = includedTemplateNameExp;

        this.ignoreMissingExp = ignoreMissingExp;
        if (ignoreMissingExp != null && ignoreMissingExp.isLiteral()) {
            try {
                ignoreMissingExpPrecalcedValue = Boolean.valueOf(
                        ignoreMissingExp.evalToBoolean(template.getConfiguration()));
            } catch (TemplateException e) {
                throw new ParseException("Expected a boolean as the value of the \"ignore_missing\" attribute",
                        ignoreMissingExp, e);
            }
        } else {
            ignoreMissingExpPrecalcedValue = null;
        }
    }
    
    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        final String includedTemplateName = includedTemplateNameExp.evalAndCoerceToPlainText(env);
        final String fullIncludedTemplateName;
        try {
            fullIncludedTemplateName = env.toFullTemplateName(getTemplate().getLookupName(), includedTemplateName);
        } catch (MalformedTemplateNameException e) {
            throw new TemplateException(e, env,
                    "Malformed template name ", new _DelayedJQuote(e.getTemplateName()), ":\n",
                    e.getMalformednessDescription());
        }
        
        final boolean ignoreMissing;
        if (ignoreMissingExpPrecalcedValue != null) {
            ignoreMissing = ignoreMissingExpPrecalcedValue.booleanValue();
        } else if (ignoreMissingExp != null) {
            ignoreMissing = ignoreMissingExp.evalToBoolean(env);
        } else {
            ignoreMissing = false;
        }
        
        final Template includedTemplate;
        try {
            includedTemplate = env.getTemplateForInclusion(fullIncludedTemplateName, ignoreMissing);
        } catch (IOException e) {
            throw new TemplateException(e, env,
                    "Template inclusion failed (for parameter value ",
                    new _DelayedJQuote(includedTemplateName),
                    "):\n", new _DelayedGetMessage(e));
        }
        
        if (includedTemplate != null) {
            env.include(includedTemplate);
        }
        return null;
    }
    
    @Override
    String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getLabelWithoutParameters());
        buf.append(' ');
        buf.append(includedTemplateNameExp.getCanonicalForm());
        if (ignoreMissingExp != null) {
            buf.append(" ignore_missing=").append(ignoreMissingExp.getCanonicalForm());
        }
        if (canonical) buf.append("/>");
        return buf.toString();
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#include";
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return includedTemplateNameExp;
        case 1: return ignoreMissingExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.TEMPLATE_NAME;
        case 1: return ParameterRole.IGNORE_MISSING_PARAMETER;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    private boolean getYesNo(ASTExpression exp, String s) throws TemplateException {
        try {
           return _StringUtils.getYesNo(s);
        } catch (IllegalArgumentException iae) {
            throw new TemplateException(exp,
                     "Value must be boolean (or one of these strings: "
                     + "\"n\", \"no\", \"f\", \"false\", \"y\", \"yes\", \"t\", \"true\"), but it was ",
                     new _DelayedJQuote(s), ".");
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
    
    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}
