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


import freemarker.template.SimpleCollection;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/** {@code exp!defExp}, {@code (exp)!defExp} and the same two with {@code (exp)!}. */
class DefaultToExpression extends Expression {
	
    private static final TemplateCollectionModel EMPTY_COLLECTION = new SimpleCollection(new java.util.ArrayList(0));
    
	static private class EmptyStringAndSequence 
	  implements TemplateScalarModel, TemplateSequenceModel, TemplateHashModelEx {
		public String getAsString() {
			return "";
		}
		public TemplateModel get(int i) {
			return null;
		}
		public TemplateModel get(String s) {
			return null;
		}
		public int size() {
			return 0;
		}
		public boolean isEmpty() {
			return true;
		}
		public TemplateCollectionModel keys() {
			return EMPTY_COLLECTION;
		}
		public TemplateCollectionModel values() {
			return EMPTY_COLLECTION;
		}
		
	}
	
	static final TemplateModel EMPTY_STRING_AND_SEQUENCE = new EmptyStringAndSequence();
	
	private final Expression lho, rho;
	
	DefaultToExpression(Expression lho, Expression rho) {
		this.lho = lho;
		this.rho = rho;
	}

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
		else if (rho == null) return EMPTY_STRING_AND_SEQUENCE;
		else return rho.eval(env);
	}

	boolean isLiteral() {
		return false;
	}

	protected Expression deepCloneWithIdentifierReplaced_inner(String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new DefaultToExpression(
                lho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                rho != null
                        ? rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState)
                        : null);
	}

	public String getCanonicalForm() {
		if (rho == null) {
			return lho.getCanonicalForm() + '!';
		}
		return lho.getCanonicalForm() + '!' + rho.getCanonicalForm();
	}
	
	String getNodeTypeSymbol() {
        return "...!...";
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return lho;
        case 1: return rho;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
        
}
