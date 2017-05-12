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

import java.util.Arrays;

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.util._StringUtil;

/**
 * AST directive node: {@code #setting}.
 */
final class ASTDirSetting extends ASTDirective {

    private final String key;
    private final ASTExpression value;
    
    static final String[] SETTING_NAMES = new String[] {
            // Must be sorted alphabetically!
            MutableProcessingConfiguration.BOOLEAN_FORMAT_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.BOOLEAN_FORMAT_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.DATE_FORMAT_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.DATE_FORMAT_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.DATETIME_FORMAT_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.DATETIME_FORMAT_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.LOCALE_KEY,
            MutableProcessingConfiguration.NUMBER_FORMAT_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.NUMBER_FORMAT_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.OUTPUT_ENCODING_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.OUTPUT_ENCODING_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY,
            MutableProcessingConfiguration.TIME_FORMAT_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.TIME_ZONE_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.TIME_FORMAT_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.TIME_ZONE_KEY_SNAKE_CASE,
            MutableProcessingConfiguration.URL_ESCAPING_CHARSET_KEY_CAMEL_CASE,
            MutableProcessingConfiguration.URL_ESCAPING_CHARSET_KEY_SNAKE_CASE
    };

    ASTDirSetting(Token keyTk, FMParserTokenManager tokenManager, ASTExpression value, Configuration cfg)
            throws ParseException {
        String key = keyTk.image;
        if (Arrays.binarySearch(SETTING_NAMES, key) < 0) {
            StringBuilder sb = new StringBuilder();
            if (Configuration.ExtendableBuilder.getSettingNames(true).contains(key)
                    || Configuration.ExtendableBuilder.getSettingNames(false).contains(key)) {
                sb.append("The setting name is recognized, but changing this setting from inside a template isn't "
                        + "supported.");                
            } else {
                sb.append("Unknown setting name: ");
                sb.append(_StringUtil.jQuote(key)).append(".");
                sb.append(" The allowed setting names are: ");

                int shownNamingConvention;
                {
                    int namingConvention = tokenManager.namingConvention;
                    shownNamingConvention = namingConvention != ParsingConfiguration.AUTO_DETECT_NAMING_CONVENTION
                            ? namingConvention : ParsingConfiguration.LEGACY_NAMING_CONVENTION /* [2.4] CAMEL_CASE */;
                }
                
                boolean first = true;
                for (String correctName : SETTING_NAMES) {
                    int correctNameNamingConvention = _StringUtil.getIdentifierNamingConvention(correctName);
                    if (shownNamingConvention == ParsingConfiguration.CAMEL_CASE_NAMING_CONVENTION
                            ? correctNameNamingConvention != ParsingConfiguration.LEGACY_NAMING_CONVENTION
                            : correctNameNamingConvention != ParsingConfiguration.CAMEL_CASE_NAMING_CONVENTION) {
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
    ASTElement[] accept(Environment env) throws TemplateException {
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
        try {
            env.setSetting(key, strval);
        } catch (ConfigurationException e) {
            throw new _MiscTemplateException(env, e.getMessage(), e.getCause());
        }
        return null;
    }
    
    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(_StringUtil.toFTLTopLevelTragetIdentifier(key));
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
    
}
