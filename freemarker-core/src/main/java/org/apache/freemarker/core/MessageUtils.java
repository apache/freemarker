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

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.TemplateLanguageUtils;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtils {

    static final String UNKNOWN_DATE_TO_STRING_ERROR_MESSAGE
            = "Can't convert the date-like value to string because it isn't "
              + "known if it's a date (no time part), time or date-time value.";
    
    static final String UNKNOWN_DATE_TYPE_ERROR_TIP =
            "Use ?date, ?time, or ?dateTime to tell FreeMarker the exact type.";

    static final Object[] UNKNOWN_DATE_TO_STRING_TIPS = {
            UNKNOWN_DATE_TYPE_ERROR_TIP,
            "If you need a particular format only once, use ?string(pattern), like ?string('dd.MM.yyyy HH:mm:ss'), "
            + "to specify which fields to display. "
    };

    static final String FM3_SNAKE_CASE = "\nThe name contains '_' character, but since FreeMarker 3 names defined "
            + "by the template language use camel case (e.g. someExampleName).";

    static final String EMBEDDED_MESSAGE_BEGIN = "---begin-message---\n";

    static final String EMBEDDED_MESSAGE_END = "\n---end-message---";

    static final String ERROR_MESSAGE_HR = "----";
    
    static final String STRING_COERCABLE_TYPES_DESC
            = "string or something automatically convertible to string (number, date or boolean)";
    static final Class[] EXPECTED_TYPES_STRING_COERCABLE = new Class[] {
        TemplateStringModel.class, TemplateNumberModel.class, TemplateDateModel.class, TemplateBooleanModel.class
    };

    static final String STRING_COERCABLE_TYPES_OR_TOM_DESC
            = STRING_COERCABLE_TYPES_DESC + ", or \"template output\"";
    static final Class[] EXPECTED_TYPES_STRING_COERCABLE_TYPES_AND_TOM;
    static {
        EXPECTED_TYPES_STRING_COERCABLE_TYPES_AND_TOM = new Class[EXPECTED_TYPES_STRING_COERCABLE.length + 1];
        int i;
        for (i = 0; i < EXPECTED_TYPES_STRING_COERCABLE.length; i++) {
            EXPECTED_TYPES_STRING_COERCABLE_TYPES_AND_TOM[i] = EXPECTED_TYPES_STRING_COERCABLE[i];
        }
        EXPECTED_TYPES_STRING_COERCABLE_TYPES_AND_TOM[i] = TemplateMarkupOutputModel.class;
    }

    static final String EXPECTED_TYPE_ITERABLE_DESC = "iterable (like a sequence)";

    private MessageUtils() {
        // Not meant to be instantiated
    }

    static String formatLocationForSimpleParsingError(String templateSourceOrLookupName, int line, int column) {
        return formatLocation("in", templateSourceOrLookupName, line, column);
    }

    static String formatLocationForEvaluationError(Template template, int line, int column) {
        return formatLocation("at", template, line, column);
    }

    static String formatLocationForEvaluationError(ASTDirMacroOrFunction macro, int line, int column) {
        Template t = macro.getTemplate();
        return formatLocation("at", t != null ? t.getSourceOrLookupName() : null, macro.getName(), macro.isFunction(),
                line, column);
    }

    private static String formatLocation(String preposition, Template template, int line, int column) {
        return formatLocation(preposition, template != null ? template.getSourceOrLookupName() : null, line, column);
    }

    private static String formatLocation(String preposition, String templateSourceName, int line, int column) {
        return formatLocation(
                preposition, templateSourceName,
                null, false,
                line, column);
    }

    private static String formatLocation(
            String preposition, String templateSourceName,
            String macroOrFuncName, boolean isFunction,
            int line, int column) {
        String templateDesc;
        if (line < 0) {
            templateDesc = "?eval-ed string";
            macroOrFuncName = null;
        } else {
            templateDesc = templateSourceName != null
                ? "template " + _StringUtils.jQuoteNoXSS(templateSourceName)
                : "nameless template";
        }
        return "in " + templateDesc
              + (macroOrFuncName != null
                      ? " in " + (isFunction ? "function " : "macro ") + _StringUtils.jQuote(macroOrFuncName)
                      : "")
              + " "
              + preposition + " " + formatPosition(line, column);
    }

    static String formatPosition(int line, int column) {
        return "line " + (line >= 0 ? line : line - (ASTNode.RUNTIME_EVAL_LINE_DISPLACEMENT - 1))
                + ", column " + column;
    }

    /**
     * Returns a single line string that is no longer than {@code maxLength}.
     * If will truncate the string at line-breaks too.
     * The truncation is always signaled with a a {@code "..."} at the end of the result string.
     */
    static String shorten(String s, int maxLength) {
        if (maxLength < 5) maxLength = 5;

        boolean isTruncated = false;
        
        int brIdx = s.indexOf('\n');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        }
        brIdx = s.indexOf('\r');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        }
        
        if (s.length() > maxLength) {
            s = s.substring(0, maxLength - 3);
            isTruncated = true;
        }
        
        if (!isTruncated) {
            return s;
        } else {
            if (s.endsWith(".")) {
                if (s.endsWith("..")) {
                    if (s.endsWith("...")) {
                        return s;
                    } else {
                        return s + ".";
                    }
                } else {
                    return s + "..";
                }
            } else {
                return s + "...";
            }
        }
    }
    
    static StringBuilder appendExpressionAsUntearable(StringBuilder sb, ASTExpression argExp) {
        boolean needParen =
                !(argExp instanceof ASTExpNumberLiteral)
                && !(argExp instanceof ASTExpStringLiteral)
                && !(argExp instanceof ASTExpBooleanLiteral)
                && !(argExp instanceof ASTExpListLiteral)
                && !(argExp instanceof ASTExpHashLiteral)
                && !(argExp instanceof ASTExpVariable)
                && !(argExp instanceof ASTExpDot)
                && !(argExp instanceof ASTExpDynamicKeyName)
                && !(argExp instanceof ASTExpFunctionCall)
                && !(argExp instanceof ASTExpBuiltIn);
        if (needParen) sb.append('(');
        sb.append(argExp.getCanonicalForm());
        if (needParen) sb.append(')');
        return sb;
    }

    static TemplateException newInstantiatingClassNotAllowedException(String className, Environment env) {
        return new TemplateException(env,
                "Instantiating ", className, " is not allowed in the template for security reasons.");
    }
    
    static TemplateException newCantFormatUnknownTypeDateException(
            ASTExpression dateSourceExpr, UnknownDateTypeFormattingUnsupportedException cause) {
        return new TemplateException(cause, null, new _ErrorDescriptionBuilder(
                UNKNOWN_DATE_TO_STRING_ERROR_MESSAGE)
                .blame(dateSourceExpr)
                .tips(UNKNOWN_DATE_TO_STRING_TIPS));
    }

    static TemplateException newCantFormatDateException(TemplateDateFormat format, ASTExpression dataSrcExp,
                                                        TemplateValueFormatException e) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "Failed to format date/time/dateTime with format ", new _DelayedJQuote(format.getDescription()), ": ",
                e.getMessage())
                .blame(dataSrcExp); 
        return new TemplateException(e, null, desc);
    }
    
    static TemplateException newCantFormatNumberException(TemplateNumberFormat format, ASTExpression dataSrcExp,
                                                          TemplateValueFormatException e) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "Failed to format number with format ", new _DelayedJQuote(format.getDescription()), ": ",
                e.getMessage())
                .blame(dataSrcExp); 
        return new TemplateException(e, null, desc);
    }
    
    /**
     * @return "a" or "an" or "a(n)" (or "" for empty string) for an FTL type name
     */
    static String getAOrAn(String s) {
        if (s == null) return null;
        if (s.length() == 0) return "";
        
        char fc = Character.toLowerCase(s.charAt(0));
        if (fc == 'a' || fc == 'e' || fc == 'i') {
            return "an";
        } else if (fc == 'h') { 
            String ls = s.toLowerCase();
            if (ls.startsWith("has") || ls.startsWith("hi")) { 
                return "a";
            } else if (ls.startsWith("ht")) { 
                return "an";
            } else {
                return "a(n)";
            }
        } else if (fc == 'u' || fc == 'o') {
            return "a(n)";
        } else {
            char sc = (s.length() > 1) ? s.charAt(1) : '\0'; 
            if (fc == 'x' && !(sc == 'a' || sc == 'e' || sc == 'i' || sc == 'a' || sc == 'o' || sc == 'u')) {
                return "an";
            } else {
                return "a";
            }
        }
    }

    static TemplateException newUnexpectedOperandTypeException(
            ASTExpression blamed, TemplateModel model, Class<? extends TemplateModel> expectedType, Environment env)
            throws InvalidReferenceException {
        return newUnexpectedOperandTypeException(blamed, model, null, new Class[] { expectedType }, null, env);
    }

    static TemplateException newUnexpectedOperandTypeException(
            ASTExpression blamed, TemplateModel model, String expectedTypesDesc, Class<? extends TemplateModel> expectedType,
            Object[] tips,
            Environment env)
            throws InvalidReferenceException {
        return newUnexpectedOperandTypeException(
                blamed, model, expectedTypesDesc, new Class[] { expectedType }, tips, env);
    }

    static TemplateException newUnexpectedOperandTypeException(
            ASTExpression blamed, TemplateModel model, String expectedTypesDesc, Class[] expectedTypes, Object[] tips,
            Environment env)
            throws InvalidReferenceException {
        return new TemplateException(null, env, blamed, newUnexpectedOperandTypeDescriptionBuilder(
                blamed,
                null,
                model, expectedTypesDesc, expectedTypes, env)
                .tips(tips));
    }

    static TemplateException newUnexpectedAssignmentTargetTypeException(
            String blamedAssignmentTargetVarName, TemplateModel model, Class<? extends TemplateModel> expectedType,
            Environment env)
            throws InvalidReferenceException {
        return newUnexpectedAssignmentTargetTypeException(
                blamedAssignmentTargetVarName, model, null, new Class[] { expectedType }, null, env);
    }

    /**
     * Used for assignments that use {@code +=} and such.
     */
    static TemplateException newUnexpectedAssignmentTargetTypeException(
            String blamedAssignmentTargetVarName, TemplateModel model, String expectedTypesDesc, Class[] expectedTypes,
            Object[] tips,
            Environment env)
            throws InvalidReferenceException {
        return new TemplateException(null, env, null, newUnexpectedOperandTypeDescriptionBuilder(
                null,
                blamedAssignmentTargetVarName,
                model, expectedTypesDesc, expectedTypes, env).tips(tips));
    }

    private static _ErrorDescriptionBuilder newUnexpectedOperandTypeDescriptionBuilder(
            ASTExpression blamed, String blamedAssignmentTargetVarName,
            TemplateModel model, String expectedTypesDesc, Class<? extends TemplateModel>[] expectedTypes, Environment env)
            throws InvalidReferenceException {
        if (model == null) {
            throw InvalidReferenceException.getInstance(blamed, env);
        }

        if (expectedTypesDesc == null) {
            if (expectedTypes.length != 1) {
                throw new BugException("Can't generate expectedTypesDesc");
            }
            expectedTypesDesc = TemplateLanguageUtils.getTypeName(expectedTypes[0]);
        }
        _ErrorDescriptionBuilder errorDescBuilder = new _ErrorDescriptionBuilder(
                "Expected ", new _DelayedAOrAn(expectedTypesDesc), ", but ",
                (
                        blamedAssignmentTargetVarName != null
                                ? new Object[] {
                                "assignment target variable ",
                                new _DelayedJQuote(blamedAssignmentTargetVarName) }
                                : blamed != null
                                        ? "this"
                                        : "the expression"
                ),
                " has evaluated to ",
                new _DelayedAOrAn(new _DelayedTemplateLanguageTypeDescription(model)),
                (blamed != null ? ":" : ".")
        )
        .blame(blamed).showBlamer(true);
        if (model instanceof _UnexpectedTypeErrorExplainerTemplateModel) {
            Object[] tip = ((_UnexpectedTypeErrorExplainerTemplateModel) model).explainTypeError(expectedTypes);
            if (tip != null) {
                errorDescBuilder.tip(tip);
            }
        }
        return errorDescBuilder;
    }

}
