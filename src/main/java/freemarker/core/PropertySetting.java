/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * An instruction that sets a property of the template rendering
 * environment.
 */
final class PropertySetting extends TemplateElement {

    private final String key;
    private final Expression value;

    PropertySetting(String key, Expression value) {
        this.key = key;
        this.value = value;
    }

    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine)
    throws
        ParseException
    {
        super.setLocation(template, beginColumn, beginLine, endColumn, endLine);
        
        if (!key.equals(Configurable.LOCALE_KEY) &&
            !key.equals(Configurable.NUMBER_FORMAT_KEY) &&
            !key.equals(Configurable.TIME_FORMAT_KEY) &&
            !key.equals(Configurable.DATE_FORMAT_KEY) &&
            !key.equals(Configurable.DATETIME_FORMAT_KEY) &&
            !key.equals(Configurable.TIME_ZONE_KEY) &&
            !key.equals(Configurable.BOOLEAN_FORMAT_KEY) &&
            !key.equals(Configurable.CLASSIC_COMPATIBLE_KEY) &&
            !key.equals(Configurable.URL_ESCAPING_CHARSET_KEY)) 
        {
            throw new ParseException(
                    "Invalid setting name, or it's not allowed to change "
                    + "the value of the setting with FTL: "
                    + key,
                    template, beginLine, beginColumn);
        }
    }

    void accept(Environment env) throws TemplateException {
        TemplateModel mval = value.eval(env);
        String strval;
        if (mval instanceof TemplateScalarModel) {
            strval = ((TemplateScalarModel) mval).getAsString();
        } else if (mval instanceof TemplateBooleanModel) {
            strval = ((TemplateBooleanModel) mval).getAsBoolean() ? "true" : "false";
        } else if (mval instanceof TemplateNumberModel) {
            strval = ((TemplateNumberModel) mval).getAsNumber().toString();
        } else {
            strval = value.evalAndCoerceToString(env);
        }
        env.setSetting(key, strval);
    }
    
    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(key);
        sb.append('=');
        sb.append(value.getCanonicalForm());
        if (canonical) sb.append("/>");
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return "#setting";
    }

    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return key;
        case 1: return value;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ITEM_KEY;
        case 1: return ParameterRole.ITEM_VALUE;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
}
