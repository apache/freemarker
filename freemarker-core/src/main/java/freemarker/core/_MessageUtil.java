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

import java.util.ArrayList;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.StringUtil;

/**
 * Used internally only, might change without notice!
 * Utilities for creating error messages (and other messages).
 */
public class _MessageUtil {

    static final String UNKNOWN_DATE_TO_STRING_ERROR_MESSAGE
            = "Can't convert the date-like value to string because it isn't "
              + "known if it's a date (no time part), time or date-time value.";
    
    static final String UNKNOWN_DATE_PARSING_ERROR_MESSAGE
            = "Can't parse the string to date-like value because it isn't "
              + "known if it's desired result should be a date (no time part), a time, or a date-time value.";

    static final String UNKNOWN_DATE_TYPE_ERROR_TIP = 
            "Use ?date, ?time, or ?datetime to tell FreeMarker the exact type.";
    
    static final Object[] UNKNOWN_DATE_TO_STRING_TIPS = {
            UNKNOWN_DATE_TYPE_ERROR_TIP,
            "If you need a particular format only once, use ?string(pattern), like ?string('dd.MM.yyyy HH:mm:ss'), "
            + "to specify which fields to display. "
    };

    static final String EMBEDDED_MESSAGE_BEGIN = "---begin-message---\n";

    static final String EMBEDDED_MESSAGE_END = "\n---end-message---";

    // Can't be instantiated
    private _MessageUtil() { }
        
    static String formatLocationForSimpleParsingError(Template template, int line, int column) {
        return formatLocation("in", template, line, column);
    }

    static String formatLocationForSimpleParsingError(String templateSourceName, int line, int column) {
        return formatLocation("in", templateSourceName, line, column);
    }

    static String formatLocationForDependentParsingError(Template template, int line, int column) {
        return formatLocation("on", template, line, column);
    }

    static String formatLocationForDependentParsingError(String templateSourceName, int line, int column) {
        return formatLocation("on", templateSourceName, line, column);
    }

    static String formatLocationForEvaluationError(Template template, int line, int column) {
        return formatLocation("at", template, line, column);
    }

    static String formatLocationForEvaluationError(Macro macro, int line, int column) {
        Template t = macro.getTemplate();
        return formatLocation("at", t != null ? t.getSourceName() : null, macro.getName(), macro.isFunction(), line, column);
    }
    
    static String formatLocationForEvaluationError(String templateSourceName, int line, int column) {
        return formatLocation("at", templateSourceName, line, column);
    }

    private static String formatLocation(String preposition, Template template, int line, int column) {
        return formatLocation(preposition, template != null ? template.getSourceName() : null, line, column);
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
                ? "template " + StringUtil.jQuoteNoXSS(templateSourceName)
                : "nameless template";
        }
        return "in " + templateDesc
              + (macroOrFuncName != null
                      ? " in " + (isFunction ? "function " : "macro ") + StringUtil.jQuote(macroOrFuncName)
                      : "")
              + " "
              + preposition + " " + formatPosition(line, column);
    }
    
    static String formatPosition(int line, int column) {
        return "line " + (line >= 0 ? line : line - (TemplateObject.RUNTIME_EVAL_LINE_DISPLACEMENT - 1))
                + ", column " + column;
    }

    /**
     * Returns a single line string that is no longer than {@code maxLength}.
     * If will truncate the string at line-breaks too.
     * The truncation is always signaled with a a {@code "..."} at the end of the result string.  
     */
    public static String shorten(String s, int maxLength) {
        if (maxLength < 5) maxLength = 5;
        
        boolean isTruncated = false;
        
        int brIdx = s.indexOf('\n');
        if (brIdx != -1) {
            s = s.substring(0, brIdx);
            isTruncated = true;
        };
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
    
    public static StringBuilder appendExpressionAsUntearable(StringBuilder sb, Expression argExp) {
        boolean needParen =
                !(argExp instanceof NumberLiteral)
                && !(argExp instanceof StringLiteral)
                && !(argExp instanceof BooleanLiteral)
                && !(argExp instanceof ListLiteral)
                && !(argExp instanceof HashLiteral)
                && !(argExp instanceof Identifier)
                && !(argExp instanceof Dot)
                && !(argExp instanceof DynamicKeyName)
                && !(argExp instanceof MethodCall)
                && !(argExp instanceof BuiltIn)
                && !(argExp instanceof ExistsExpression)
                && !(argExp instanceof ParentheticalExpression);
        if (needParen) sb.append('(');
        sb.append(argExp.getCanonicalForm());
        if (needParen) sb.append(')');
        return sb;
    }

    public static TemplateModelException newArgCntError(String methodName, int argCnt, int expectedCnt) {
        return newArgCntError(methodName, argCnt, expectedCnt, expectedCnt);
    }
    
    public static TemplateModelException newArgCntError(String methodName, int argCnt, int minCnt, int maxCnt) {
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

    public static TemplateModelException newMethodArgMustBeStringException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "string", arg);
    }

    public static TemplateModelException newMethodArgMustBeNumberException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "number", arg);
    }
    
    public static TemplateModelException newMethodArgMustBeBooleanException(String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "boolean", arg);
    }
    
    public static TemplateModelException newMethodArgMustBeExtendedHashException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "extended hash", arg);
    }

    public static TemplateModelException newMethodArgMustBeExtendedHashOrSequnceException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "extended hash or sequence", arg);
    }

    public static TemplateModelException newMethodArgMustBeSequenceException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence", arg);
    }
    
    public static TemplateModelException newMethodArgMustBeSequenceOrCollectionException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "sequence or collection", arg);
    }

    public static TemplateModelException newMethodArgMustBeStringOrMarkupOutputException(
            String methodName, int argIdx, TemplateModel arg) {
        return newMethodArgUnexpectedTypeException(methodName, argIdx, "string or markup output", arg);
    }

    public static TemplateModelException newMethodArgUnexpectedTypeException(
            String methodName, int argIdx, String expectedType, TemplateModel arg) {
        return new _TemplateModelException(
                methodName, "(...) expects ", new _DelayedAOrAn(expectedType), " as argument #", Integer.valueOf(argIdx + 1),
                ", but received ", new _DelayedAOrAn(new _DelayedFTLTypeDescription(arg)), ".");
    }
    
    /**
     * The type of the argument was good, but it's value wasn't.
     */
    public static TemplateModelException newMethodArgInvalidValueException(
            String methodName, int argIdx, Object... details) {
        return new _TemplateModelException(
                methodName, "(...) argument #", Integer.valueOf(argIdx + 1),
                " had invalid value: ", details);
    }

    /**
     * The type of the argument was good, but the values of two or more arguments are inconsistent with each other.
     */
    public static TemplateModelException newMethodArgsInvalidValueException(
            String methodName, Object... details) {
        return new _TemplateModelException(methodName, "(...) arguments have invalid value: ", details);
    }
    
    public static TemplateException newInstantiatingClassNotAllowedException(String className, Environment env) {
        return new _MiscTemplateException(env,
                "Instantiating ", className, " is not allowed in the template for security reasons.");
    }
    
    public static _TemplateModelException newCantFormatUnknownTypeDateException(
            Expression dateSourceExpr, UnknownDateTypeFormattingUnsupportedException cause) {
        return new _TemplateModelException(cause, null, new _ErrorDescriptionBuilder(
                _MessageUtil.UNKNOWN_DATE_TO_STRING_ERROR_MESSAGE)
                .blame(dateSourceExpr)
                .tips(_MessageUtil.UNKNOWN_DATE_TO_STRING_TIPS));
    }

    public static TemplateException newCantFormatDateException(TemplateDateFormat format, Expression dataSrcExp,
            TemplateValueFormatException e, boolean useTempModelExc) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "Failed to format date/time/datetime with format ", new _DelayedJQuote(format.getDescription()), ": ",
                e.getMessage())
                .blame(dataSrcExp); 
        return useTempModelExc
                ? new _TemplateModelException(e, null, desc)
                : new _MiscTemplateException(e, null, desc);
    }
    
    public static TemplateException newCantFormatNumberException(TemplateNumberFormat format, Expression dataSrcExp,
            TemplateValueFormatException e, boolean useTempModelExc) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                "Failed to format number with format ", new _DelayedJQuote(format.getDescription()), ": ",
                e.getMessage())
                .blame(dataSrcExp); 
        return useTempModelExc
                ? new _TemplateModelException(e, null, desc)
                : new _MiscTemplateException(e, null, desc);
    }

    public static TemplateModelException newKeyValuePairListingNonStringKeyExceptionMessage(
            TemplateModel key, TemplateHashModelEx listedHashEx) {
        return new _TemplateModelException(new _ErrorDescriptionBuilder(
                "When listing key-value pairs of traditional hash "
                + "implementations, all keys must be strings, but one of them "
                + "was ",
                new _DelayedAOrAn(new _DelayedFTLTypeDescription(key)), "."
                ).tip("The listed value's TemplateModel class was ",
                        new _DelayedShortClassName(listedHashEx.getClass()),
                        ", which doesn't implement ",
                        new _DelayedShortClassName(TemplateHashModelEx2.class),
                        ", which leads to this restriction."));
    }

    /**
     * Because of the limitations of FTL lambdas (called "local lambdas"), sometimes we must condense the lazy result
     * down into a sequence. However, doing that automatically is only allowed if the input was a sequence as well. If
     * it wasn't a sequence, we don't dare to collect the result into a sequence automatically (because it's possibly
     * too long), and that's when this error message comes.
     */
    public static TemplateException newLazilyGeneratedCollectionMustBeSequenceException(Expression blamed) {
        return new _MiscTemplateException(blamed,
                "The result is a listable value with lazy transformation(s) applied on it, but it's not " +
                "an FTL sequence (it's not a List-like value, but an Iterator-like value). The place doesn't " +
                "support such values due to technical limitations. So either pass it to a construct that supports " +
                "such values (like ", "<#list transformedListable as x>", "), or, if you know that you don't have " +
                "too many elements, use transformedListable?sequence to allow it to be treated as an FTL sequence.");
    }

    /**
     * @return "a" or "an" or "a(n)" (or "" for empty string) for an FTL type name
     */
    static public String getAOrAn(String s) {
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
