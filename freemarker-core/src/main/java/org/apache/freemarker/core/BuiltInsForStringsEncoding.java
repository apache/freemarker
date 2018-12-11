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

import static org.apache.freemarker.core.util.CallableUtils.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util._StringUtils;

class BuiltInsForStringsEncoding {

    static class htmlBI extends BuiltInForLegacyEscaping {
        
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.XHTMLEnc(s));
        }
        
    }

    static class j_stringBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.javaStringEnc(s));
        }
    }

    static class js_stringBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.javaScriptStringEnc(s));
        }
    }

    static class json_stringBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.jsonStringEnc(s));
        }
    }

    static class rtfBI extends BuiltInForLegacyEscaping {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.RTFEnc(s));
        }
    }

    static class urlBI extends BuiltInForString {
        
        static class UrlBIResult extends BuiltInsForStringsEncoding.AbstractUrlBIResult {
    
            protected UrlBIResult(ASTExpBuiltIn parent, String target, Environment env) {
                super(parent, target, env);
            }
    
            @Override
            protected String encodeWithCharset(Charset charset) throws UnsupportedEncodingException {
                return _StringUtils.URLEnc(targetAsString, charset);
            }

        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new UrlBIResult(this, s, env);
        }
        
    }

    static class urlPathBI extends BuiltInForString {
    
        static class UrlPathBIResult extends BuiltInsForStringsEncoding.AbstractUrlBIResult {
    
            protected UrlPathBIResult(ASTExpBuiltIn parent, String target, Environment env) {
                super(parent, target, env);
            }
    
            @Override
            protected String encodeWithCharset(Charset charset) throws UnsupportedEncodingException {
                return _StringUtils.URLPathEnc(targetAsString, charset);
            }
            
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new UrlPathBIResult(this, s, env);
        }
        
    }

    static class xhtmlBI extends BuiltInForLegacyEscaping {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.XHTMLEnc(s));
        }
    }

    static class xmlBI extends BuiltInForLegacyEscaping {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.XMLEnc(s));
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsEncoding() { }

    static abstract class AbstractUrlBIResult implements TemplateStringModel, TemplateFunctionModel,
            ASTExpBuiltIn.BuiltInCallable {
        
        protected final ASTExpBuiltIn parent;
        protected final String targetAsString;
        private final Environment env;
        private String cachedResult;
        
        protected AbstractUrlBIResult(ASTExpBuiltIn parent, String targetAsString, Environment env) {
            this.parent = parent;
            this.targetAsString = targetAsString;
            this.env = env;
        }
        
        protected abstract String encodeWithCharset(Charset charset) throws UnsupportedEncodingException;

        @Override
        public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                throws TemplateException {
            try {
                String charsetName = getStringArgument(args,0, this);
                Charset charset;
                try {
                    charset = Charset.forName(charsetName);
                } catch (UnsupportedCharsetException e) {
                    throw new TemplateException(e, "Wrong charset name, or charset is unsupported by the runtime "
                            + "environment: " + _StringUtils.jQuote(charsetName));
                }
                return new SimpleString(encodeWithCharset(charset));
            } catch (Exception e) {
                throw new TemplateException(e, "Failed to execute URL encoding.");
            }
        }

        @Override
        public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
            return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
        }

        @Override
        public String getAsString() throws TemplateException {
            if (cachedResult == null) {
                Charset charset = env.getEffectiveURLEscapingCharset();
                if (charset == null) {
                    throw new TemplateException(
                            "To do URL encoding, the framework that encloses FreeMarker must specify the \"",
                            Configuration.Builder.OUTPUT_ENCODING_KEY, "\" setting or the \"",
                            Configuration.Builder.URL_ESCAPING_CHARSET_KEY,
                            "\" setting, so ask the programmers to set them. Or, as a last chance, you can set the "
                            + "url_encoding_charset setting in the template, e.g. <#setting ",
                            Configuration.Builder.URL_ESCAPING_CHARSET_KEY,
                            "='ISO-8859-1'>, or give the charset explicitly to the built-in, e.g. "
                            + "foo?url('ISO-8859-1').");
                }
                try {
                    cachedResult = encodeWithCharset(charset);
                } catch (UnsupportedEncodingException e) {
                    throw new TemplateException(e, "Failed to execute URL encoding.");
                }
            }
            return cachedResult;
        }

        @Override
        public String getBuiltInName() {
            return parent.key;
        }

        @Override
        public String getOriginName() {
            return ASTExpBuiltIn.getOriginName(this);
        }
    }

}
