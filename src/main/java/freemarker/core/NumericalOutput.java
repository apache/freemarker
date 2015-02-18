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
import java.text.NumberFormat;
import java.util.Locale;

import freemarker.template.TemplateException;

/**
 * An instruction that outputs the value of a numerical expression.
 */
final class NumericalOutput extends TemplateElement {

    private final Expression expression;
    private final boolean hasFormat;
    private final int minFracDigits;
    private final int maxFracDigits;
    private volatile FormatHolder formatCache; // creating new NumberFormat is slow operation

    NumericalOutput(Expression expression) {
        this.expression = expression;
        hasFormat = false;
        this.minFracDigits = 0;
        this.maxFracDigits = 0;
    }

    NumericalOutput(Expression expression,
                    int minFracDigits,
                    int maxFracDigits) 
    {
        this.expression = expression;
        hasFormat = true;
        this.minFracDigits = minFracDigits;
        this.maxFracDigits = maxFracDigits;
    }

    void accept(Environment env) throws TemplateException, IOException 
    {
        Number num = expression.evalToNumber(env);
        
        FormatHolder fmth = formatCache;  // atomic sampling
        if (fmth == null || !fmth.locale.equals(env.getLocale())) {
            synchronized(this) {
                fmth = formatCache;
                if (fmth == null || !fmth.locale.equals(env.getLocale())) {
                    NumberFormat fmt = NumberFormat.getNumberInstance(env.getLocale());
                    if (hasFormat) {
                        fmt.setMinimumFractionDigits(minFracDigits);
                        fmt.setMaximumFractionDigits(maxFracDigits);
                    } else {
                        fmt.setMinimumFractionDigits(0);
                        fmt.setMaximumFractionDigits(50);
                    }
                    fmt.setGroupingUsed(false);
                    formatCache = new FormatHolder(fmt, env.getLocale());
                    fmth = formatCache;
                }
            }
        }
        // We must use Format even if hasFormat == false.
        // Some locales may use non-Arabic digits, thus replacing the
        // decimal separator in the result of toString() is not enough.
        env.getOut().write(fmth.format.format(num));
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer("#{");
        buf.append(expression.getCanonicalForm());
        if (hasFormat) {
            buf.append(" ; ");
            buf.append("m");
            buf.append(minFracDigits);
            buf.append("M");
            buf.append(maxFracDigits);
        }
        buf.append("}");
        return buf.toString();
    }
    
    String getNodeTypeSymbol() {
        return "#{...}";
    }

    boolean heedsOpeningWhitespace() {
        return true;
    }

    boolean heedsTrailingWhitespace() {
        return true;
    }
    
    private static class FormatHolder {
        final NumberFormat format;
        final Locale locale;
        
        FormatHolder(NumberFormat format, Locale locale) {
            this.format = format;
            this.locale = locale;
        }
    }

    int getParameterCount() {
        return 3;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return expression;
        case 1: return new Integer(minFracDigits);
        case 2: return new Integer(maxFracDigits);
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.CONTENT;
        case 1: return ParameterRole.MINIMUM_DECIMALS;
        case 2: return ParameterRole.MAXIMUM_DECIMALS;
        default: throw new IndexOutOfBoundsException();
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
}
