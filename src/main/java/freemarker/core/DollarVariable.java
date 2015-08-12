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
import java.io.Writer;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateOutputModel;
import freemarker.template.utility.StringUtil;

/**
 * An instruction that outputs the value of an <tt>Expression</tt>.
 */
final class DollarVariable extends Interpolation {

    private final Expression expression;
    
    /** For {@code #escape x as ...} (legacy auto-escaping) */
    private final Expression escapedExpression;
    
    /** For OutputFormat-based auto-escaping */
    private final OutputFormat autoEscFormat;

    DollarVariable(Expression expression, Expression escapedExpression, OutputFormat autoEscFormat) {
        this.expression = expression;
        this.escapedExpression = escapedExpression;
        this.autoEscFormat = autoEscFormat;
    }

    /**
     * Outputs the string value of the enclosed expression.
     */
    @Override
    void accept(Environment env) throws TemplateException, IOException {
        TemplateModel tm = escapedExpression.eval(env);
        Writer out = env.getOut();
        String s = EvalUtil.coerceModelToString(tm, escapedExpression, null, true, env);
        if (s != null) {
            if (autoEscFormat != null) {
                autoEscFormat.output(s, out);
            } else {
                out.write(s);
            }
        } else {
            TemplateOutputModel tom = (TemplateOutputModel) tm;
            OutputFormat tomOF = tom.getOutputFormat();
            if (autoEscFormat != null && tomOF != autoEscFormat) {
                throw new _MiscTemplateException(env,
                        "Incompatible output formats. The current auto-escaping output format is ",
                        new _DelayedJQuote(autoEscFormat.getMimeType()),
                        " (", new _DelayedToString(autoEscFormat),
                        "), while the value to print belongs to the ", 
                        new _DelayedJQuote(tomOF.getMimeType()), " output format (",
                        new _DelayedToString(tomOF));
            }
            tomOF.output(tom, out);
        }
    }

    @Override
    protected String dump(boolean canonical, boolean inStringLiteral) {
        StringBuilder sb = new StringBuilder();
        sb.append("${");
        final String exprCF = expression.getCanonicalForm();
        sb.append(inStringLiteral ? StringUtil.FTLStringLiteralEnc(exprCF, '"') : exprCF);
        sb.append("}");
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
