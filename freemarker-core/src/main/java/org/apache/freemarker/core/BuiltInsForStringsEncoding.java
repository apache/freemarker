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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import org.apache.freemarker.core.model.TemplateMethodModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.util._StringUtil;

class BuiltInsForStringsEncoding {

    static class htmlBI extends BuiltInForLegacyEscaping {
        
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(_StringUtil.XHTMLEnc(s));
        }
        
    }

    static class j_stringBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(_StringUtil.javaStringEnc(s));
        }
    }

    static class js_stringBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(_StringUtil.javaScriptStringEnc(s));
        }
    }

    static class json_stringBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(_StringUtil.jsonStringEnc(s));
        }
    }

    static class rtfBI extends BuiltInForLegacyEscaping {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(_StringUtil.RTFEnc(s));
        }
    }

    static class urlBI extends BuiltInForString {
        
        static class UrlBIResult extends BuiltInsForStringsEncoding.AbstractUrlBIResult {
    
            protected UrlBIResult(ASTExpBuiltIn parent, String target, Environment env) {
                super(parent, target, env);
            }
    
            @Override
            protected String encodeWithCharset(Charset charset) throws UnsupportedEncodingException {
                return _StringUtil.URLEnc(targetAsString, charset);
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
                return _StringUtil.URLPathEnc(targetAsString, charset);
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
            return new SimpleScalar(_StringUtil.XHTMLEnc(s));
        }
    }

    static class xmlBI extends BuiltInForLegacyEscaping {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(_StringUtil.XMLEnc(s));
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsEncoding() { }

    static abstract class AbstractUrlBIResult implements
    TemplateScalarModel, TemplateMethodModel {
        
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
        public Object exec(List args) throws TemplateModelException {
            parent.checkMethodArgCount(args.size(), 1);
            try {
                String charsetName = (String) args.get(0);
                Charset charset = null;
                try {
                    charset = Charset.forName(charsetName);
                } catch (UnsupportedCharsetException e) {
                    throw new _TemplateModelException(e, "Wrong charset name, or charset is unsupported by the runtime "
                            + "environment: " + _StringUtil.jQuote(charsetName));
                }
                return new SimpleScalar(encodeWithCharset(charset));
            } catch (Exception e) {
                throw new _TemplateModelException(e, "Failed to execute URL encoding.");
            }
        }
        
        @Override
        public String getAsString() throws TemplateModelException {
            if (cachedResult == null) {
                Charset charset = env.getEffectiveURLEscapingCharset();
                if (charset == null) {
                    throw new _TemplateModelException(
                            "To do URL encoding, the framework that encloses "
                            + "FreeMarker must specify the output encoding "
                            + "or the URL encoding charset, so ask the "
                            + "programmers to fix it. Or, as a last chance, "
                            + "you can set the url_encoding_charset setting in "
                            + "the template, e.g. "
                            + "<#setting url_escaping_charset='ISO-8859-1'>, or "
                            + "give the charset explicitly to the buit-in, e.g. "
                            + "foo?url('ISO-8859-1').");
                }
                try {
                    cachedResult = encodeWithCharset(charset);
                } catch (UnsupportedEncodingException e) {
                    throw new _TemplateModelException(e, "Failed to execute URL encoding.");
                }
            }
            return cachedResult;
        }
        
    }

}
