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

package freemarker.core;

import java.io.IOException;
import java.io.Writer;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

/**
 * An interpolation like <code>${exp}</code> or {@code [=exp]}. The class name is the remnant of old times, but as
 * some users are using the package-visible AST API, it wasn't renamed.
 * 
 * @see NumericalOutput
 */
final class DollarVariable extends Interpolation {

    private final Expression expression;
    
    /** For {@code #escape x as ...} (legacy auto-escaping) */
    private final Expression escapedExpression;
    
    /** For OutputFormat-based auto-escaping */
    private final OutputFormat outputFormat;
    private final MarkupOutputFormat markupOutputFormat;
    private final boolean autoEscape;

    DollarVariable(
            Expression expression, Expression escapedExpression,
            OutputFormat outputFormat, boolean autoEscape) {
        this.expression = expression;
        this.escapedExpression = escapedExpression;
        this.outputFormat = outputFormat;
        this.markupOutputFormat
                = (MarkupOutputFormat) (outputFormat instanceof MarkupOutputFormat ? outputFormat : null);
        this.autoEscape = autoEscape;
    }

    /**
     * Outputs the string value of the enclosed expression.
     */
    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        final Object moOrStr = calculateInterpolatedStringOrMarkup(env);
        final Writer out = env.getOut();
        if (moOrStr instanceof String) {
            final String s = (String) moOrStr;
            if (autoEscape) {
                markupOutputFormat.output(s, out);
            } else {
                out.write(s);
            }
        } else {
            final TemplateMarkupOutputModel mo = (TemplateMarkupOutputModel) moOrStr;
            final MarkupOutputFormat moOF = mo.getOutputFormat();
            // ATTENTION: Keep this logic in sync. ?esc/?noEsc's logic!
            if (moOF == outputFormat) {
                moOF.output(mo, out);
            } else if (!outputFormat.isOutputFormatMixingAllowed()) {
                final String srcPlainText;
                // ATTENTION: Keep this logic in sync. ?esc/?noEsc's logic!
                srcPlainText = moOF.getSourcePlainText(mo);
                if (srcPlainText == null) {
                    throw new _TemplateModelException(escapedExpression,
                            "The value to print is in ", new _DelayedToString(moOF),
                            " format, which differs from the current output format, ",
                            new _DelayedToString(outputFormat), ". Format conversion wasn't possible.");
                }
                if (markupOutputFormat != null) {
                    markupOutputFormat.output(srcPlainText, out);
                } else {
                    out.write(srcPlainText);
                }
            } else if (markupOutputFormat != null) {
                markupOutputFormat.outputForeign(mo, out);
            } else {
                moOF.output(mo, out);
            }
        }
        return null;
    }

    @Override
    protected Object calculateInterpolatedStringOrMarkup(Environment env) throws TemplateException {
        return EvalUtil.coerceModelToStringOrMarkup(escapedExpression.eval(env), escapedExpression, null, env);
    }

    @Override
    protected String dump(boolean canonical, boolean inStringLiteral) {
        StringBuilder sb = new StringBuilder();
        int syntax = getTemplate().getInterpolationSyntax();
        sb.append(syntax != Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX ? "${" : "[=");
        final String exprCF = expression.getCanonicalForm();
        sb.append(inStringLiteral ? StringUtil.FTLStringLiteralEnc(exprCF, '"') : exprCF);
        sb.append(syntax != Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX ? "}" : "]");
        if (!canonical && expression != escapedExpression) {
            sb.append(" auto-escaped");            
        }
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
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
