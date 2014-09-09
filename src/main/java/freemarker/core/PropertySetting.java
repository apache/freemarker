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
            !key.equals(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY) &&
            !key.equals(Configurable.BOOLEAN_FORMAT_KEY) &&
            !key.equals(Configurable.CLASSIC_COMPATIBLE_KEY) &&
            !key.equals(Configurable.URL_ESCAPING_CHARSET_KEY) &&
            !key.equals(Configurable.OUTPUT_ENCODING_KEY)
            ) 
        {
            throw new ParseException(
                    "Invalid setting name, or it's not allowed to change "
                    + "the value of the setting with FTL: "
                    + key,
                    template, beginLine, beginColumn, endLine, endColumn);
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
