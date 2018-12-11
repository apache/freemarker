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

/**
 * A subclass of {@link TemplateException} that says that an FTL expression has evaluated to {@code null} or it refers
 * to something that doesn't exist. At least in FreeMarker 2.3.x these two cases aren't distinguished.
 */
public class InvalidReferenceException extends TemplateException {

    static final InvalidReferenceException FAST_INSTANCE;
    static {
        Environment prevEnv = Environment.getCurrentEnvironment();
        try {
            Environment.setCurrentEnvironment(null);
            FAST_INSTANCE = new InvalidReferenceException(
                    "Invalid reference. Details are unavilable, as this should have been handled by an FTL construct. "
                    + "If it wasn't, that's problably a bug in FreeMarker.",
                    null);
        } finally {
            Environment.setCurrentEnvironment(prevEnv);
        }
    }
    
    private static final Object[] TIP = {
        "If the failing expression is known to be legally refer to something that's sometimes null or missing, "
        + "either specify a default value like myOptionalVar!myDefault, or use ",
        "<#if myOptionalVar??>", "when-present", "<#else>", "when-missing", "</#if>",
        ". (These only cover the last step of the expression; to cover the whole expression, "
        + "use parenthesis: (myOptionalVar.foo)!myDefault, (myOptionalVar.foo)??"
    };

    private static final Object[] TIP_MISSING_ASSIGNMENT_TARGET = {
            "If the target variable is known to be legally null or missing sometimes, instead of something like ",
            "<#assign x += 1>", ", you could write ", "<#if x??>", "<#assign x += 1>", "</#if>",
            " or ", "<#assign x = (x!0) + 1>"
    };
    
    private static final String TIP_NO_DOLLAR =
            "Variable references must not start with \"$\", unless the \"$\" is really part of the variable name.";

    private static final String TIP_LAST_STEP_DOT =
            "It's the step after the last dot that caused this error, not those before it.";

    private static final String TIP_LAST_STEP_SQUARE_BRACKET =
            "It's the final [] step that caused this error, not those before it.";
    
    private static final String TIP_JSP_TAGLIBS =
            "The \"JspTaglibs\" variable isn't a core FreeMarker feature; "
            + "it's only available when templates are invoked through org.apache.freemarker.servlet.FreemarkerServlet"
            + " (or through some other custom FreeMarker-JSP integration solution).";
    
    /**
     * Creates and invalid reference exception that contains no information about what was missing or null.
     * As such, try to avoid this constructor.
     */
    public InvalidReferenceException(Environment env) {
        super("Invalid reference: The expression has evaluated to null or refers to something that doesn't exist.",
                env);
    }

    /**
     * Creates and invalid reference exception that contains no programmatically extractable information about the
     * blamed expression. As such, try to avoid this constructor, unless need to raise this expression from outside
     * the FreeMarker core.
     */
    public InvalidReferenceException(String description, Environment env) {
        super(description, env);
    }

    /**
     * This is the recommended constructor, but it's only used internally, and has no backward compatibility guarantees.
     * 
     * @param expression The expression that evaluates to missing or null. The last step of the expression should be
     *     the failing one, like in {@code goodStep.failingStep.furtherStep} it should only contain
     *     {@code goodStep.failingStep}.
     */
    InvalidReferenceException(_ErrorDescriptionBuilder description, Environment env, ASTExpression expression) {
        super(null, env, expression, description);
    }

    /**
     * Use this whenever possible, as it returns {@link #FAST_INSTANCE} instead of creating a new instance, when
     * appropriate.
     */
    static InvalidReferenceException getInstance(ASTExpression blamed, Environment env) {
        if (env != null && env.getFastInvalidReferenceExceptions()) {
            return FAST_INSTANCE;
        } else {
            if (blamed != null) {
                final _ErrorDescriptionBuilder errDescBuilder
                        = new _ErrorDescriptionBuilder("The following has evaluated to null or missing:").blame(blamed);
                if (endsWithDollarVariable(blamed)) {
                    errDescBuilder.tips(TIP_NO_DOLLAR, TIP);
                } else if (blamed instanceof ASTExpDot) {
                    final String rho = ((ASTExpDot) blamed).getRHO();
                    String nameFixTip = null;
                    if ("size".equals(rho)) {
                        nameFixTip = "To query the size of a collection or map use ?size, like myList?size";
                    } else if ("length".equals(rho)) {
                        nameFixTip = "To query the length of a string use ?length, like myString?size";
                    }
                    errDescBuilder.tips(
                            nameFixTip == null
                                    ? new Object[] { TIP_LAST_STEP_DOT, TIP }
                                    : new Object[] { TIP_LAST_STEP_DOT, nameFixTip, TIP });
                } else if (blamed instanceof ASTExpDynamicKeyName) {
                    errDescBuilder.tips(TIP_LAST_STEP_SQUARE_BRACKET, TIP);
                } else if (blamed instanceof ASTExpVariable
                        && ((ASTExpVariable) blamed).getName().equals("JspTaglibs")) {
                    errDescBuilder.tips(TIP_JSP_TAGLIBS, TIP);
                } else {
                    errDescBuilder.tip(TIP);
                }
                return new InvalidReferenceException(errDescBuilder, env, blamed);
            } else {
                return new InvalidReferenceException(env);
            }
        }
    }
    
    /**
     * Used for assignments that use operators like {@code +=}, when the target variable was null/missing. 
     */
    static InvalidReferenceException getInstance(int scope, String missingAssignedVarName, String assignmentOperator,
            Environment env) {
        if (env != null && env.getFastInvalidReferenceExceptions()) {
            return FAST_INSTANCE;
        } else {
            final _ErrorDescriptionBuilder errDescBuilder = new _ErrorDescriptionBuilder(
                    "The target variable of the assignment, ",
                    new _DelayedJQuote(missingAssignedVarName),
                    ", was null or missing in the " + ASTDirAssignment.scopeAsString(scope) + ", and the \"",
                    assignmentOperator, "\" operator must get its value from there before assigning to it."
                    );
            if (missingAssignedVarName.startsWith("$")) {
                errDescBuilder.tips(TIP_NO_DOLLAR, TIP_MISSING_ASSIGNMENT_TARGET);
            } else {
                errDescBuilder.tip(TIP_MISSING_ASSIGNMENT_TARGET);
            }
            return new InvalidReferenceException(errDescBuilder, env, null);
        }
    }

    private static boolean endsWithDollarVariable(ASTExpression blame) {
        return blame instanceof ASTExpVariable && ((ASTExpVariable) blame).getName().startsWith("$")
                || blame instanceof ASTExpDot && ((ASTExpDot) blame).getRHO().startsWith("$");
    }
    
}
