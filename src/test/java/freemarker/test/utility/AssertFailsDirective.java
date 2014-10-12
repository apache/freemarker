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

package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.NullWriter;
import freemarker.template.utility.StringUtil;

public class AssertFailsDirective implements TemplateDirectiveModel {
    
    public static AssertFailsDirective INSTANCE = new AssertFailsDirective();

    private static final String MESSAGE_PARAM = "message";
    private static final String MESSAGE_REGEXP_PARAM = "messageRegexp";
    private static final String EXCEPTION_PARAM = "exception";
    private static final String CAUSE_NESTING_LEVEL_PARAM = "causeNestingLevel";
    
    private AssertFailsDirective() { }

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
                body.render(NullWriter.INSTANCE);
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
                                + "and thus it doesn't contain:\n" + StringUtil.jQuote(message) + ".",
                                env);
                    }
                    if (message != null) {
                        String actualMessage = e instanceof TemplateException
                                ? ((TemplateException) e).getMessageWithoutStackTop() : e.getMessage();
                        if (actualMessage.toLowerCase().indexOf(message.toLowerCase()) == -1) {
                            throw new AssertationFailedInTemplateException(
                                    "Failure is not like expected. The exception message:\n" + StringUtil.jQuote(actualMessage)
                                    + "\ndoesn't contain:\n" + StringUtil.jQuote(message) + ".",
                                    env);
                        }
                    }
                    if (messageRegexp != null) {
                        if (!messageRegexp.matcher(e.getMessage()).find()) {
                            throw new AssertationFailedInTemplateException(
                                    "Failure is not like expected. The exception message:\n" + StringUtil.jQuote(e.getMessage())
                                    + "\ndoesn't match this regexp:\n" + StringUtil.jQuote(messageRegexp.toString())
                                    + ".",
                                    env);
                        }
                    }
                }
                if (exception != null && e.getClass().getName().indexOf(exception) == -1) {
                    throw new AssertationFailedInTemplateException(
                            "Failure is not like expected. The exception class name " + StringUtil.jQuote(e.getClass().getName())
                            + " doesn't contain " + StringUtil.jQuote(message) + ".",
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
