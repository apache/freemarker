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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.TemplateModelUtils;

class BuiltInsForCallables {

    static abstract class AbstractWithArgsBI extends BuiltIn {

        protected abstract boolean isOrderLast();

        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel model = target.eval(env);
            if (model instanceof Macro) {
                return new BIMethodForMacroAndFunction((Macro) model);
            } else if (model instanceof TemplateDirectiveModel) {
                return new BIMethodForDirective((TemplateDirectiveModel) model);
            } else if (model instanceof TemplateMethodModel) {
                return new BIMethodForMethod((TemplateMethodModel) model);
            } else {
                throw new UnexpectedTypeException(
                        target, model,
                        "macro, function, directive, or method", new Class[] { Macro.class,
                        TemplateDirectiveModel.class, TemplateMethodModel.class },
                        env);
            }
        }

        private class BIMethodForMacroAndFunction implements TemplateMethodModelEx {

            private final Macro macroOrFunction;

            private BIMethodForMacroAndFunction(Macro macroOrFunction) {
                this.macroOrFunction = macroOrFunction;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args.size(), 1);
                TemplateModel argTM = (TemplateModel) args.get(0);

                Macro.WithArgs withArgs;
                if (argTM instanceof TemplateSequenceModel) {
                    withArgs = new Macro.WithArgs((TemplateSequenceModel) argTM, isOrderLast());
                } else if (argTM instanceof TemplateHashModelEx) {
                    if (macroOrFunction.isFunction()) {
                        throw new _TemplateModelException("When applied on a function, ?",  key,
                                " can't have a hash argument. Use a sequence argument.");
                    }
                    withArgs = new Macro.WithArgs((TemplateHashModelEx) argTM, isOrderLast());
                } else {
                    throw _MessageUtil.newMethodArgMustBeExtendedHashOrSequnceException("?" + key, 0, argTM);
                }

                return new Macro(macroOrFunction, withArgs);
            }

        }

        private class BIMethodForMethod implements TemplateMethodModelEx {

            private final TemplateMethodModel method;

            public BIMethodForMethod(TemplateMethodModel method) {
                this.method = method;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args.size(), 1);
                TemplateModel argTM = (TemplateModel) args.get(0);

                if (argTM instanceof TemplateSequenceModel) {
                    final TemplateSequenceModel withArgs = (TemplateSequenceModel) argTM;
                    if (method instanceof TemplateMethodModelEx) {
                        return new TemplateMethodModelEx() {
                            @Override
                            public Object exec(List origArgs) throws TemplateModelException {
                                int withArgsSize = withArgs.size();
                                List<TemplateModel> newArgs = new ArrayList<>(
                                        withArgsSize + origArgs.size());

                                if (isOrderLast()) {
                                    newArgs.addAll(origArgs);
                                }
                                for (int i = 0; i < withArgsSize; i++) {
                                    newArgs.add(withArgs.get(i));
                                }
                                if (!isOrderLast()) {
                                    newArgs.addAll(origArgs);
                                }

                                return method.exec(newArgs);
                            }
                        };
                    } else {
                        return new TemplateMethodModel() {
                            @Override
                            public Object exec(List origArgs) throws TemplateModelException {
                                int withArgsSize = withArgs.size();
                                List<String> newArgs = new ArrayList<>(
                                        withArgsSize + origArgs.size());

                                if (isOrderLast()) {
                                    newArgs.addAll(origArgs);
                                }
                                for (int i = 0; i < withArgsSize; i++) {
                                    TemplateModel argVal = withArgs.get(i);
                                    newArgs.add(argValueToString(argVal));
                                }
                                if (!isOrderLast()) {
                                    newArgs.addAll(origArgs);
                                }

                                return method.exec(newArgs);
                            }

                            /**
                             * Mimics the behavior of method call expression when it calls legacy method model.
                             */
                            private String argValueToString(TemplateModel argVal) throws TemplateModelException {
                                String argValStr;
                                if (argVal instanceof TemplateScalarModel) {
                                    argValStr = ((TemplateScalarModel) argVal).getAsString();
                                } else if (argVal == null) {
                                    argValStr = null;
                                } else {
                                    try {
                                        argValStr = EvalUtil.coerceModelToPlainText(argVal, null, null,
                                                Environment.getCurrentEnvironment());
                                    } catch (TemplateException e) {
                                        throw new _TemplateModelException(e,
                                                "Failed to convert method argument to string. Argument type was: ",
                                                new _DelayedFTLTypeDescription(argVal));
                                    }
                                }
                                return argValStr;
                            }
                        };
                    }
                } else if (argTM instanceof TemplateHashModelEx) {
                    throw new _TemplateModelException("When applied on a method, ?",  key,
                            " can't have a hash argument. Use a sequence argument.");
                } else {
                    throw _MessageUtil.newMethodArgMustBeExtendedHashOrSequnceException("?" + key, 0, argTM);
                }
            }

        }

        private class BIMethodForDirective implements TemplateMethodModelEx {

            private final TemplateDirectiveModel directive;

            public BIMethodForDirective(TemplateDirectiveModel directive) {
                this.directive = directive;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args.size(), 1);
                TemplateModel argTM = (TemplateModel) args.get(0);

                if (argTM instanceof TemplateHashModelEx) {
                    final TemplateHashModelEx withArgs = (TemplateHashModelEx) argTM;
                    return new TemplateDirectiveModel() {
                        @Override
                        public void execute(Environment env, Map origArgs, TemplateModel[] loopVars,
                                TemplateDirectiveBody body) throws TemplateException, IOException {
                            int withArgsSize = withArgs.size();
                            // This is unnecessarily big if there are overridden arguments, but we care more about
                            // avoiding rehashing.
                            Map<String, TemplateModel> newArgs = new LinkedHashMap<>(
                                    (withArgsSize + origArgs.size()) * 4 / 3, 1f);

                            TemplateHashModelEx2.KeyValuePairIterator withArgsIter =
                                    TemplateModelUtils.getKeyValuePairIterator(withArgs);
                            if (isOrderLast()) {
                                newArgs.putAll(origArgs);
                                while (withArgsIter.hasNext()) {
                                    TemplateHashModelEx2.KeyValuePair withArgsKVP = withArgsIter.next();
                                    String argName = getArgumentName(withArgsKVP);
                                    if (!newArgs.containsKey(argName)) {
                                        newArgs.put(argName, withArgsKVP.getValue());
                                    }
                                }
                            } else {
                                while (withArgsIter.hasNext()) {
                                    TemplateHashModelEx2.KeyValuePair withArgsKVP = withArgsIter.next();
                                    newArgs.put(getArgumentName(withArgsKVP), withArgsKVP.getValue());
                                }
                                newArgs.putAll(origArgs);
                            }

                            directive.execute(env, newArgs, loopVars, body);
                        }

                        private String getArgumentName(TemplateHashModelEx2.KeyValuePair withArgsKVP) throws
                                TemplateModelException {
                            TemplateModel argNameTM = withArgsKVP.getKey();
                            if (!(argNameTM instanceof TemplateScalarModel)) {
                                throw new _TemplateModelException(
                                        "Expected string keys in the ?", key, "(...) arguments, " +
                                        "but one of the keys was ",
                                        new _DelayedAOrAn(new _DelayedFTLTypeDescription(argNameTM)), ".");
                            }
                            return EvalUtil.modelToString((TemplateScalarModel) argNameTM, null, null);
                        }
                    };
                } else if (argTM instanceof TemplateSequenceModel) {
                    throw new _TemplateModelException("When applied on a directive, ?",  key,
                            "(...) can't have a sequence argument. Use a hash argument.");
                } else {
                    throw _MessageUtil.newMethodArgMustBeExtendedHashOrSequnceException("?" + key, 0, argTM);
                }
            }

        }

    }

    static final class with_argsBI extends AbstractWithArgsBI {
        @Override
        protected boolean isOrderLast() {
            return false;
        }
    }

    static final class with_args_lastBI extends AbstractWithArgsBI {
        @Override
        protected boolean isOrderLast() {
            return true;
        }
    }

}
