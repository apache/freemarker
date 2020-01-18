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


import freemarker.template.SimpleCollection;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Constants;

/** {@code exp!defExp}, {@code (exp)!defExp} and the same two with {@code (exp)!}. */
class DefaultToExpression extends Expression {
	
    private static final TemplateCollectionModel EMPTY_COLLECTION = new SimpleCollection(new java.util.ArrayList(0));
    
	static private class EmptyStringAndSequenceAndHash implements TemplateScalarModel, TemplateSequenceModel,
	        TemplateHashModelEx2 {
		@Override
        public String getAsString() {
			return "";
		}
		@Override
        public TemplateModel get(int i) {
			return null;
		}
		@Override
        public TemplateModel get(String s) {
			return null;
		}
		@Override
        public int size() {
			return 0;
		}
		@Override
        public boolean isEmpty() {
			return true;
		}
		@Override
        public TemplateCollectionModel keys() {
			return EMPTY_COLLECTION;
		}
		@Override
        public TemplateCollectionModel values() {
			return EMPTY_COLLECTION;
		}
        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
            return Constants.EMPTY_KEY_VALUE_PAIR_ITERATOR;
        }
	}
	
	static final TemplateModel EMPTY_STRING_AND_SEQUENCE_AND_HASH = new EmptyStringAndSequenceAndHash();
	
	private final Expression lho, rho;
	
	DefaultToExpression(Expression lho, Expression rho) {
		this.lho = lho;
		this.rho = rho;
	}

	@Override
    TemplateModel _eval(Environment env) throws TemplateException {
		TemplateModel left;
		if (lho instanceof ParentheticalExpression) {
            boolean lastFIRE = env.setFastInvalidReferenceExceptions(true);
	        try {
                left = lho.eval(env);
	        } catch (InvalidReferenceException ire) {
	            left = null;
            } finally {
                env.setFastInvalidReferenceExceptions(lastFIRE);
	        }
		} else {
            left = lho.eval(env);
		}
		
		if (left != null) return left;
		else if (rho == null) return EMPTY_STRING_AND_SEQUENCE_AND_HASH;
		else return rho.eval(env);
	}

	@Override
    boolean isLiteral() {
		return false;
	}

	@Override
    protected Expression deepCloneWithIdentifierReplaced_inner(String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new DefaultToExpression(
                lho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                rho != null
                        ? rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState)
                        : null);
	}

	@Override
    public String getCanonicalForm() {
		if (rho == null) {
			return lho.getCanonicalForm() + '!';
		}
		return lho.getCanonicalForm() + '!' + rho.getCanonicalForm();
	}
	
	@Override
    String getNodeTypeSymbol() {
        return "...!...";
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return lho;
        case 1: return rho;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
        
}
