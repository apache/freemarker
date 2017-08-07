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
import java.io.Writer;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
// TODO [FM3] Most functionality here should be made public on some way
public final class _TemplateCallableModelUtils {

    private _TemplateCallableModelUtils() {
        //
    }

    public static void executeWith0Arguments(
            TemplateDirectiveModel directive, CallPlace callPlace, Writer out, Environment env)
            throws IOException, TemplateException {
        directive.execute(getArgumentArrayWithNoArguments(directive), callPlace, out, env);
    }

    public static TemplateModel executeWith0Arguments(
            TemplateFunctionModel function, CallPlace callPlace, Environment env)
            throws TemplateException {
        return function.execute(getArgumentArrayWithNoArguments(function), callPlace, env);
    }

    private static TemplateModel[] getArgumentArrayWithNoArguments(TemplateCallableModel callable) {
        ArgumentArrayLayout argsLayout = callable.getArgumentArrayLayout();
        int totalLength = argsLayout.getTotalLength();
        if (totalLength == 0) {
            return Constants.EMPTY_TEMPLATE_MODEL_ARRAY;
        } else {
            TemplateModel[] args = new TemplateModel[totalLength];

            int positionalVarargsArgumentIndex = argsLayout.getPositionalVarargsArgumentIndex();
            if (positionalVarargsArgumentIndex != -1) {
                args[positionalVarargsArgumentIndex] = Constants.EMPTY_SEQUENCE;
            }

            int namedVarargsArgumentIndex = argsLayout.getNamedVarargsArgumentIndex();
            if (namedVarargsArgumentIndex != -1) {
                args[namedVarargsArgumentIndex] = Constants.EMPTY_SEQUENCE;
            }
            
            return args;
        }
    }

    public static TemplateNumberModel castArgumentToNumber(TemplateModel[] args, int argIndex, boolean allowNull,
            Environment env) throws TemplateException {
        return getTemplateNumberModel(args[argIndex], argIndex, allowNull, env);
    }

    private static TemplateNumberModel getTemplateNumberModel(TemplateModel argValue, int argIndex, boolean allowNull,
            Environment env) throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return (TemplateNumberModel) argValue;
        }
        if (argValue == null) {
            if (allowNull) {
                return null;
            }
            throw new _MiscTemplateException(env,
                    "The ", new _DelayedOrdinal(argIndex + 1), " argument can't be null.");
        }
        throw new NonNumericalException(argIndex, argValue, null, env);
    }

    public static TemplateNumberModel castArgumentToNumber(TemplateModel argValue, String argName, boolean allowNull,
            Environment env) throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return (TemplateNumberModel) argValue;
        }
        if (argValue == null) {
            if (allowNull) {
                return null;
            }
            throw new _MiscTemplateException(env,
                    "The ", new _DelayedJQuote(argName), " argument can't be null.");
        }
        throw new NonNumericalException(argName, argValue, null, env);
    }

}
