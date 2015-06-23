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

package freemarker.core;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * An instruction that makes a single assignment, like [#local x=1].
 * This is also used as the child of {@link AssignmentInstruction}, if there are multiple assignments in the same tag,
 * like in [#local x=1 x=2].
 */
final class Assignment extends TemplateElement {

    // These must not clash with ArithmeticExpression.TYPE_... constants: 
    private static final int OPERATOR_TYPE_EQUALS = 0x10000;
    private static final int OPERATOR_TYPE_PLUS_EQUALS = 0x10001;
    private static final int OPERATOR_TYPE_PLUS_PLUS = 0x10002;
    private static final int OPERATOR_TYPE_MINUS_MINUS = 0x10003;
    
    private final int/*enum*/ scope;
    private final String variableName;
    private final int operatorType;
    private final Expression valueExp;
    private Expression namespaceExp;

    static final int NAMESPACE = 1;
    static final int LOCAL = 2;
    static final int GLOBAL = 3;
    
    // Java 5: Integer.valueOf(1)
    private static final Number ONE = new Integer(1);

    /**
     * @param variableName the variable name to assign to.
     * @param valueExp the expression to assign.
     * @param scope the scope of the assignment, one of NAMESPACE, LOCAL, or GLOBAL
     */
    Assignment(String variableName,
            int operator,
            Expression valueExp,
            int scope) {
        this.scope = scope;
        
        this.variableName = variableName;
        
        if (operator == FMParserTokenManager.EQUALS) {
            operatorType = OPERATOR_TYPE_EQUALS;
        } else {
            switch (operator) {
            case FMParserTokenManager.PLUS_PLUS:
                operatorType = OPERATOR_TYPE_PLUS_PLUS;
                break;
            case FMParserTokenManager.MINUS_MINUS:
                operatorType = OPERATOR_TYPE_MINUS_MINUS;
                break;
            case FMParserTokenManager.PLUS_EQUALS:
                operatorType = OPERATOR_TYPE_PLUS_EQUALS;
                break;
            case FMParserTokenManager.MINUS_EQUALS:
                operatorType = ArithmeticExpression.TYPE_SUBSTRACTION;
                break;
            case FMParserTokenManager.TIMES_EQUALS:
                operatorType = ArithmeticExpression.TYPE_MULTIPLICATION;
                break;
            case FMParserTokenManager.DIV_EQUALS:
                operatorType = ArithmeticExpression.TYPE_DIVISION;
                break;
            case FMParserTokenManager.MOD_EQUALS:
                operatorType = ArithmeticExpression.TYPE_MODULO;
                break;
            default:
                throw new BugException();
            }
        }
        
        this.valueExp = valueExp;
    }
    
    void setNamespaceExp(Expression namespaceExp) {
        if (scope != NAMESPACE && namespaceExp != null) throw new BugException();
        this.namespaceExp =  namespaceExp;
    }

    void accept(Environment env) throws TemplateException {
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
            if (value == null) {
                if (env.isClassicCompatible()) {
                    value = TemplateScalarModel.EMPTY_STRING;
                } else {
                    throw InvalidReferenceException.getInstance(valueExp, env);
                }
            }
        } else {
            TemplateModel lhoValue;
            if (namespace == null) {
                lhoValue = env.getLocalVariable(variableName);
            } else {
                lhoValue = namespace.get(variableName);
            }
            
            if (operatorType == OPERATOR_TYPE_PLUS_EQUALS) {  // Add or concat operation
                if (lhoValue == null) {
                    if (env.isClassicCompatible()) {
                        lhoValue = TemplateScalarModel.EMPTY_STRING;
                    } else {
                        throw InvalidReferenceException.getInstance(
                                variableName, getOperatorTypeAsString(), env);
                    }
                }
                
                value = valueExp.eval(env);
                if (value == null) {
                    if (env.isClassicCompatible()) {
                        value = TemplateScalarModel.EMPTY_STRING;
                    } else {
                        throw InvalidReferenceException.getInstance(valueExp, env);
                    }
                }
                value = AddConcatExpression._eval(env, namespaceExp, null, lhoValue, valueExp, value);
            } else {  // Numerical operation
                Number lhoNumber;
                if (lhoValue instanceof TemplateNumberModel) {
                    lhoNumber = EvalUtil.modelToNumber((TemplateNumberModel) lhoValue, null);
                } else if (lhoValue == null) {
                    throw InvalidReferenceException.getInstance(variableName, getOperatorTypeAsString(), env);
                } else {
                    throw new NonNumericalException(variableName, lhoValue, null, env);
                }

                if (operatorType == OPERATOR_TYPE_PLUS_PLUS) {
                    value  = AddConcatExpression._evalOnNumbers(env, getParentElement(), lhoNumber, ONE);
                } else if (operatorType == OPERATOR_TYPE_MINUS_MINUS) {
                    value = ArithmeticExpression._eval(
                            env, getParentElement(), lhoNumber, ArithmeticExpression.TYPE_SUBSTRACTION, ONE);
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
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        String dn = getParentElement() instanceof AssignmentInstruction ? null : getNodeTypeSymbol();
        if (dn != null) {
            if (canonical) buf.append("<");
            buf.append(dn);
            buf.append(' ');
        }
        
        buf.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(variableName));
        
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
        String result = buf.toString();
        return result;
    }
    
    String getNodeTypeSymbol() {
        return getDirectiveName(scope);
    }
    
    static String getDirectiveName(int scope) {
        if (scope == Assignment.LOCAL) {
            return "#local";
        } else if (scope == Assignment.GLOBAL) {
            return "#global";
        } else if (scope == Assignment.NAMESPACE) {
            return "#assign";
        } else {
            return "#{unknown_assignment_type}";
        }
    }
    
    int getParameterCount() {
        return 5;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return variableName;
        case 1: return getOperatorTypeAsString();
        case 2: return valueExp;
        case 3: return new Integer(scope);
        case 4: return namespaceExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

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
