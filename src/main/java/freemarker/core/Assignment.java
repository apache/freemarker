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

    private final int/*enum*/ scope;
    private final String variableName;
    private final int operator;
    private final Expression valueExp;
    private Expression namespaceExp;

    static final int NAMESPACE = 1;
    static final int LOCAL = 2;
    static final int GLOBAL = 3;

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
        this.operator = operator;
        this.valueExp = valueExp;
    }
    
    void setNamespaceExp(Expression namespaceExp) {
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
        if (operator == FMParserTokenManager.EQUALS) {
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
            
            if (lhoValue == null) {
                if (env.isClassicCompatible()) {
                    lhoValue = TemplateScalarModel.EMPTY_STRING;
                } else {
                    throw InvalidReferenceException.getInstance(
                            variableName, assignmentOperatorToString(operator), env);
                }
            }
            
            if (operator == FMParserTokenManager.PLUS_EQUALS) {
                value = valueExp.eval(env);
                if (value == null) {
                    if (env.isClassicCompatible()) {
                        value = TemplateScalarModel.EMPTY_STRING;
                    } else {
                        throw InvalidReferenceException.getInstance(valueExp, env);
                    }
                }
                value = AddConcatExpression._eval(env, namespaceExp, null, lhoValue, valueExp, value);
            } else {
                Number lhoNumber;
                if (lhoValue instanceof TemplateNumberModel) {
                    lhoNumber = EvalUtil.modelToNumber((TemplateNumberModel) lhoValue, null);
                } else {
                    throw new NonNumericalException(variableName, lhoValue, null, env);
                }

                Number rhoNumber = valueExp.evalToNumber(env);
                
                int arithExpType;
                switch (operator) {
                case FMParserTokenManager.MINUS_EQUALS:
                    arithExpType = ArithmeticExpression.TYPE_SUBSTRACTION;
                    break;
                case FMParserTokenManager.TIMES_EQUALS:
                    arithExpType = ArithmeticExpression.TYPE_MULTIPLICATION;
                    break;
                case FMParserTokenManager.DIV_EQUALS:
                    arithExpType = ArithmeticExpression.TYPE_DIVISION;
                    break;
                case FMParserTokenManager.MOD_EQUALS:
                    arithExpType = ArithmeticExpression.TYPE_MODULO;
                    break;
                default:
                    throw new BugException();
                }
                
                value = ArithmeticExpression._eval(env, this, lhoNumber, arithExpType, rhoNumber);
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
        String dn = parent instanceof AssignmentInstruction ? null : getNodeTypeSymbol();
        if (dn != null) {
            if (canonical) buf.append("<");
            buf.append(dn);
            buf.append(' ');
        }
        
        buf.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(variableName));
        
        buf.append(' ');
        buf.append(assignmentOperatorToString(operator));
        buf.append(' ');
        buf.append(valueExp.getCanonicalForm());
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
        return 4;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return variableName;
        case 1: return valueExp;
        case 2: return new Integer(scope);
        case 3: return namespaceExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ASSIGNMENT_TARGET;
        case 1: return ParameterRole.ASSIGNMENT_SOURCE;
        case 2: return ParameterRole.VARIABLE_SCOPE;
        case 3: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
    private String assignmentOperatorToString(int op) {
        switch (op) {
        case FMParserTokenManager.PLUS_EQUALS: return "+=";
        case FMParserTokenManager.MINUS_EQUALS: return "-=";
        case FMParserTokenManager.TIMES_EQUALS: return "*=";
        case FMParserTokenManager.DIV_EQUALS: return "/=";
        case FMParserTokenManager.MOD_EQUALS: return "%=";
        case FMParserTokenManager.EQUALS: return "=";
        default: return "{unrecognized operator}=";
        }
    }
    
}
