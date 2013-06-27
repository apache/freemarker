/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
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

/*
 * 22 October 1999: This class added by Holger Arendt.
 */

package freemarker.core;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.utility.NullWriter;


/**
 * A unary operator that calls a TemplateMethodModel.  It associates with the
 * <tt>Identifier</tt> or <tt>Dot</tt> to its left.
 */
final class MethodCall extends Expression {

    private final Expression target;
    private final ListLiteral arguments;

    MethodCall(Expression target, ArrayList arguments) {
        this(target, new ListLiteral(arguments));
    }

    private MethodCall(Expression target, ListLiteral arguments) {
        this.target = target;
        this.arguments = arguments;
    }

    TemplateModel _eval(Environment env) throws TemplateException
    {
        TemplateModel targetModel = target.eval(env);
        if (targetModel instanceof TemplateMethodModel) {
            TemplateMethodModel targetMethod = (TemplateMethodModel)targetModel;
            List argumentStrings = 
            targetMethod instanceof TemplateMethodModelEx
            ? arguments.getModelList(env)
            : arguments.getValueList(env);
            Object result = targetMethod.exec(argumentStrings);
            return env.getObjectWrapper().wrap(result);
        }
        else if (targetModel instanceof Macro) {
            Macro func = (Macro) targetModel;
            env.setLastReturnValue(null);
            if (!func.isFunction) {
                throw new _MiscTemplateException(env, "A macro cannot be called in an expression.");
            }
            Writer prevOut = env.getOut();
            try {
                env.setOut(NullWriter.INSTANCE);
                env.visit(func, null, arguments.items, null, null);
            } catch (IOException ioe) {
                throw new InternalError("This should be impossible.");
            } finally {
                env.setOut(prevOut);
            }
            return env.getLastReturnValue();
        }
        else {
            throw new UnexpectedTypeException(target, targetModel, "method", env);
        }
    }

    public String getCanonicalForm() {
        StringBuffer buf = new StringBuffer();
        buf.append(target.getCanonicalForm());
        buf.append("(");
        String list = arguments.getCanonicalForm();
        buf.append(list.substring(1, list.length() -1));
        buf.append(")");
        return buf.toString();
    }

    String getNodeTypeSymbol() {
        return "...(...)";
    }
    
    TemplateModel getConstantValue() {
        return null;
    }

    boolean isLiteral() {
        return false;
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new MethodCall(
                target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                (ListLiteral)arguments.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    int getParameterCount() {
        return 1 + arguments.items.size();
    }

    Object getParameterValue(int idx) {
        if (idx == 0) {
            return target;
        } else if (idx < getParameterCount()) {
            return arguments.items.get(idx - 1);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.CALLEE;
        } else if (idx < getParameterCount()) {
            return ParameterRole.ARGUMENT_VALUE;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

}
