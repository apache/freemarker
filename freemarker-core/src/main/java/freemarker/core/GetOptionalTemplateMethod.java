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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import freemarker.template.MalformedTemplateNameException;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template._ObjectWrappers;
import freemarker.template.utility.TemplateModelUtils;

/**
 * Implements {@code .get_optional_template(name, options)}.
 */
class GetOptionalTemplateMethod implements TemplateMethodModelEx {

    static final GetOptionalTemplateMethod INSTANCE = new GetOptionalTemplateMethod(
            BuiltinVariable.GET_OPTIONAL_TEMPLATE);
    static final GetOptionalTemplateMethod INSTANCE_CC = new GetOptionalTemplateMethod(
            BuiltinVariable.GET_OPTIONAL_TEMPLATE_CC);
    
    private static final String OPTION_ENCODING = "encoding";
    private static final String OPTION_PARSE = "parse";

    private static final String RESULT_INCLUDE = "include";
    private static final String RESULT_IMPORT = "import";
    private static final String RESULT_EXISTS = "exists";
   
    /** Used in error messages */
    private final String methodName;

    private GetOptionalTemplateMethod(String builtInVarName) {
        this.methodName = "." + builtInVarName;
    }

    @Override
    public Object exec(List args) throws TemplateModelException {
        final int argCnt = args.size();
        if (argCnt < 1 || argCnt > 2) {
            throw _MessageUtil.newArgCntError(methodName, argCnt, 1, 2);
        }

        final Environment env = Environment.getCurrentEnvironment();
        if (env == null) {
            throw new IllegalStateException("No freemarer.core.Environment is associated to the current thread.");
        }
        
        final String absTemplateName;
        {
            TemplateModel arg = (TemplateModel) args.get(0);
            if (!(arg instanceof TemplateScalarModel)) {
                throw _MessageUtil.newMethodArgMustBeStringException(methodName, 0, arg);
            }
            String templateName  = EvalUtil.modelToString((TemplateScalarModel) arg, null, env);
            
            try {
                absTemplateName = env.toFullTemplateName(env.getCurrentTemplate().getName(), templateName);
            } catch (MalformedTemplateNameException e) {
                throw new _TemplateModelException(
                        e, "Failed to convert template path to full path; see cause exception.");
            }
        }
        
        final TemplateHashModelEx options;
        if (argCnt > 1) {
            TemplateModel arg = (TemplateModel) args.get(1);
            if (!(arg instanceof TemplateHashModelEx)) {
                throw _MessageUtil.newMethodArgMustBeExtendedHashException(methodName, 1, arg);
            }
            options = (TemplateHashModelEx) arg;
        } else {
            options = null;
        }
        
        String encoding = null;
        boolean parse = true;
        if (options != null) {
            final KeyValuePairIterator kvpi = TemplateModelUtils.getKeyValuePairIterator(options);
            while (kvpi.hasNext()) {
                final KeyValuePair kvp = kvpi.next();
                
                final String optName;
                {
                    TemplateModel optNameTM = kvp.getKey();
                    if (!(optNameTM instanceof TemplateScalarModel)) {
                        throw _MessageUtil.newMethodArgInvalidValueException(methodName, 1,
                                "All keys in the options hash must be strings, but found ",
                                new _DelayedAOrAn(new _DelayedFTLTypeDescription(optNameTM)));
                    }
                    optName = ((TemplateScalarModel) optNameTM).getAsString();
                }
                
                final TemplateModel optValue = kvp.getValue();
                
                if (OPTION_ENCODING.equals(optName)) {
                    encoding = getStringOption(OPTION_ENCODING, optValue); 
                } else if (OPTION_PARSE.equals(optName)) {
                    parse = getBooleanOption(OPTION_PARSE, optValue); 
                } else {
                    throw _MessageUtil.newMethodArgInvalidValueException(methodName, 1,
                            "Unsupported option ", new _DelayedJQuote(optName), "; valid names are: ",
                            new _DelayedJQuote(OPTION_ENCODING), ", ", new _DelayedJQuote(OPTION_PARSE), ".");
                }
            }
        }

        final Template template;
        try {
            template = env.getTemplateForInclusion(absTemplateName, encoding, parse, true);
        } catch (IOException e) {
            throw new _TemplateModelException(
                    e, "I/O error when trying to load optional template ", new _DelayedJQuote(absTemplateName),
                        "; see cause exception");
        }
        
        SimpleHash result = new SimpleHash(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
        result.put(RESULT_EXISTS, template != null);
        // If the template is missing, result.include and such will be missing too, so that a default can be
        // conveniently provided like in <@optTemp.include!myDefaultMacro />.
        if (template != null) {
            result.put(RESULT_INCLUDE, new TemplateDirectiveModel() {
                @Override
                public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                        throws TemplateException, IOException {
                    if (!params.isEmpty()) {
                        throw new TemplateException("This directive supports no parameters.", env);
                    }
                    if (loopVars.length != 0) {
                        throw new TemplateException("This directive supports no loop variables.", env);
                    }
                    if (body != null) {
                        throw new TemplateException("This directive supports no nested content.", env);
                    }
                    
                    env.include(template);
                }
            });
            result.put(RESULT_IMPORT, new TemplateMethodModelEx() {
                @Override
                public Object exec(List args) throws TemplateModelException {
                    if (!args.isEmpty()) {
                        throw new TemplateModelException("This method supports no parameters.");
                    }
                    
                    try {
                        return env.importLib(template, null);
                    } catch (IOException | TemplateException e) {
                        throw new _TemplateModelException(e, "Failed to import loaded template; see cause exception");
                    }
                }
            });
        }
        return result;
    }

    private boolean getBooleanOption(String optionName, TemplateModel value) throws TemplateModelException {
        if (!(value instanceof TemplateBooleanModel)) {
            throw _MessageUtil.newMethodArgInvalidValueException(methodName, 1,
                    "The value of the ", new _DelayedJQuote(optionName), " option must be a boolean, but it was ",
                    new _DelayedAOrAn(new _DelayedFTLTypeDescription(value)), ".");
        }
        return ((TemplateBooleanModel) value).getAsBoolean();
    }

    private String getStringOption(String optionName, TemplateModel value) throws TemplateModelException {
        if (!(value instanceof TemplateScalarModel)) {
            throw _MessageUtil.newMethodArgInvalidValueException(methodName, 1,
                    "The value of the ", new _DelayedJQuote(optionName), " option must be a string, but it was ",
                    new _DelayedAOrAn(new _DelayedFTLTypeDescription(value)), ".");
        }
        return EvalUtil.modelToString((TemplateScalarModel) value, null, null);
    }

}