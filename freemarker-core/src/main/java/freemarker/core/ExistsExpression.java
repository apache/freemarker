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

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/** {@code exp??} and {@code (exp)??} */
class ExistsExpression extends Expression {
	
	protected final Expression exp;
	
	ExistsExpression(Expression exp) {
		this.exp = exp;
	}

	@Override
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

	@Override
    boolean isLiteral() {
		return false;
	}

	@Override
    protected Expression deepCloneWithIdentifierReplaced_inner(String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
		return new ExistsExpression(
		        exp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
	}

	@Override
    public String getCanonicalForm() {
		return exp.getCanonicalForm() + getNodeTypeSymbol();
	}
	
	@Override
    String getNodeTypeSymbol() {
        return "??";
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        return exp;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return ParameterRole.LEFT_HAND_OPERAND;
    }
	
}
