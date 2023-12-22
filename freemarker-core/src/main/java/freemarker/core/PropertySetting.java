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

import java.util.Arrays;

import freemarker.template.Configuration;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.StringUtil;

/**
 * An instruction that sets a property of the template rendering
 * environment.
 */
final class PropertySetting extends TemplateElement {

    private final String key;
    private final Expression value;
    private final ValueSafetyChecker valueSafetyChecker;
    
    static final String[] SETTING_NAMES = new String[] {
            // Must be sorted alphabetically!
            Configurable.BOOLEAN_FORMAT_KEY_CAMEL_CASE,
            Configurable.BOOLEAN_FORMAT_KEY_SNAKE_CASE,
            Configurable.C_FORMAT_KEY_CAMEL_CASE,
            Configurable.C_FORMAT_KEY_SNAKE_CASE,
            Configurable.CLASSIC_COMPATIBLE_KEY_CAMEL_CASE,
            Configurable.CLASSIC_COMPATIBLE_KEY_SNAKE_CASE,
            Configurable.DATE_FORMAT_KEY_CAMEL_CASE,
            Configurable.DATE_FORMAT_KEY_SNAKE_CASE,
            Configurable.DATETIME_FORMAT_KEY_CAMEL_CASE,
            Configurable.DATETIME_FORMAT_KEY_SNAKE_CASE,
            Configurable.LOCALE_KEY,
            Configurable.NUMBER_FORMAT_KEY_CAMEL_CASE,
            Configurable.NUMBER_FORMAT_KEY_SNAKE_CASE,
            Configurable.OUTPUT_ENCODING_KEY_CAMEL_CASE,
            Configurable.OUTPUT_ENCODING_KEY_SNAKE_CASE,
            Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE,
            Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY,
            Configurable.TIME_FORMAT_KEY_CAMEL_CASE,
            Configurable.TIME_ZONE_KEY_CAMEL_CASE,
            Configurable.TIME_FORMAT_KEY_SNAKE_CASE,
            Configurable.TIME_ZONE_KEY_SNAKE_CASE,
            Configurable.URL_ESCAPING_CHARSET_KEY_CAMEL_CASE,
            Configurable.URL_ESCAPING_CHARSET_KEY_SNAKE_CASE
    };

    PropertySetting(Token keyTk, FMParserTokenManager tokenManager, Expression value, Configuration cfg)
            throws ParseException {
        final String key = keyTk.image;
        if (Arrays.binarySearch(SETTING_NAMES, key) < 0) {
            StringBuilder sb = new StringBuilder();
            if (_TemplateAPI.getConfigurationSettingNames(cfg, true).contains(key)
                    || _TemplateAPI.getConfigurationSettingNames(cfg, false).contains(key)) {
                sb.append("The setting name is recognized, but changing this setting from inside a template isn't "
                        + "supported.");                
            } else {
                sb.append("Unknown setting name: ");
                sb.append(StringUtil.jQuote(key)).append(".");
                sb.append(" The allowed setting names are: ");

                int shownNamingConvention;
                {
                    int namingConvention = tokenManager.namingConvention;
                    shownNamingConvention = namingConvention != Configuration.AUTO_DETECT_NAMING_CONVENTION
                            ? namingConvention : Configuration.LEGACY_NAMING_CONVENTION /* [2.4] CAMEL_CASE */; 
                }
                
                boolean first = true;
                for (int i = 0; i < SETTING_NAMES.length; i++) {
                    String correctName = SETTING_NAMES[i];
                    int correctNameNamingConvetion = _CoreStringUtils.getIdentifierNamingConvention(correctName);
                    if (shownNamingConvention == Configuration.CAMEL_CASE_NAMING_CONVENTION 
                            ? correctNameNamingConvetion != Configuration.LEGACY_NAMING_CONVENTION
                            : correctNameNamingConvetion != Configuration.CAMEL_CASE_NAMING_CONVENTION) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }
                    
                        sb.append(SETTING_NAMES[i]);
                    }
                }
            }
            throw new ParseException(sb.toString(), null, keyTk);
        }
        
        this.key = key;
        this.value = value;

        if (key.equals(Configurable.C_FORMAT_KEY_SNAKE_CASE) || key.equals(Configurable.C_FORMAT_KEY_CAMEL_CASE)) {
            valueSafetyChecker = new ValueSafetyChecker() {
                @Override
                public void check(Environment env, String actualValue) throws TemplateException {
                    if (actualValue.startsWith("@")
                            || StandardCFormats.STANDARD_C_FORMATS.containsKey(actualValue)
                            || actualValue.equals("default")) {
                        return;
                    }
                    throw new TemplateException("It's not allowed to set \"" + key + "\" to "
                            + StringUtil.jQuote(actualValue) + " in a template. Use a standard c format name ("
                            + _CoreStringUtils.commaSeparatedJQuotedItems(StandardCFormats.STANDARD_C_FORMATS.keySet()) + "), " +
                            "or registered custom  c format name after a \"@\".",
                            env);
                }
            };
        } else {
            valueSafetyChecker = null;
        }
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException {
        TemplateModel mval = value.eval(env);
        String strval;
        if (mval instanceof TemplateScalarModel) {
            strval = ((TemplateScalarModel) mval).getAsString();
        } else if (mval instanceof TemplateBooleanModel) {
            strval = ((TemplateBooleanModel) mval).getAsBoolean() ? "true" : "false";
        } else if (mval instanceof TemplateNumberModel) {
            strval = ((TemplateNumberModel) mval).getAsNumber().toString();
        } else {
            strval = value.evalAndCoerceToStringOrUnsupportedMarkup(env);
        }
        if (valueSafetyChecker != null) {
            valueSafetyChecker.check(env, strval);
        }
        env.setSetting(key, strval);
        return null;
    }
    
    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(key));
        sb.append('=');
        sb.append(value.getCanonicalForm());
        if (canonical) sb.append("/>");
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "#setting";
    }

    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return key;
        case 1: return value;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ITEM_KEY;
        case 1: return ParameterRole.ITEM_VALUE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    private interface ValueSafetyChecker {
        void check(Environment env, String value) throws TemplateException;
    }
    
}
