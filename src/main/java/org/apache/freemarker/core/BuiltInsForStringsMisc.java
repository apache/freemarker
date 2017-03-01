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

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateMethodModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateTransformModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl._StaticObjectWrappers;
import org.apache.freemarker.core.model.impl.BeanModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;

class BuiltInsForStringsMisc {

    // Can't be instantiated
    private BuiltInsForStringsMisc() { }
    
    static class booleanBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException {
            final boolean b;
            if (s.equals("true")) {
                b = true;
            } else if (s.equals("false")) {
                b = false;
            } else if (s.equals(env.getTrueStringValue())) {
                b = true;
            } else if (s.equals(env.getFalseStringValue())) {
                b = false;
            } else {
                throw new _MiscTemplateException(this, env,
                        "Can't convert this string to boolean: ", new _DelayedJQuote(s));
            }
            return b ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
    }

    static class evalBI extends OutputFormatBoundBuiltIn {
        
        @Override
        protected TemplateModel calculateResult(Environment env) throws TemplateException {
            return calculateResult(BuiltInForString.getTargetString(target, env), env);
        }
        
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            Template parentTemplate = getTemplate();
            
            ASTExpression exp = null;
            try {
                try {
                    ParserConfiguration pCfg = parentTemplate.getParserConfiguration();
                    
                    SimpleCharStream simpleCharStream = new SimpleCharStream(
                            new StringReader("(" + s + ")"),
                            RUNTIME_EVAL_LINE_DISPLACEMENT, 1,
                            s.length() + 2);
                    simpleCharStream.setTabSize(pCfg.getTabSize());
                    FMParserTokenManager tkMan = new FMParserTokenManager(
                            simpleCharStream);
                    tkMan.SwitchTo(FMParserConstants.FM_EXPRESSION);

                    // pCfg.outputFormat is exceptional: it's inherited from the lexical context
                    if (pCfg.getOutputFormat() != outputFormat) {
                        pCfg = new _ParserConfigurationWithInheritedFormat(
                                pCfg, outputFormat, Integer.valueOf(autoEscapingPolicy));
                    }
                    
                    FMParser parser = new FMParser(
                            parentTemplate, false, tkMan, pCfg, null);
                    
                    exp = parser.ASTExpression();
                } catch (TokenMgrError e) {
                    throw e.toParseException(parentTemplate);
                }
            } catch (ParseException e) {
                throw new _MiscTemplateException(this, env,
                        "Failed to \"?", key, "\" string with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessage(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failing expression:");
            }
            try {
                return exp.eval(env);
            } catch (TemplateException e) {
                throw new _MiscTemplateException(this, env,
                        "Failed to \"?", key, "\" string with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessageWithoutStackTop(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failing expression:");
            }
        }
        
    }
    
    /**
     * A method that takes a parameter and evaluates it as a scalar,
     * then treats that scalar as template source code and returns a
     * transform model that evaluates the template in place.
     * The template inherits the configuration and environment of the executing
     * template. By default, its name will be equal to 
     * <tt>executingTemplate.getName() + "$anonymous_interpreted"</tt>. You can
     * specify another parameter to the method call in which case the
     * template name suffix is the specified id instead of "anonymous_interpreted".
     */
    static class interpretBI extends OutputFormatBoundBuiltIn {
        
        /**
         * Constructs a template on-the-fly and returns it embedded in a
         * {@link TemplateTransformModel}.
         * 
         * <p>The built-in has two arguments:
         * the arguments passed to the method. It can receive at
         * least one and at most two arguments, both must evaluate to a scalar. 
         * The first scalar is interpreted as a template source code and a template
         * is built from it. The second (optional) is used to give the generated
         * template a name.
         * 
         * @return a {@link TemplateTransformModel} that when executed inside
         * a <tt>&lt;transform></tt> block will process the generated template
         * just as if it had been <tt>&lt;transform></tt>-ed at that point.
         */
        @Override
        protected TemplateModel calculateResult(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            ASTExpression sourceExpr = null;
            String id = "anonymous_interpreted";
            if (model instanceof TemplateSequenceModel) {
                sourceExpr = ((ASTExpression) new ASTExpDynamicKeyName(target, new ASTExpNumberLiteral(Integer.valueOf(0))).copyLocationFrom(target));
                if (((TemplateSequenceModel) model).size() > 1) {
                    id = ((ASTExpression) new ASTExpDynamicKeyName(target, new ASTExpNumberLiteral(Integer.valueOf(1))).copyLocationFrom(target)).evalAndCoerceToPlainText(env);
                }
            } else if (model instanceof TemplateScalarModel) {
                sourceExpr = target;
            } else {
                throw new UnexpectedTypeException(
                        target, model,
                        "sequence or string", new Class[] { TemplateSequenceModel.class, TemplateScalarModel.class },
                        env);
            }
            String templateSource = sourceExpr.evalAndCoerceToPlainText(env);
            Template parentTemplate = env.getCurrentTemplate();
            
            final Template interpretedTemplate;
            try {
                ParserConfiguration pCfg = parentTemplate.getParserConfiguration();
                // pCfg.outputFormat is exceptional: it's inherited from the lexical context
                if (pCfg.getOutputFormat() != outputFormat) {
                    pCfg = new _ParserConfigurationWithInheritedFormat(
                            pCfg, outputFormat, Integer.valueOf(autoEscapingPolicy));
                }
                interpretedTemplate = new Template(
                        (parentTemplate.getName() != null ? parentTemplate.getName() : "nameless_template") + "->" + id,
                        null,
                        new StringReader(templateSource),
                        parentTemplate.getConfiguration(), pCfg,
                        null);
            } catch (IOException e) {
                throw new _MiscTemplateException(this, e, env,
                        "Template parsing with \"?", key, "\" has failed with this error:\n\n",
                        MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                        new _DelayedGetMessage(e),
                        MessageUtil.EMBEDDED_MESSAGE_END,
                        "\n\nThe failed expression:");
            }
            
            interpretedTemplate.setLocale(env.getLocale());
            return new TemplateProcessorModel(interpretedTemplate);
        }

        private class TemplateProcessorModel
        implements
            TemplateTransformModel {
            private final Template template;
            
            TemplateProcessorModel(Template template) {
                this.template = template;
            }
            
            @Override
            public Writer getWriter(final Writer out, Map args) throws TemplateModelException, IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    boolean lastFIRE = env.setFastInvalidReferenceExceptions(false);
                    try {
                        env.include(template);
                    } finally {
                        env.setFastInvalidReferenceExceptions(lastFIRE);
                    }
                } catch (Exception e) {
                    throw new _TemplateModelException(e,
                            "Template created with \"?", key, "\" has stopped with this error:\n\n",
                            MessageUtil.EMBEDDED_MESSAGE_BEGIN,
                            new _DelayedGetMessage(e),
                            MessageUtil.EMBEDDED_MESSAGE_END);
                }
        
                return new Writer(out)
                {
                    @Override
                    public void close() {
                    }
                    
                    @Override
                    public void flush() throws IOException {
                        out.flush();
                    }
                    
                    @Override
                    public void write(char[] cbuf, int off, int len) throws IOException {
                        out.write(cbuf, off, len);
                    }
                };
            }
        }

    }

    static class numberBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env)  throws TemplateException {
            try {
                return new SimpleNumber(env.getArithmeticEngine().toNumber(s));
            } catch (NumberFormatException nfe) {
                throw NonNumericalException.newMalformedNumberException(this, s, env);
            }
        }
    }

    /**
     * A built-in that allows us to instantiate an instance of a java class.
     * Usage is something like: <tt>&lt;#assign foobar = "foo.bar.MyClass"?new()></tt>;
     */
    static class newBI extends ASTExpBuiltIn {
        
        @Override
        TemplateModel _eval(Environment env)
                throws TemplateException {
            return new ConstructorFunction(target.evalAndCoerceToPlainText(env), env, target.getTemplate());
        }

        class ConstructorFunction implements TemplateMethodModelEx {

            private final Class<?> cl;
            private final Environment env;
            
            public ConstructorFunction(String classname, Environment env, Template template) throws TemplateException {
                this.env = env;
                cl = env.getNewBuiltinClassResolver().resolve(classname, env, template);
                if (!TemplateModel.class.isAssignableFrom(cl)) {
                    throw new _MiscTemplateException(newBI.this, env,
                            "Class ", cl.getName(), " does not implement org.apache.freemarker.core.TemplateModel");
                }
                if (BeanModel.class.isAssignableFrom(cl)) {
                    throw new _MiscTemplateException(newBI.this, env,
                            "Bean Models cannot be instantiated using the ?", key, " built-in");
                }
            }

            @Override
            public Object exec(List arguments) throws TemplateModelException {
                ObjectWrapper ow = env.getObjectWrapper();
                DefaultObjectWrapper dow =
                    ow instanceof DefaultObjectWrapper
                    ? (DefaultObjectWrapper) ow
                    : _StaticObjectWrappers.DEFAULT_OBJECT_WRAPPER;
                return dow.newInstance(cl, arguments);
            }
        }
    }
    
}
