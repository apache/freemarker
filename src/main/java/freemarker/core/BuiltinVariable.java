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

import java.util.Date;

import freemarker.template.Configuration;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

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
    static final String CURRENT_NODE = "current_node";
    static final String NODE = "node";
    static final String PASS = "pass";
    static final String VARS = "vars";
    static final String VERSION = "version";
    static final String ERROR = "error";
    static final String OUTPUT_ENCODING = "output_encoding";
    static final String URL_ESCAPING_CHARSET = "url_escaping_charset";
    static final String NOW = "now";

    private final String name;

    BuiltinVariable(String name) throws ParseException {
        name = name.intern();
        this.name = name;
        if (name != TEMPLATE_NAME 
            && name != NAMESPACE
            && name != MAIN
            && name != GLOBALS
            && name != LOCALS
            && name != LANG
            && name != LOCALE
            && name != DATA_MODEL
            && name != CURRENT_NODE
            && name != NODE
            && name != PASS
            && name != VARS
            && name != VERSION
            && name != OUTPUT_ENCODING
            && name != URL_ESCAPING_CHARSET
            && name != ERROR
            && name != NOW)
        {
            throw new ParseException("Unknown built-in variable: " + name, this);
        }
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
