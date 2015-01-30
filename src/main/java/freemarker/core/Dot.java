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
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * The dot operator. Used to reference items inside a
 * <code>TemplateHashModel</code>.
 */
final class Dot extends Expression {
    private final Expression target;
    private final String key;

    Dot(Expression target, String key) {
        this.target = target;
        this.key = key;
    }

    TemplateModel _eval(Environment env) throws TemplateException
    {
        TemplateModel leftModel = target.eval(env);
        if(leftModel instanceof TemplateHashModel) {
            return ((TemplateHashModel) leftModel).get(key);
        }
        if (leftModel == null && env.isClassicCompatible()) {
            return null; // ${noSuchVar.foo} has just printed nothing in FM 1.
        }
        throw new NonHashException(target, leftModel, env);
    }

    public String getCanonicalForm() {
        return target.getCanonicalForm() + getNodeTypeSymbol() + _CoreStringUtils.toFTLIdentifierReferenceAfterDot(key);
    }
    
    String getNodeTypeSymbol() {
        return ".";
    }
    
    boolean isLiteral() {
        return target.isLiteral();
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new Dot(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        key);
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        return idx == 0 ? (Object) target : (Object) key;
    }

    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
    
    String getRHO() {
        return key;
    }

    boolean onlyHasIdentifiers() {
        return (target instanceof Identifier) || ((target instanceof Dot) && ((Dot) target).onlyHasIdentifiers());
    }
}