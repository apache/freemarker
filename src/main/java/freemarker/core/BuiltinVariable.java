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
import java.util.Date;

import freemarker.template.Configuration;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * A reference to a built-in identifier, such as .root
 */
final class BuiltinVariable extends Expression {

    static final String TEMPLATE_NAME = "template_name";
    static final String NAMESPACE = "namespace";
    static final String MAIN = "main";
    static final String GLOBALS = "globals";
    static final String LOCALS = "locals";
    static final String DATA_MODEL = "data_model";
    static final String LANG = "lang";
    static final String LOCALE = "locale";
    static final String LOCALE_OBJECT = "locale_object";
    static final String CURRENT_NODE = "current_node";
    static final String NODE = "node";
    static final String PASS = "pass";
    static final String VARS = "vars";
    static final String VERSION = "version";
    static final String ERROR = "error";
    static final String OUTPUT_ENCODING = "output_encoding";
    static final String URL_ESCAPING_CHARSET = "url_escaping_charset";
    static final String NOW = "now";
    
    static final String[] SPEC_VAR_NAMES = new String[] {
        CURRENT_NODE,
        DATA_MODEL,
        ERROR,
        GLOBALS,
        LANG,
        LOCALE,
        LOCALE_OBJECT,
        LOCALS,
        MAIN,
        NAMESPACE,
        NODE,
        NOW,
        OUTPUT_ENCODING,
        PASS,
        TEMPLATE_NAME,
        URL_ESCAPING_CHARSET,
        VARS,
        VERSION
    };

    private final String name;

    BuiltinVariable(String name) throws ParseException {
        if (Arrays.binarySearch(SPEC_VAR_NAMES, name) < 0) {
            StringBuffer sb = new StringBuffer();
            sb.append("Unknown special variable name: ");
            sb.append(StringUtil.jQuote(name)).append(".");
            final String underscoredName = _CoreStringUtils.camelCaseToUnderscored(name);
            if (!underscoredName.equals(name) && Arrays.binarySearch(SPEC_VAR_NAMES, underscoredName) >= 0) {
                sb.append(" Supporting camelCase special variable names is planned for FreeMarker 2.4.0; check if an "
                            + "update is available, and if it indeed supports camel case. "
                            + "Until that, use \"").append(underscoredName).append("\".");
            } else {
                sb.append(" The allowed special variable names are: ");
                for (int i = 0; i < SPEC_VAR_NAMES.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(SPEC_VAR_NAMES[i]);
                }
            }
            throw new ParseException(sb.toString(), this);
        }
        
        this.name = name.intern();
    }

    TemplateModel _eval(Environment env) throws TemplateException {
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
        if (name == DATA_MODEL) {
            return env.getDataModel();
        }
        if (name == VARS) {
            return new VarsHash(env);
        }
        if (name == LOCALE) {
            return new SimpleScalar(env.getLocale().toString());
        }
        if (name == LOCALE_OBJECT) {
            return env.getObjectWrapper().wrap(env.getLocale());
        }
        if (name == LANG) {
            return new SimpleScalar(env.getLocale().getLanguage());
        }
        if (name == CURRENT_NODE || name == NODE) {
            return env.getCurrentVisitorNode();
        }
        if (name == TEMPLATE_NAME) {
            return new SimpleScalar(env.getTemplate().getName());
        }
        if (name == PASS) {
            return Macro.DO_NOTHING_MACRO;
        }
        if (name == VERSION) {
            return new SimpleScalar(Configuration.getVersionNumber());
        }
        if (name == OUTPUT_ENCODING) {
            String s = env.getOutputEncoding();
            return s != null ? new SimpleScalar(s) : null;
        }
        if (name == URL_ESCAPING_CHARSET) {
            String s = env.getURLEscapingCharset();
            return s != null ? new SimpleScalar(s) : null;
        }
        if (name == ERROR) {
            return new SimpleScalar(env.getCurrentRecoveredErrorMessage());
        }
        if (name == NOW) {
            return new SimpleDate(new Date(), TemplateDateModel.DATETIME);
        }
        throw new _MiscTemplateException(this, new Object[] { "Invalid built-in variable: ", name });
    }

    public String toString() {
        return "." + name;
    }

    public String getCanonicalForm() {
        return "." + name;
    }
    
    String getNodeTypeSymbol() {
        return getCanonicalForm();
    }

    boolean isLiteral() {
        return false;
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return this;
    }

    static class VarsHash implements TemplateHashModel {
        
        Environment env;
        
        VarsHash(Environment env) {
            this.env = env;
        }
        
        public TemplateModel get(String key) throws TemplateModelException {
            return env.getVariable(key);
        }
        
        public boolean isEmpty() {
            return false;
        }
    }
    
    int getParameterCount() {
        return 0;
    }

    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }
    
}
