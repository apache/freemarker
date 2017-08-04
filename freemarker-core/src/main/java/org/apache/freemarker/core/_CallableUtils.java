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
import java.io.Serializable;
import java.io.Writer;
import java.util.List;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
// TODO [FM3] Most functionality here should be made public on some way. Also BuiltIn-s has some duplicates utiltity
// methods for this functionality (checking arguments). Need to clean this up.
public final class _CallableUtils {

    private _CallableUtils() {
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

    public static Number castArgToNumber(TemplateModel[] args, int argIndex, boolean allowNull)
            throws TemplateException {
        return castArgToNumber(args[argIndex], argIndex, allowNull);
    }

    public static Number castArgToNumber(TemplateModel argValue, int argIndex, boolean allowNull)
            throws TemplateException {
        return castArgToNumber(argValue, null, argIndex, allowNull);
    }

    public static Number castArgToNumber(TemplateModel argValue, String argName, boolean allowNull)
            throws TemplateException {
        return castArgToNumber(argValue, argName, -1, allowNull);
    }

    private static Number castArgToNumber(TemplateModel argValue, String argName, int argIndex, boolean allowNull)
            throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) argValue).getAsNumber();
        }
        if (argValue == null) {
            if (allowNull) {
                return null;
            }
            throw new _MiscTemplateException(
                    "The ", argName != null ? new _DelayedJQuote(argName) : new _DelayedOrdinal(argIndex + 1),
                    " argument can't be null.");
        }
        throw new NonNumericalException((Serializable) argName != null ? argName : argIndex, argValue, null, null);
    }
    
    //

    public static String castArgToString(List<? extends TemplateModel> args, int argIndex) throws TemplateException {
        return castArgToString(args, argIndex, false);
    }

    public static String castArgToString(List<? extends TemplateModel> args, int argIndex, boolean allowNull) throws
            TemplateException {
        return castArgToString(args.get(argIndex), argIndex, allowNull);
    }

    public static String castArgToString(TemplateModel[] args, int argIndex, boolean allowNull) throws TemplateException {
        return castArgToString(args[argIndex], argIndex, allowNull);
    }

    public static String castArgToString(TemplateModel argValue, int argIndex) throws TemplateException {
        return castArgToString(argValue, argIndex, false);
    }

    public static String castArgToString(TemplateModel argValue, int argIndex, boolean allowNull) throws TemplateException {
        return castArgToString(argValue, null, argIndex, allowNull);
    }

    public static String castArgToString(TemplateModel argValue, String argName, boolean allowNull) throws TemplateException {
        return castArgToString(argValue, argName, -1, allowNull);
    }

    private static String castArgToString(
            TemplateModel argValue, String argName, int argIndex,
            boolean allowNull) throws TemplateException {
        if (argValue instanceof TemplateScalarModel) {
            return _EvalUtil.modelToString((TemplateScalarModel) argValue, null, null);
        }
        if (argValue == null) {
            if (allowNull) {
                return null;
            }
            throw new _MiscTemplateException(
                    "The ", argName != null ? new _DelayedJQuote(argName) : new _DelayedOrdinal(argIndex + 1),
                    " argument can't be null.");
        }
        throw new NonStringException((Serializable) argName != null ? argName : argIndex, argValue, null, null);
    }
    
}
