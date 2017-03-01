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

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._StringUtil;

/**
 * AST directive node: An instruction that makes a single assignment, like {@code <#local x=1>}, {@code <#global x=1>},
 * {@code <#assign x=1>}.
 * This is also used as the child of {@link ASTDirAssignmentsContainer}, if there are multiple assignments in the same
 * tag, like in {@code <#local x=1 y=2>}.
 */
final class ASTDirAssignment extends ASTDirective {

    // These must not clash with ArithmeticExpression.TYPE_... constants: 
    private static final int OPERATOR_TYPE_EQUALS = 0x10000;
    private static final int OPERATOR_TYPE_PLUS_EQUALS = 0x10001;
    private static final int OPERATOR_TYPE_PLUS_PLUS = 0x10002;
    private static final int OPERATOR_TYPE_MINUS_MINUS = 0x10003;
    
    private final int/*enum*/ scope;
    private final String variableName;
    private final int operatorType;
    private final ASTExpression valueExp;
    private ASTExpression namespaceExp;

    static final int NAMESPACE = 1;
    static final int LOCAL = 2;
    static final int GLOBAL = 3;
    
    private static final Number ONE = Integer.valueOf(1);

    /**
     * @param variableName the variable name to assign to.
     * @param valueExp the expression to assign.
     * @param scope the scope of the assignment, one of NAMESPACE, LOCAL, or GLOBAL
     */
    ASTDirAssignment(String variableName,
            int operator,
            ASTExpression valueExp,
            int scope) {
        this.scope = scope;
        
        this.variableName = variableName;
        
        if (operator == FMParserConstants.EQUALS) {
            operatorType = OPERATOR_TYPE_EQUALS;
        } else {
            switch (operator) {
            case FMParserConstants.PLUS_PLUS:
                operatorType = OPERATOR_TYPE_PLUS_PLUS;
                break;
            case FMParserConstants.MINUS_MINUS:
                operatorType = OPERATOR_TYPE_MINUS_MINUS;
                break;
            case FMParserConstants.PLUS_EQUALS:
                operatorType = OPERATOR_TYPE_PLUS_EQUALS;
                break;
            case FMParserConstants.MINUS_EQUALS:
                operatorType = ArithmeticExpression.TYPE_SUBSTRACTION;
                break;
            case FMParserConstants.TIMES_EQUALS:
                operatorType = ArithmeticExpression.TYPE_MULTIPLICATION;
                break;
            case FMParserConstants.DIV_EQUALS:
                operatorType = ArithmeticExpression.TYPE_DIVISION;
                break;
            case FMParserConstants.MOD_EQUALS:
                operatorType = ArithmeticExpression.TYPE_MODULO;
                break;
            default:
                throw new BugException();
            }
        }
        
        this.valueExp = valueExp;
    }
    
    void setNamespaceExp(ASTExpression namespaceExp) {
        if (scope != NAMESPACE && namespaceExp != null) throw new BugException();
        this.namespaceExp =  namespaceExp;
    }

    @Override
    ASTElement[] accept(Environment env) throws TemplateException {
        final Environment.Namespace namespace;
        if (namespaceExp == null) {
            switch (scope) {
            case LOCAL:
                namespace = null;
                break;
            case GLOBAL:
                namespace = env.getGlobalNamespace();
                break;
            case NAMESPACE:
                namespace = env.getCurrentNamespace();
                break;
            default:
                throw new BugException("Unexpected scope type: " + scope);
            }
        } else {
            TemplateModel namespaceTM = namespaceExp.eval(env);
            try {
                namespace = (Environment.Namespace) namespaceTM;
            } catch (ClassCastException e) {
                throw new NonNamespaceException(namespaceExp, namespaceTM, env);
            }
            if (namespace == null) {
                throw InvalidReferenceException.getInstance(namespaceExp, env);
            }
        }
        
        TemplateModel value;
        if (operatorType == OPERATOR_TYPE_EQUALS) {
            value = valueExp.eval(env);
            valueExp.assertNonNull(value, env);
        } else {
            TemplateModel lhoValue;
            if (namespace == null) {
                lhoValue = env.getLocalVariable(variableName);
            } else {
                lhoValue = namespace.get(variableName);
            }
            
            if (operatorType == OPERATOR_TYPE_PLUS_EQUALS) {  // Add or concat operation
                if (lhoValue == null) {
                    throw InvalidReferenceException.getInstance(
                            variableName, getOperatorTypeAsString(), env);
                }
                
                value = valueExp.eval(env);
                valueExp.assertNonNull(value, env);
                value = ASTExpAddOrConcat._eval(env, namespaceExp, null, lhoValue, valueExp, value);
            } else {  // Numerical operation
                Number lhoNumber;
                if (lhoValue instanceof TemplateNumberModel) {
                    lhoNumber = _EvalUtil.modelToNumber((TemplateNumberModel) lhoValue, null);
                } else if (lhoValue == null) {
                    throw InvalidReferenceException.getInstance(variableName, getOperatorTypeAsString(), env);
                } else {
                    throw new NonNumericalException(variableName, lhoValue, null, env);
                }

                if (operatorType == OPERATOR_TYPE_PLUS_PLUS) {
                    value  = ASTExpAddOrConcat._evalOnNumbers(env, getParent(), lhoNumber, ONE);
                } else if (operatorType == OPERATOR_TYPE_MINUS_MINUS) {
                    value = ArithmeticExpression._eval(
                            env, getParent(), lhoNumber, ArithmeticExpression.TYPE_SUBSTRACTION, ONE);
                } else { // operatorType == ArithmeticExpression.TYPE_...
                    Number rhoNumber = valueExp.evalToNumber(env);
                    value = ArithmeticExpression._eval(env, this, lhoNumber, operatorType, rhoNumber);
                }
            }
        }
        
        if (namespace == null) {
            env.setLocalVariable(variableName, value);
        } else {
            namespace.put(variableName, value);
        }
        return null;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        String dn = getParent() instanceof ASTDirAssignmentsContainer ? null : getNodeTypeSymbol();
        if (dn != null) {
            if (canonical) buf.append("<");
            buf.append(dn);
            buf.append(' ');
        }
        
        buf.append(_StringUtil.toFTLTopLevelTragetIdentifier(variableName));
        
        if (valueExp != null) {
            buf.append(' ');
        }
        buf.append(getOperatorTypeAsString());
        if (valueExp != null) {
            buf.append(' ');
            buf.append(valueExp.getCanonicalForm());
        }
        if (dn != null) {
            if (namespaceExp != null) {
                buf.append(" in ");
                buf.append(namespaceExp.getCanonicalForm());
            }
            if (canonical) buf.append(">");
        }
        return buf.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return getDirectiveName(scope);
    }
    
    static String getDirectiveName(int scope) {
        if (scope == ASTDirAssignment.LOCAL) {
            return "#local";
        } else if (scope == ASTDirAssignment.GLOBAL) {
            return "#global";
        } else if (scope == ASTDirAssignment.NAMESPACE) {
            return "#assign";
        } else {
            return "#{unknown_assignment_type}";
        }
    }
    
    @Override
    int getParameterCount() {
        return 5;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return variableName;
        case 1: return getOperatorTypeAsString();
        case 2: return valueExp;
        case 3: return Integer.valueOf(scope);
        case 4: return namespaceExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ASSIGNMENT_TARGET;
        case 1: return ParameterRole.ASSIGNMENT_OPERATOR;
        case 2: return ParameterRole.ASSIGNMENT_SOURCE;
        case 3: return ParameterRole.VARIABLE_SCOPE;
        case 4: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
    private String getOperatorTypeAsString() {
        if (operatorType == OPERATOR_TYPE_EQUALS) {
            return "=";
        } else if (operatorType == OPERATOR_TYPE_PLUS_EQUALS) {
            return "+=";
        } else if (operatorType == OPERATOR_TYPE_PLUS_PLUS) {
            return "++";
        } else if (operatorType == OPERATOR_TYPE_MINUS_MINUS) {
            return "--";
        } else {
            return ArithmeticExpression.getOperatorSymbol(operatorType) + "=";
        }
    }
    
}
