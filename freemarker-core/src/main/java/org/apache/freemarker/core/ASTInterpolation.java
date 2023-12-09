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

import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.TemplateLanguageUtils;

import java.io.IOException;
import java.io.Writer;

/**
 * AST interpolation node: <code>${exp}</code>
 */
//TODO [FM3] will be public
final class ASTInterpolation extends ASTElement {

    private final ASTExpression expression;
    
    /** For {@code #escape x as ...} (legacy auto-escaping) */
    private final ASTExpression escapedExpression;
    
    /** For OutputFormat-based auto-escaping */
    private final OutputFormat outputFormat;
    private final MarkupOutputFormat markupOutputFormat;
    private final boolean autoEscape;
    
    ASTInterpolation(
            ASTExpression expression, ASTExpression escapedExpression,
            OutputFormat outputFormat, boolean autoEscape) {
        this.expression = expression;
        this.escapedExpression = escapedExpression;
        this.outputFormat = outputFormat;
        markupOutputFormat
                = (MarkupOutputFormat) (outputFormat instanceof MarkupOutputFormat ? outputFormat : null);
        this.autoEscape = autoEscape;
    }

    /**
     * Outputs the string value of the enclosed expression.
     */
    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        printStringOrTemplateOutputModel(
                escapedExpression, outputFormat, markupOutputFormat, autoEscape, env);
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void printStringOrTemplateOutputModel(
            ASTExpression exp,
            OutputFormat outputFormat,
            MarkupOutputFormat markupOutputFormat, boolean autoEscape, 
            Environment env) throws IOException, TemplateException {
        final Writer out = env.getOut();
        
        final Object moOrStr = _EvalUtils.coerceModelToPlainTextOrMarkup(exp.eval(env), exp, null, env);
        if (moOrStr instanceof String) {
            final String s = (String) moOrStr;
            if (autoEscape) {
                markupOutputFormat.output(s, out);
            } else {
                out.write(s);
            }
        } else {
            final TemplateMarkupOutputModel mo = (TemplateMarkupOutputModel) moOrStr;
            _EvalUtils.printTemplateMarkupOutputModel(mo, outputFormat, out, exp);
        }
    }

    /**
     * Returns the already type-converted value that this interpolation will insert into the output.
     * 
     * @return A {@link String} or {@link TemplateMarkupOutputModel}. Not {@code null}.
     */
    Object calculateInterpolatedStringOrMarkup(Environment env) throws TemplateException {
        return _EvalUtils.coerceModelToPlainTextOrMarkup(escapedExpression.eval(env), escapedExpression, null, env);
    }

    @Override
    final String dump(boolean canonical) {
        return dump(canonical, false);
    }
    
    final String getCanonicalFormInStringLiteral() {
        return dump(true, true);
    }

    private String dump(boolean canonical, boolean inStringLiteral) {
        TemplateLanguage tempLang = getTemplate().getParsingConfiguration().getTemplateLanguage();
        if (!(tempLang instanceof DefaultTemplateLanguage)) {
            // TODO [FM3]
            throw new BugException("Dumping custom template langauges not yet supported");
        }
        InterpolationSyntax syntax = ((DefaultTemplateLanguage) tempLang).getInterpolationSyntax();

        StringBuilder sb = new StringBuilder();
        sb.append(syntax != InterpolationSyntax.SQUARE_BRACKET ? "${" : "[=");
        final String exprCF = expression.getCanonicalForm();
        sb.append(inStringLiteral ? TemplateLanguageUtils.escapeStringLiteralPart(exprCF, '"') : exprCF);
        sb.append(syntax != InterpolationSyntax.SQUARE_BRACKET ? "}" : "]");
        if (!canonical && expression != escapedExpression) {
            sb.append(" auto-escaped");            
        }
        return sb.toString();
    }
    
    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "${...}";
    }

    @Override
    boolean heedsOpeningWhitespace() {
        return true;
    }

    @Override
    boolean heedsTrailingWhitespace() {
        return true;
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return expression;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.CONTENT;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
