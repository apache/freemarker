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

import java.util.ArrayList;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;
import org.apache.freemarker.core.valueformat.UnknownDateTypeFormattingUnsupportedException;

/**
 * Utilities for creating error messages (and other messages).
 */
class MessageUtil {

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

    // Can't be instantiated
    private MessageUtil() { }

    static String formatLocationForSimpleParsingError(Template template, int line, int column) {
        return formatLocation("in", template, line, column);
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
                ? "template " + _StringUtil.jQuoteNoXSS(templateSourceName)
                : "nameless template";
        }
        return "in " + templateDesc
              + (macroOrFuncName != null
                      ? " in " + (isFunction ? "function " : "macro ") + _StringUtil.jQuote(macroOrFuncName)
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
                && !(argExp instanceof ASTExpMethodCall)
                && !(argExp instanceof ASTExpBuiltIn);
        if (needParen) sb.append('(');
        sb.append(argExp.getCanonicalForm());
        if (needParen) sb.append(')');
        return sb;
    }

    static TemplateModelException newArgCntError(String methodName, int argCnt, int expectedCnt) {
        return newArgCntError(methodName, argCnt, expectedCnt, expectedCnt);
    }
    
    static TemplateModelException newArgCntError(String methodName, int argCnt, int minCnt, int maxCnt) {
        ArrayList/*<Object>*/ desc = new ArrayList(20);
        
        desc.add(methodName);
        
        desc.add("(");
        if (maxCnt != 0) desc.add("...");
        desc.add(") expects ");
        
        if (minCnt == maxCnt) {
            if (maxCnt == 0) {
                desc.add("no");
            } else {
                desc.add(Integer.valueOf(maxCnt));
            }
        } else if (maxCnt - minCnt == 1) {
            desc.add(Integer.valueOf(minCnt));
            desc.add(" or ");
            desc.add(Integer.valueOf(maxCnt));
        } else {
            desc.add(Integer.valueOf(minCnt));
            if (maxCnt != Integer.MAX_VALUE) {
                desc.add(" to ");
                desc.add(Integer.valueOf(maxCnt));
            } else {
                desc.add(" or more (unlimited)");
            }
        }
        desc.add(" argument");
        if (maxCnt > 1) desc.add("s");
        
        desc.add(" but has received ");
        if (argCnt == 0) {
            desc.add("none");
        } else {
            desc.add(Integer.valueOf(argCnt));
        }
        desc.add(".");
        
        return new _TemplateModelException(desc.toArray());
    }

    static TemplateModelException newMethodArgMustBeStringException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "string", arg);
    }

    static TemplateModelException newMethodArgMustBeNumberException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "number", arg);
    }
    
    static TemplateModelException newMethodArgMustBeBooleanException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "boolean", arg);
    }
    
    static TemplateModelException newMethodArgMustBeExtendedHashException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "extended hash", arg);
    }
    
    static TemplateModelException newMethodArgMustBeSequenceException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence", arg);
    }
    
    static TemplateModelException newMethodArgMustBeSequenceOrCollectionException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence or collection", arg);
    }
    
    static TemplateModelException newMethodArgUnexpectedTypeException(
            String methodName, int argIdx, String expectedType, TemplateModel arg) {
        return new _TemplateModelException(
                methodName, "(...) expects ", new _DelayedAOrAn(expectedType), " as argument #", Integer.valueOf(argIdx + 1),
                ", but received ", new _DelayedAOrAn(new _DelayedFTLTypeDescription(arg)), ".");
    }
    
    /**
     * The type of the argument was good, but it's value wasn't.
     */
    static TemplateModelException newMethodArgInvalidValueException(
            String methodName, int argIdx, Object... details) {
        return new _TemplateModelException(
                methodName, "(...) argument #", Integer.valueOf(argIdx + 1),
                " had invalid value: ", details);
    }

    /**
     * The type of the argument was good, but the values of two or more arguments are inconsistent with each other.
     */
    static TemplateModelException newMethodArgsInvalidValueException(
            String methodName, Object... details) {
        return new _TemplateModelException(methodName, "(...) arguments have invalid value: ", details);
    }
    
    static TemplateException newInstantiatingClassNotAllowedException(String className, Environment env) {
        return new _MiscTemplateException(env,
                "Instantiating ", className, " is not allowed in the template for security reasons.");
    }
    
    static TemplateModelException newCantFormatUnknownTypeDateException(
            ASTExpression dateSourceExpr, UnknownDateTypeFormattingUnsupportedException cause) {
        return new _TemplateModelException(cause, null, new _ErrorDescriptionBuilder(
                MessageUtil.UNKNOWN_DATE_TO_STRING_ERROR_MESSAGE)
                .blame(dateSourceExpr)
                .tips(MessageUtil.UNKNOWN_DATE_TO_STRING_TIPS));
    }

    static TemplateException newCantFormatDateException(TemplateDateFormat format, ASTExpression dataSrcExp,
                                                        TemplateValueFormatException e, boolean useTempModelExc) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "Failed to format date/time/dateTime with format ", new _DelayedJQuote(format.getDescription()), ": ",
                e.getMessage())
                .blame(dataSrcExp); 
        return useTempModelExc
                ? new _TemplateModelException(e, null, desc)
                : new _MiscTemplateException(e, null, desc);
    }
    
    static TemplateException newCantFormatNumberException(TemplateNumberFormat format, ASTExpression dataSrcExp,
                                                          TemplateValueFormatException e, boolean useTempModelExc) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "Failed to format number with format ", new _DelayedJQuote(format.getDescription()), ": ",
                e.getMessage())
                .blame(dataSrcExp); 
        return useTempModelExc
                ? new _TemplateModelException(e, null, desc)
                : new _MiscTemplateException(e, null, desc);
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
    
}
