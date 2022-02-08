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

import java.util.List;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * A local lambada expression is a lambda expression that creates a function that can only be called from the same
 * context where it was created, and thus it doesn't need closure support. As of this writing (2019-02), this is the
 * only kind of lambda expression supported, as supporting closures would add overhead to many basic operations, while
 * local lambdas are "good enough" for the main use cases in templates (for filtering/transforming lists). Also,
 * closures can be quite confusing when the lambda expression refers to variables that are not effectively final,
 * such as a loop variable. So that's yet another issue to address if we go for less restricted lambdas.
 */
final class LocalLambdaExpression extends Expression {

    private final LambdaParameterList lho;
    private final Expression rho;

    LocalLambdaExpression(LambdaParameterList lho, Expression rho) {
        this.lho = lho;
        this.rho = rho;
    }

    @Override
    public String getCanonicalForm() {
        return lho.getCanonicalForm() + " -> " + rho.getCanonicalForm();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "->";
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        throw new TemplateException("Can't get lambda expression as a value: Lambdas currently can only be used on a " +
                "few special places.",
                env);
    }

    /**
     * Call the function defined by the lambda expression; overload specialized for 1 argument, the most common case.
     */
    TemplateModel invokeLambdaDefinedFunction(TemplateModel argValue, Environment env) throws TemplateException {
        return env.evaluateWithNewLocal(rho, lho.getParameters().get(0).getName(),
                argValue != null ? argValue : TemplateNullModel.INSTANCE);
    }

    @Override
    boolean isLiteral() {
        // As we don't support true lambdas, they can't be evaluted in parse time.
        return false;
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        for (Identifier parameter : lho.getParameters()) {
            if (parameter.getName().equals(replacedIdentifier)) {
                // As Expression.deepCloneWithIdentifierReplaced was exposed to users back then, now we can't add
                // "throws ParseException" to this, therefore, we use UncheckedParseException as a workaround.
                throw new UncheckedParseException(new ParseException(
                        "Escape placeholder (" + replacedIdentifier + ") can't be used in the " +
                        "parameter list of a lambda expressions.", this));
            }
        }

        return new LocalLambdaExpression(
    	        lho,
    	        rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    int getParameterCount() {
        return lho.getParameters().size() + 1;
    }

    @Override
    Object getParameterValue(int idx) {
        int paramCount = getParameterCount();
        if (idx < paramCount - 1) {
            return lho.getParameters().get(idx);
        } else if (idx == paramCount - 1) {
            return rho;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        int paramCount = getParameterCount();
        if (idx < paramCount - 1) {
            return ParameterRole.ARGUMENT_NAME;
        } else if (idx == paramCount - 1) {
            return ParameterRole.VALUE;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    LambdaParameterList getLambdaParameterList() {
        return lho;
    }

    /** The left side of the `->`. */
    static class LambdaParameterList {
        private final Token openingParenthesis;
        private final Token closingParenthesis;
        private final List<Identifier> parameters;

        public LambdaParameterList(Token openingParenthesis, List<Identifier> parameters, Token closingParenthesis) {
            this.openingParenthesis = openingParenthesis;
            this.closingParenthesis = closingParenthesis;
            this.parameters = parameters;
        }

        /** Maybe {@code null} */
        public Token getOpeningParenthesis() {
            return openingParenthesis;
        }

        /** Maybe {@code null} */
        public Token getClosingParenthesis() {
            return closingParenthesis;
        }

        public List<Identifier> getParameters() {
            return parameters;
        }

        public String getCanonicalForm() {
            if (parameters.size() == 1) {
                return parameters.get(0).getCanonicalForm();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append('(');
                for (int i = 0; i < parameters.size(); i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    Identifier parameter = parameters.get(i);
                    sb.append(parameter.getCanonicalForm());
                }
                sb.append(')');
                return sb.toString();
            }
        }
    }

}
