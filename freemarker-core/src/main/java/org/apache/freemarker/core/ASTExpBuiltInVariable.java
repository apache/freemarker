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

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Set;

import org.apache.freemarker.core.ASTDirMacroOrFunction.Context;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SimpleDate;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST expression node: {@code .name}
 */
final class ASTExpBuiltInVariable extends ASTExpression {

    static final String MAIN_TEMPLATE_NAME = "mainTemplateName";
    static final String CURRENT_TEMPLATE_NAME = "currentTemplateName";
    static final String NAMESPACE = "namespace";
    static final String MAIN = "main";
    static final String GLOBALS = "globals";
    static final String LOCALS = "locals";
    static final String DATA_MODEL = "dataModel";
    static final String LANG = "lang";
    static final String LOCALE = MutableProcessingConfiguration.LOCALE_KEY;
    static final String LOCALE_OBJECT = MutableProcessingConfiguration.LOCALE_KEY + "Object";
    static final String NODE = "node";
    static final String PASS = "pass";
    static final String VARS = "vars";
    static final String VERSION = "version";
    static final String INCOMPATIBLE_IMPROVEMENTS = Configuration.Builder.INCOMPATIBLE_IMPROVEMENTS_KEY;
    static final String ERROR = "error";
    static final String OUTPUT_ENCODING = MutableProcessingConfiguration.OUTPUT_ENCODING_KEY;
    static final String OUTPUT_FORMAT = MutableParsingAndProcessingConfiguration.OUTPUT_FORMAT_KEY;
    static final String AUTO_ESC = "autoEsc";
    static final String URL_ESCAPING_CHARSET = MutableProcessingConfiguration.URL_ESCAPING_CHARSET_KEY;
    static final String NOW = "now";
    static final String GET_OPTIONAL_TEMPLATE = "getOptionalTemplate";
    static final String CALLER_TEMPLATE_NAME = "callerTemplateName";    
    
    static final Set<String> BUILT_IN_VARIABLE_NAMES = new _SortedArraySet<>(
        // IMPORTANT! Keep this sorted alphabetically!
        AUTO_ESC,
        CALLER_TEMPLATE_NAME,
        CURRENT_TEMPLATE_NAME,
        DATA_MODEL,
        ERROR,
        GET_OPTIONAL_TEMPLATE,
        GLOBALS,
        INCOMPATIBLE_IMPROVEMENTS,
        LANG,
        LOCALE,
        LOCALE_OBJECT,
        LOCALS,
        MAIN,
        MAIN_TEMPLATE_NAME,
        NAMESPACE,
        NODE,
        NOW,
        OUTPUT_ENCODING,
        OUTPUT_FORMAT,
        PASS,
        URL_ESCAPING_CHARSET,
        VARS,
        VERSION
    );

    private final String name;
    private final TemplateModel parseTimeValue;

    ASTExpBuiltInVariable(Token nameTk, FMParserTokenManager tokenManager, TemplateModel parseTimeValue)
            throws ParseException {
        String name = nameTk.image;
        this.parseTimeValue = parseTimeValue;
        if (!BUILT_IN_VARIABLE_NAMES.contains(name)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown special variable name: ");
            sb.append(_StringUtils.jQuote(name)).append(".");

            String correctedName;
            if (name.indexOf('_') != -1) {
                sb.append(MessageUtils.FM3_SNAKE_CASE);
                correctedName = _StringUtils.snakeCaseToCamelCase(name);
                if (!BUILT_IN_VARIABLE_NAMES.contains(correctedName)) {
                    correctedName = null;
                }
            } else if (name.equals("auto_escape") || name.equals("auto_escaping") || name.equals("autoEsc")
                    || name.equals("autoEscape") || name.equals("autoEscaping")) {
                correctedName = "autoEsc";
            } else {
                correctedName = null;
            }

            if (correctedName != null) {
                sb.append("\nThe correct name is: ").append(correctedName);
            } else {
                sb.append("\nThe supported special variable names are: ");
                boolean first = true;
                for (final String supportedName : BUILT_IN_VARIABLE_NAMES) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(supportedName);
                }
            }
            throw new ParseException(sb.toString(), null, nameTk);
        }
        
        this.name = name.intern();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        if (parseTimeValue != null) {
            return parseTimeValue;
        }
        if (name == NAMESPACE) {
            return env.getCurrentNamespace();
        }
        if (name == MAIN) {
            return env.getMainNamespace();
        }
        if (name == GLOBALS) {
            return env.getGloballyVisibleVariables();
        }
        if (name == LOCALS) {
            ASTDirMacroOrFunction.Context ctx = env.getCurrentMacroContext();
            return ctx == null ? null : ctx.getLocals();
        }
        if (name == DATA_MODEL) {
            return env.getDataModelWithSharedVariableFallback();
        }
        if (name == VARS) {
            return new VarsHash(env);
        }
        if (name == LOCALE) {
            return new SimpleString(env.getLocale().toString());
        }
        if (name == LOCALE_OBJECT) {
            return env.getObjectWrapper().wrap(env.getLocale());
        }
        if (name == LANG) {
            return new SimpleString(env.getLocale().getLanguage());
        }
        if (name == NODE) {
            return env.getCurrentVisitorNode();
        }
        if (name == MAIN_TEMPLATE_NAME) {
            return SimpleString.newInstanceOrNull(env.getMainTemplate().getLookupName());
        }
        if (name == CURRENT_TEMPLATE_NAME) {
            return SimpleString.newInstanceOrNull(env.getCurrentTemplate().getLookupName());
        }
        if (name == PASS) {
            return ASTDirMacroOrFunction.PASS_MACRO;
        }
        if (name == OUTPUT_ENCODING) {
            Charset encoding = env.getOutputEncoding();
            return encoding != null ? new SimpleString(encoding.name()) : null;
        }
        if (name == URL_ESCAPING_CHARSET) {
            Charset charset = env.getURLEscapingCharset();
            return charset != null ? new SimpleString(charset.name()) : null;
        }
        if (name == ERROR) {
            return new SimpleString(env.getCurrentRecoveredErrorMessage());
        }
        if (name == NOW) {
            return new SimpleDate(new Date(), TemplateDateModel.DATE_TIME);
        }
        if (name == VERSION) {
            return new SimpleString(Configuration.getVersion().toString());
        }
        if (name == INCOMPATIBLE_IMPROVEMENTS) {
            return new SimpleString(env.getConfiguration().getIncompatibleImprovements().toString());
        }
        if (name == GET_OPTIONAL_TEMPLATE) {
            return GetOptionalTemplateFunction.INSTANCE;
        }        
        if (name == CALLER_TEMPLATE_NAME) {
            Context ctx = env.getCurrentMacroContext();
            if (ctx == null) {
                // TODO [FM3] Adjust error message if the special variable syntax is not `.someName` anymore.
                throw new TemplateException(
                        "Can't get ." + CALLER_TEMPLATE_NAME + " here, as there's no macro or function (that's "
                        + "implemented in the template) call in context.");
            }
            String name = ctx.callPlace.getTemplate().getLookupName();
            return name != null ? new SimpleString(name) : TemplateStringModel.EMPTY_STRING;
        }
        
        throw new TemplateException(this,
                "Invalid special variable: ", name);
    }

    @Override
    public String getCanonicalForm() {
        return "." + name;
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return getCanonicalForm();
    }

    @Override
    boolean isLiteral() {
        return false;
    }

    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        return this;
    }

    static class VarsHash implements TemplateHashModel {
        Environment env;
        
        VarsHash(Environment env) {
            this.env = env;
        }
        
        @Override
        public TemplateModel get(String key) throws TemplateException {
            return env.getVariable(key);
        }
    }
    
    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }
    
}
