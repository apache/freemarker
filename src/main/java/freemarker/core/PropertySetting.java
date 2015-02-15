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

import java.util.Arrays;

import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.StringUtil;

/**
 * An instruction that sets a property of the template rendering
 * environment.
 */
final class PropertySetting extends TemplateElement {

    private final String key;
    private final Expression value;
    
    static final String[] SETTING_NAMES = new String[] {
            Configurable.BOOLEAN_FORMAT_KEY,
            Configurable.CLASSIC_COMPATIBLE_KEY,
            Configurable.DATE_FORMAT_KEY,
            Configurable.DATETIME_FORMAT_KEY,
            Configurable.LOCALE_KEY,
            Configurable.NUMBER_FORMAT_KEY,
            Configurable.OUTPUT_ENCODING_KEY,
            Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY,
            Configurable.TIME_FORMAT_KEY,
            Configurable.TIME_ZONE_KEY,
            Configurable.URL_ESCAPING_CHARSET_KEY
    };

    PropertySetting(String key, Expression value) {
        this.key = key;
        this.value = value;
    }

    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine)
    throws
        ParseException
    {
        super.setLocation(template, beginColumn, beginLine, endColumn, endLine);
        
        if (Arrays.binarySearch(SETTING_NAMES, key) < 0) {
            StringBuffer sb = new StringBuffer();
            sb.append("Unknown setting name: ");
            sb.append(StringUtil.jQuote(key)).append(".");
            final String underscoredName = _CoreStringUtils.camelCaseToUnderscored(key);
            if (!underscoredName.equals(key) && Arrays.binarySearch(SETTING_NAMES, underscoredName) >= 0) {
                sb.append(" Supporting camelCase setting names is planned for FreeMarker 2.4.0; check if an update is "
                            + "available, and if it indeed supports camel case. "
                            + "Until that, use \"").append(underscoredName).append("\".");
            } else if (((Configurable) template).getSettingNames().contains(key)
                    || ((Configurable) template).getSettingNames().contains(underscoredName)) {
                sb.append(" The setting name is recognized, but changing this setting in a template isn't supported.");                
            } else {
                sb.append(" The allowed setting names are: ");
                for (int i = 0; i < SETTING_NAMES.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(SETTING_NAMES[i]);
                }
            }
            throw new ParseException(
                    sb.toString(),
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
        sb.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(key));
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

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}
