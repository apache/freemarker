/*
 * Copyright (c) 2006 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
