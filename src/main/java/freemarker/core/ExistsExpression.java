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

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/** {@code exp??} and {@code (exp)??} */
class ExistsExpression extends Expression {
	
	protected final Expression exp;
	
	ExistsExpression(Expression exp) {
		this.exp = exp;
	}

	TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel tm;
	    if (exp instanceof ParentheticalExpression) {
            boolean lastFIRE = env.setFastInvalidReferenceExceptions(true);
            try {
                tm = exp.eval(env);
            } catch (InvalidReferenceException ire) {
                tm = null;
            } finally {
                env.setFastInvalidReferenceExceptions(lastFIRE);
            }
	    } else {
            tm = exp.eval(env);
	    }
		return tm == null ? TemplateBooleanModel.FALSE : TemplateBooleanModel.TRUE;
	}

	boolean isLiteral() {
		return false;
	}

	protected Expression deepCloneWithIdentifierReplaced_inner(String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		return new ExistsExpression(
		        exp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
	}

	public String getCanonicalForm() {
		return exp.getCanonicalForm() + getNodeTypeSymbol();
	}
	
	String getNodeTypeSymbol() {
        return "??";
    }

    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        return exp;
    }

    ParameterRole getParameterRole(int idx) {
        return ParameterRole.LEFT_HAND_OPERAND;
    }
	
}
