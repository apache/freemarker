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

import java.util.Set;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST directive node: {@code #setting}.
 */
final class ASTDirSetting extends ASTDirective {

    private final String key;
    private final ASTExpression value;
    
    static final Set<String> SETTING_NAMES = new _SortedArraySet<>(
            // Must be sorted alphabetically!
            MutableProcessingConfiguration.BOOLEAN_FORMAT_KEY,
            MutableProcessingConfiguration.DATE_FORMAT_KEY,
            MutableProcessingConfiguration.DATE_TIME_FORMAT_KEY,
            MutableProcessingConfiguration.LOCALE_KEY,
            MutableProcessingConfiguration.NUMBER_FORMAT_KEY,
            MutableProcessingConfiguration.OUTPUT_ENCODING_KEY,
            MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY,
            MutableProcessingConfiguration.TIME_FORMAT_KEY,
            MutableProcessingConfiguration.TIME_ZONE_KEY,
            MutableProcessingConfiguration.URL_ESCAPING_CHARSET_KEY
    );

    ASTDirSetting(Token keyTk, FMParserTokenManager tokenManager, ASTExpression value, Configuration cfg)
            throws ParseException {
        String key = keyTk.image;
        if (!SETTING_NAMES.contains(key)) {
            StringBuilder sb = new StringBuilder();
            if (Configuration.ExtendableBuilder.getSettingNames().contains(key)) {
                sb.append("The setting name is recognized, but changing this setting from inside a template isn't "
                        + "supported.");                
            } else {
                sb.append("Unknown setting name: ");
                sb.append(_StringUtils.jQuote(key)).append(".");

                String correctedKey;
                if (key.indexOf('_') != -1) {
                    sb.append(MessageUtils.FM3_SNAKE_CASE);
                    correctedKey = _StringUtils.snakeCaseToCamelCase(key);
                    if (!SETTING_NAMES.contains(correctedKey)) {
                        if (key.equals("datetime_format")) {
                            correctedKey = "dateTimeFormat";
                        } else {
                            correctedKey = null;
                        }
                    }
                } else if (key.equals("datetimeFormat")) {
                    correctedKey = "dateTimeFormat";
                } else {
                    correctedKey = null;
                }

                if (correctedKey != null) {
                    sb.append("\nThe correct name is: ").append(correctedKey);
                } else {
                    sb.append("\nThe allowed setting names are: ");

                    boolean first = true;
                    for (String correctName : SETTING_NAMES) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }

                        sb.append(correctName);
                    }
                }
            }
            throw new ParseException(sb.toString(), null, keyTk);
        }
        
        this.key = key;
        this.value = value;
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException {
        TemplateModel mval = value.eval(env);
        String strval;
        if (mval instanceof TemplateStringModel) {
            strval = ((TemplateStringModel) mval).getAsString();
        } else if (mval instanceof TemplateBooleanModel) {
            strval = ((TemplateBooleanModel) mval).getAsBoolean() ? "true" : "false";
        } else if (mval instanceof TemplateNumberModel) {
            strval = ((TemplateNumberModel) mval).getAsNumber().toString();
        } else {
            strval = value.evalAndCoerceToStringOrUnsupportedMarkup(env);
        }
        try {
            env.setSetting(key, strval);
        } catch (ConfigurationException e) {
            throw new TemplateException(env, e.getMessage(), e.getCause());
        }
        return null;
    }
    
    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
        sb.append(' ');
        sb.append(_StringUtils.toFTLTopLevelTragetIdentifier(key));
        sb.append('=');
        sb.append(value.getCanonicalForm());
        if (canonical) sb.append("/>");
        return sb.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
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
    
}
