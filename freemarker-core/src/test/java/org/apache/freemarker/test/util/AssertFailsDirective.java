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

package org.apache.freemarker.test.util;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateDirectiveBody;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.util._StringUtil;

public class AssertFailsDirective implements TemplateDirectiveModel {
    
    public static AssertFailsDirective INSTANCE = new AssertFailsDirective();

    private static final String MESSAGE_PARAM = "message";
    private static final String MESSAGE_REGEXP_PARAM = "messageRegexp";
    private static final String EXCEPTION_PARAM = "exception";
    private static final String CAUSE_NESTING_LEVEL_PARAM = "causeNestingLevel";
    
    private AssertFailsDirective() { }

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        String message = null;
        Pattern messageRegexp = null;
        String exception = null;
        int causeNestingLevel = 0;
        for (Object paramEnt  : params.entrySet()) {
            Map.Entry<String, TemplateModel> param = (Map.Entry) paramEnt;
            String paramName = param.getKey();
            if (paramName.equals(MESSAGE_PARAM)) {
                message = getAsString(param.getValue(), MESSAGE_PARAM, env);
            } else if (paramName.equals(MESSAGE_REGEXP_PARAM)) {
                messageRegexp = Pattern.compile(
                        getAsString(param.getValue(), MESSAGE_REGEXP_PARAM, env),
                        Pattern.CASE_INSENSITIVE);
            } else if (paramName.equals(EXCEPTION_PARAM)) {
                exception = getAsString(param.getValue(), EXCEPTION_PARAM, env);
            } else if (paramName.equals(CAUSE_NESTING_LEVEL_PARAM)) {
                causeNestingLevel = getAsInt(param.getValue(), CAUSE_NESTING_LEVEL_PARAM, env);
            } else {
                throw new UnsupportedParameterException(paramName, env);
            }
        }
        
        if (body != null) {
            boolean blockFailed;
            try {
                body.render(_NullWriter.INSTANCE);
                blockFailed = false;
            } catch (Throwable e) {
                blockFailed = true;
                
                int causeNestingLevelCountDown = causeNestingLevel; 
                while (causeNestingLevelCountDown != 0) {
                    e = e.getCause();
                    if (e == null) {
                        throw new AssertationFailedInTemplateException(
                                "Failure is not like expected: The cause exception nesting dept was lower than "
                                + causeNestingLevel + ".",
                                env);
                    }
                    causeNestingLevelCountDown--;
                }
                
                if (message != null || messageRegexp != null) {
                    if (e.getMessage() == null) {
                        throw new AssertationFailedInTemplateException(
                                "Failure is not like expected. The exception message was null, "
                                + "and thus it doesn't contain:\n" + _StringUtil.jQuote(message) + ".",
                                env);
                    }
                    if (message != null) {
                        String actualMessage = e instanceof TemplateException
                                ? ((TemplateException) e).getMessageWithoutStackTop() : e.getMessage();
                        if (actualMessage.toLowerCase().indexOf(message.toLowerCase()) == -1) {
                            throw new AssertationFailedInTemplateException(
                                    "Failure is not like expected. The exception message:\n" + _StringUtil.jQuote(actualMessage)
                                    + "\ndoesn't contain:\n" + _StringUtil.jQuote(message) + ".",
                                    env);
                        }
                    }
                    if (messageRegexp != null) {
                        if (!messageRegexp.matcher(e.getMessage()).find()) {
                            throw new AssertationFailedInTemplateException(
                                    "Failure is not like expected. The exception message:\n" + _StringUtil.jQuote(e.getMessage())
                                    + "\ndoesn't match this regexp:\n" + _StringUtil.jQuote(messageRegexp.toString())
                                    + ".",
                                    env);
                        }
                    }
                }
                if (exception != null && e.getClass().getName().indexOf(exception) == -1) {
                    throw new AssertationFailedInTemplateException(
                            "Failure is not like expected. The exception class name " + _StringUtil.jQuote(e.getClass().getName())
                            + " doesn't contain " + _StringUtil.jQuote(message) + ".",
                            env);
                }
            }
            if (!blockFailed) {
                throw new AssertationFailedInTemplateException(
                        "Block was expected to fail, but it didn't.",
                        env);
            }
        }
    }

    private String getAsString(TemplateModel value, String paramName, Environment env)
            throws BadParameterTypeException, TemplateModelException {
        if (value instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) value).getAsString(); 
        } else {
            throw new BadParameterTypeException(paramName, "string", value, env);
        }
    }

    private int getAsInt(TemplateModel value, String paramName, Environment env) throws BadParameterTypeException, TemplateModelException {
        if (value instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) value).getAsNumber().intValue(); 
        } else {
            throw new BadParameterTypeException(paramName, "number", value, env);
        }
    }
    
}
