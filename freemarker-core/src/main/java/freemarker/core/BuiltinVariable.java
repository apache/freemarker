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
import java.util.Date;

import freemarker.core.Macro.Context;
import freemarker.template.Configuration;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template._VersionInts;
import freemarker.template.utility.StringUtil;

/**
 * A reference to a built-in identifier, such as .root
 */
final class BuiltinVariable extends Expression {

    static final String TEMPLATE_NAME_CC = "templateName";
    static final String TEMPLATE_NAME = "template_name";
    static final String MAIN_TEMPLATE_NAME_CC = "mainTemplateName";
    static final String MAIN_TEMPLATE_NAME = "main_template_name";
    static final String CURRENT_TEMPLATE_NAME_CC = "currentTemplateName";
    static final String CURRENT_TEMPLATE_NAME = "current_template_name";
    static final String NAMESPACE = "namespace";
    static final String MAIN = "main";
    static final String GLOBALS = "globals";
    static final String LOCALS = "locals";
    static final String DATA_MODEL_CC = "dataModel";
    static final String DATA_MODEL = "data_model";
    static final String LANG = "lang";
    static final String LOCALE = Configurable.LOCALE_KEY;
    static final String LOCALE_OBJECT_CC = Configurable.LOCALE_KEY_CAMEL_CASE + "Object";
    static final String LOCALE_OBJECT = Configurable.LOCALE_KEY + "_object";
    static final String TIME_ZONE_CC = Configurable.TIME_ZONE_KEY_CAMEL_CASE;
    static final String TIME_ZONE = Configurable.TIME_ZONE_KEY;
    static final String CURRENT_NODE_CC = "currentNode";
    static final String CURRENT_NODE = "current_node";
    static final String NODE = "node";
    static final String PASS = "pass";
    static final String VARS = "vars";
    static final String VERSION = "version";
    static final String INCOMPATIBLE_IMPROVEMENTS_CC = Configuration.INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE;
    static final String INCOMPATIBLE_IMPROVEMENTS = Configuration.INCOMPATIBLE_IMPROVEMENTS_KEY;
    static final String ERROR = "error";
    static final String OUTPUT_ENCODING_CC = Configurable.OUTPUT_ENCODING_KEY_CAMEL_CASE;
    static final String OUTPUT_ENCODING = Configurable.OUTPUT_ENCODING_KEY;
    static final String OUTPUT_FORMAT_CC = Configuration.OUTPUT_FORMAT_KEY_CAMEL_CASE;
    static final String OUTPUT_FORMAT = Configuration.OUTPUT_FORMAT_KEY;
    static final String AUTO_ESC_CC = "autoEsc";
    static final String AUTO_ESC = "auto_esc";
    static final String URL_ESCAPING_CHARSET_CC = Configurable.URL_ESCAPING_CHARSET_KEY_CAMEL_CASE;
    static final String URL_ESCAPING_CHARSET = Configurable.URL_ESCAPING_CHARSET_KEY;
    static final String NOW = "now";
    static final String GET_OPTIONAL_TEMPLATE = "get_optional_template";
    static final String GET_OPTIONAL_TEMPLATE_CC = "getOptionalTemplate";
    static final String CALLER_TEMPLATE_NAME = "caller_template_name";
    static final String CALLER_TEMPLATE_NAME_CC = "callerTemplateName";
    static final String ARGS = "args";
    static final String[] SPEC_VAR_NAMES = new String[] {
        // IMPORTANT! Keep this sorted alphabetically!
        ARGS,
        AUTO_ESC_CC,
        AUTO_ESC,
        CALLER_TEMPLATE_NAME_CC,
        CALLER_TEMPLATE_NAME,
        CURRENT_NODE_CC,
        CURRENT_TEMPLATE_NAME_CC,
        CURRENT_NODE,
        CURRENT_TEMPLATE_NAME,
        DATA_MODEL_CC,
        DATA_MODEL,
        ERROR,
        GET_OPTIONAL_TEMPLATE_CC,
        GET_OPTIONAL_TEMPLATE,
        GLOBALS,
        INCOMPATIBLE_IMPROVEMENTS_CC,
        INCOMPATIBLE_IMPROVEMENTS,
        LANG,
        LOCALE,
        LOCALE_OBJECT_CC,
        LOCALE_OBJECT,
        LOCALS,
        MAIN,
        MAIN_TEMPLATE_NAME_CC,
        MAIN_TEMPLATE_NAME,
        NAMESPACE,
        NODE,
        NOW,
        OUTPUT_ENCODING_CC,
        OUTPUT_FORMAT_CC,
        OUTPUT_ENCODING,
        OUTPUT_FORMAT,
        PASS,
        TEMPLATE_NAME_CC,
        TEMPLATE_NAME,
        TIME_ZONE_CC,
        TIME_ZONE,
        URL_ESCAPING_CHARSET_CC,
        URL_ESCAPING_CHARSET,
        VARS,
        VERSION
    };

    private final String name;
    private final TemplateModel parseTimeValue;

    BuiltinVariable(Token nameTk, FMParserTokenManager tokenManager, TemplateModel parseTimeValue)
            throws ParseException {
        String name = nameTk.image;
        this.parseTimeValue = parseTimeValue;
        if (Arrays.binarySearch(SPEC_VAR_NAMES, name) < 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown special variable name: ");
            sb.append(StringUtil.jQuote(name)).append(".");
            
            int shownNamingConvention;
            {
                int namingConvention = tokenManager.namingConvention;
                shownNamingConvention = namingConvention != Configuration.AUTO_DETECT_NAMING_CONVENTION
                        ? namingConvention : Configuration.LEGACY_NAMING_CONVENTION /* [2.4] CAMEL_CASE */; 
            }
            
            {
                String correctName;
                if (name.equals("auto_escape") || name.equals("auto_escaping") || name.equals("autoesc")) {
                    correctName = "auto_esc";
                } else if (name.equals("autoEscape") || name.equals("autoEscaping")) {
                    correctName = "autoEsc";
                } else {
                    correctName = null;
                }
                if (correctName != null) {
                    sb.append(" You may meant: ");
                    sb.append(StringUtil.jQuote(correctName)).append(".");
                }
            }
            
            sb.append("\nThe allowed special variable names are: ");
            boolean first = true;
            for (int i = 0; i < SPEC_VAR_NAMES.length; i++) {
                final String correctName = SPEC_VAR_NAMES[i];
                int correctNameNamingConvetion = _CoreStringUtils.getIdentifierNamingConvention(correctName);
                if (shownNamingConvention == Configuration.CAMEL_CASE_NAMING_CONVENTION 
                        ? correctNameNamingConvetion != Configuration.LEGACY_NAMING_CONVENTION
                        : correctNameNamingConvetion != Configuration.CAMEL_CASE_NAMING_CONVENTION) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(correctName);
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
            return env.getGlobalVariables();
        }
        if (name == LOCALS) {
            Macro.Context ctx = env.getCurrentMacroContext();
            return ctx == null ? null : ctx.getLocals();
        }
        if (name == DATA_MODEL || name == DATA_MODEL_CC) {
            return env.getDataModel();
        }
        if (name == VARS) {
            return new VarsHash(env);
        }
        if (name == LOCALE) {
            return new SimpleScalar(env.getLocale().toString());
        }
        if (name == LOCALE_OBJECT || name == LOCALE_OBJECT_CC) {
            return env.getObjectWrapper().wrap(env.getLocale());
        }
        if (name == LANG) {
            return new SimpleScalar(env.getLocale().getLanguage());
        }
        if (name == CURRENT_NODE || name == NODE || name == CURRENT_NODE_CC) {
            return env.getCurrentVisitorNode();
        }
        if (name == TEMPLATE_NAME || name == TEMPLATE_NAME_CC) {
            // The behavior of env.getTemplate() was changed with IcI 2.3.22, but there was an unintended side effect
            // of changing the behavior of .template_name, which was fixed with IcI 2.3.23. IcI 2.3.22 deliberately
            // remains broken.
            return (env.getConfiguration().getIncompatibleImprovements().intValue() >= _VersionInts.V_2_3_23)
                    ? new SimpleScalar(env.getTemplate230().getName())
                    : new SimpleScalar(env.getTemplate().getName());
        }
        if (name == MAIN_TEMPLATE_NAME || name == MAIN_TEMPLATE_NAME_CC) {
            return SimpleScalar.newInstanceOrNull(env.getMainTemplate().getName());
        }
        if (name == CURRENT_TEMPLATE_NAME || name == CURRENT_TEMPLATE_NAME_CC) {
            return SimpleScalar.newInstanceOrNull(env.getCurrentTemplate().getName());
        }
        if (name == PASS) {
            return Macro.DO_NOTHING_MACRO;
        }
        if (name == OUTPUT_ENCODING || name == OUTPUT_ENCODING_CC) {
            String s = env.getOutputEncoding();
            return SimpleScalar.newInstanceOrNull(s);
        }
        if (name == URL_ESCAPING_CHARSET || name == URL_ESCAPING_CHARSET_CC) {
            String s = env.getURLEscapingCharset();
            return SimpleScalar.newInstanceOrNull(s);
        }
        if (name == ERROR) {
            return new SimpleScalar(env.getCurrentRecoveredErrorMessage());
        }
        if (name == NOW) {
            return new SimpleDate(new Date(), TemplateDateModel.DATETIME);
        }
        if (name == VERSION) {
            return new SimpleScalar(Configuration.getVersionNumber());
        }
        if (name == INCOMPATIBLE_IMPROVEMENTS || name == INCOMPATIBLE_IMPROVEMENTS_CC) {
            return new SimpleScalar(env.getConfiguration().getIncompatibleImprovements().toString());
        }
        if (name == GET_OPTIONAL_TEMPLATE) {
            return GetOptionalTemplateMethod.INSTANCE;
        }
        if (name == GET_OPTIONAL_TEMPLATE_CC) {
            return GetOptionalTemplateMethod.INSTANCE_CC;
        }
        if (name == CALLER_TEMPLATE_NAME || name == CALLER_TEMPLATE_NAME_CC) {
            TemplateObject callPlace = getRequiredMacroContext(env).callPlace;
            String name = callPlace != null ? callPlace.getTemplate().getName() : null;
            return name != null ? new SimpleScalar(name) : TemplateScalarModel.EMPTY_STRING;
        }
        if (name == ARGS) {
            TemplateModel args = getRequiredMacroContext(env).getArgsSpecialVariableValue();
            if (args == null) {
                // Should be impossible, as the parser checks this condition already.
                throw new _MiscTemplateException(this, "The \"", ARGS, "\" special variable wasn't initialized.", name);
            }
            return args;
        }
        if (name == TIME_ZONE || name == TIME_ZONE_CC) {
            return new SimpleScalar(env.getTimeZone().getID());
        }

        throw new _MiscTemplateException(this,
                "Invalid special variable: ", name);
    }

    private Context getRequiredMacroContext(Environment env) throws TemplateException {
        Context ctx = env.getCurrentMacroContext();
        if (ctx == null) {
            throw new TemplateException(
                    "Can't get ." + name + " here, as there's no macro or function (that's "
                    + "implemented in the template) call in context.", env);
        }
        return ctx;
    }

    @Override
    public String toString() {
        return "." + name;
    }

    @Override
    public String getCanonicalForm() {
        return "." + name;
    }
    
    @Override
    String getNodeTypeSymbol() {
        return getCanonicalForm();
    }

    @Override
    boolean isLiteral() {
        return false;
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return this;
    }

    static class VarsHash implements TemplateHashModel {
        
        Environment env;
        
        VarsHash(Environment env) {
            this.env = env;
        }
        
        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            return env.getVariable(key);
        }
        
        @Override
        public boolean isEmpty() {
            return false;
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
