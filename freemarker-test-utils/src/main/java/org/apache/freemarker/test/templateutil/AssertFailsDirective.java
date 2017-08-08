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

package org.apache.freemarker.test.templateutil;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.util._StringUtils;

public class AssertFailsDirective implements TemplateDirectiveModel {

    public static AssertFailsDirective INSTANCE = new AssertFailsDirective();

    private static final int MESSAGE_ARG_IDX = 0;
    private static final int MESSAGE_REGEXP_ARG_IDX = 1;
    private static final int EXCEPTION_ARG_IDX = 2;
    private static final int CAUSE_NESTING_LEVEL_ARG_IDX = 3;

    private static final String MESSAGE_ARG_NAME = "message";
    private static final String MESSAGE_REGEXP_ARG_NAME = "messageRegexp";
    private static final String EXCEPTION_ARG_NAME = "exception";
    private static final String CAUSE_NESTING_LEVEL_ARG_NAME = "causeNestingLevel";

    private static final StringToIndexMap ARG_NAME_TO_IDX = StringToIndexMap.of(
            MESSAGE_ARG_NAME, MESSAGE_ARG_IDX,
            MESSAGE_REGEXP_ARG_NAME, MESSAGE_REGEXP_ARG_IDX,
            EXCEPTION_ARG_NAME, EXCEPTION_ARG_IDX,
            CAUSE_NESTING_LEVEL_ARG_NAME, CAUSE_NESTING_LEVEL_ARG_IDX);

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0, false,
            ARG_NAME_TO_IDX, false);

    private AssertFailsDirective() {
    }

    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        String message = getAsString(args[MESSAGE_ARG_IDX], MESSAGE_ARG_NAME, env);
        Pattern messageRegexp = getAsPattern(args[MESSAGE_REGEXP_ARG_IDX], MESSAGE_REGEXP_ARG_NAME, env);
        String exception = getAsString(args[EXCEPTION_ARG_IDX], EXCEPTION_ARG_NAME, env);
        int causeNestingLevel = getAsInt(args[CAUSE_NESTING_LEVEL_ARG_IDX], 0, CAUSE_NESTING_LEVEL_ARG_NAME, env);
        if (callPlace.hasNestedContent()) {
            boolean blockFailed;
            try {
                callPlace.executeNestedContent(null, _NullWriter.INSTANCE, env);
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
                                        + "and thus it doesn't contain:\n" + _StringUtils.jQuote(message) + ".",
                                env);
                    }
                    if (message != null) {
                        String actualMessage = e instanceof TemplateException
                                ? ((TemplateException) e).getMessageWithoutStackTop() : e.getMessage();
                        if (!actualMessage.toLowerCase().contains(message.toLowerCase())) {
                            throw new AssertationFailedInTemplateException(
                                    "Failure is not like expected. The exception message:\n" + _StringUtils
                                            .jQuote(actualMessage)
                                            + "\ndoesn't contain:\n" + _StringUtils.jQuote(message) + ".",
                                    env);
                        }
                    }
                    if (messageRegexp != null) {
                        if (!messageRegexp.matcher(e.getMessage()).find()) {
                            throw new AssertationFailedInTemplateException(
                                    "Failure is not like expected. The exception message:\n" + _StringUtils
                                            .jQuote(e.getMessage())
                                            + "\ndoesn't match this regexp:\n" + _StringUtils
                                            .jQuote(messageRegexp.toString())
                                            + ".",
                                    env);
                        }
                    }
                }
                if (exception != null && !e.getClass().getName().contains(exception)) {
                    throw new AssertationFailedInTemplateException(
                            "Failure is not like expected. The exception class name " + _StringUtils
                                    .jQuote(e.getClass().getName())
                                    + " doesn't contain " + _StringUtils.jQuote(message) + ".",
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

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    private String getAsString(TemplateModel value, String paramName, Environment env)
            throws BadParameterTypeException, TemplateModelException {
        if (value == null) {
            return null;
        }
        if (value instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) value).getAsString();
        } else {
            throw new BadParameterTypeException(paramName, "string", value, env);
        }
    }

    private Pattern getAsPattern(TemplateModel value, String paramName, Environment env)
            throws TemplateModelException, BadParameterTypeException {
        String s = getAsString(value, paramName, env);
        return s != null ? Pattern.compile(s, Pattern.CASE_INSENSITIVE) : null;
    }

    private int getAsInt(TemplateModel value, int defaultValue, String paramName, Environment env) throws
            BadParameterTypeException,
            TemplateModelException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) value).getAsNumber().intValue(); 
        } else {
            throw new BadParameterTypeException(paramName, "number", value, env);
        }
    }

}
