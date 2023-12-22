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

import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template._ObjectWrappers;
import freemarker.template.utility.StringUtil;

class BuiltInsForStringsBasic {

    static class cap_firstBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            int i = 0;
            int ln = s.length();
            while (i < ln  &&  Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < ln) {
                StringBuilder b = new StringBuilder(s);
                b.setCharAt(i, Character.toUpperCase(s.charAt(i)));
                s = b.toString();
            }
            return new SimpleScalar(s);
        }
    }

    static class capitalizeBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.capitalize(s));
        }
    }

    static class chop_linebreakBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(StringUtil.chomp(s));
        }
    }

    static class containsBI extends BuiltIn {
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.indexOf(getStringMethodArg(args, 0)) != -1
                        ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToStringOrUnsupportedMarkup(env,
                    "For sequences/collections (lists and such) use \"?seq_contains\" instead."));
        }
    }

    static class ends_withBI extends BuiltInForString {
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.endsWith(getStringMethodArg(args, 0)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class ensure_ends_withBI extends BuiltInForString {
        
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                String suffix = getStringMethodArg(args, 0);
                return new SimpleScalar(s.endsWith(suffix) ? s : s + suffix);
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class ensure_starts_withBI extends BuiltInForString {
        
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1, 3);
                
                final String checkedPrefix = getStringMethodArg(args, 0);
                
                final boolean startsWithPrefix;
                final String addedPrefix; 
                if (args.size() > 1) {
                    addedPrefix = getStringMethodArg(args, 1);
                    long flags = args.size() > 2
                            ? RegexpHelper.parseFlagString(getStringMethodArg(args, 2))
                            : RegexpHelper.RE_FLAG_REGEXP;
                  
                    if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                        RegexpHelper.checkOnlyHasNonRegexpFlags(key, flags, true);
                        if ((flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) == 0) {
                            startsWithPrefix = s.startsWith(checkedPrefix);
                        } else {
                            startsWithPrefix = s.toLowerCase().startsWith(checkedPrefix.toLowerCase());
                        }
                    } else {
                        Pattern pattern = RegexpHelper.getPattern(checkedPrefix, (int) flags);
                        final Matcher matcher = pattern.matcher(s);
                        startsWithPrefix = matcher.lookingAt();
                    } 
                } else {
                    startsWithPrefix = s.startsWith(checkedPrefix);
                    addedPrefix = checkedPrefix;
                }
                return new SimpleScalar(startsWithPrefix ? s : addedPrefix + s);
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class index_ofBI extends BuiltIn {
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
            
            private BIMethod(String s) {
                this.s = s;
            }
            
            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String subStr = getStringMethodArg(args, 0);
                if (argCnt > 1) {
                    int startIdx = getNumberMethodArg(args, 1).intValue();
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr, startIdx) : s.indexOf(subStr, startIdx));
                } else {
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr) : s.indexOf(subStr));
                }
            }
        }
        
        private final boolean findLast;
    
        index_ofBI(boolean findLast) {
            this.findLast = findLast;
        }
        
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToStringOrUnsupportedMarkup(env,
                    "For sequences/collections (lists and such) use \"?seq_index_of\" instead."));
        }
    }
    
    static class keep_afterBI extends BuiltInForString {
        class KeepAfterMethod implements TemplateMethodModelEx {
            private String s;

            KeepAfterMethod(String s) {
                this.s = s;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String separatorString = getStringMethodArg(args, 0);
                long flags = argCnt > 1 ? RegexpHelper.parseFlagString(getStringMethodArg(args, 1)) : 0;
                
                int startIndex;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkOnlyHasNonRegexpFlags(key, flags, true);
                    if ((flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) == 0) {
                        startIndex = s.indexOf(separatorString);
                    } else {
                        startIndex = s.toLowerCase().indexOf(separatorString.toLowerCase());
                    }
                    if (startIndex >= 0) {
                        startIndex += separatorString.length();
                    }
                } else {
                    Pattern pattern = RegexpHelper.getPattern(separatorString, (int) flags);
                    final Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        startIndex = matcher.end();
                    } else {
                        startIndex = -1;
                    }
                } 
                return startIndex == -1 ? TemplateScalarModel.EMPTY_STRING : new SimpleScalar(s.substring(startIndex));
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new KeepAfterMethod(s);
        }
        
    }
    
    static class keep_after_lastBI extends BuiltInForString {
        class KeepAfterMethod implements TemplateMethodModelEx {
            private String s;

            KeepAfterMethod(String s) {
                this.s = s;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String separatorString = getStringMethodArg(args, 0);
                long flags = argCnt > 1 ? RegexpHelper.parseFlagString(getStringMethodArg(args, 1)) : 0;
                
                int startIndex;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkOnlyHasNonRegexpFlags(key, flags, true);
                    if ((flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) == 0) {
                        startIndex = s.lastIndexOf(separatorString);
                    } else {
                        startIndex = s.toLowerCase().lastIndexOf(separatorString.toLowerCase());
                    }
                    if (startIndex >= 0) {
                        startIndex += separatorString.length();
                    }
                } else {
                    if (separatorString.length() == 0) {
                        startIndex = s.length();
                    } else {
                        Pattern pattern = RegexpHelper.getPattern(separatorString, (int) flags);
                        final Matcher matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            startIndex = matcher.end();
                            while (matcher.find(matcher.start() + 1)) {
                                startIndex = matcher.end();
                            }
                        } else {
                            startIndex = -1;
                        }
                    }
                } 
                return startIndex == -1 ? TemplateScalarModel.EMPTY_STRING : new SimpleScalar(s.substring(startIndex));
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new KeepAfterMethod(s);
        }
        
    }
    
    static class keep_beforeBI extends BuiltInForString {
        class KeepUntilMethod implements TemplateMethodModelEx {
            private String s;

            KeepUntilMethod(String s) {
                this.s = s;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String separatorString = getStringMethodArg(args, 0);
                long flags = argCnt > 1 ? RegexpHelper.parseFlagString(getStringMethodArg(args, 1)) : 0;
                
                int stopIndex;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkOnlyHasNonRegexpFlags(key, flags, true);
                    if ((flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) == 0) {
                        stopIndex = s.indexOf(separatorString);
                    } else {
                        stopIndex = s.toLowerCase().indexOf(separatorString.toLowerCase());
                    }
                } else {
                    Pattern pattern = RegexpHelper.getPattern(separatorString, (int) flags);
                    final Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        stopIndex = matcher.start();
                    } else {
                        stopIndex = -1;
                    }
                } 
                return stopIndex == -1 ? new SimpleScalar(s) : new SimpleScalar(s.substring(0, stopIndex));
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new KeepUntilMethod(s);
        }
        
    }
    
    // TODO
    static class keep_before_lastBI extends BuiltInForString {
        class KeepUntilMethod implements TemplateMethodModelEx {
            private String s;

            KeepUntilMethod(String s) {
                this.s = s;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String separatorString = getStringMethodArg(args, 0);
                long flags = argCnt > 1 ? RegexpHelper.parseFlagString(getStringMethodArg(args, 1)) : 0;
                
                int stopIndex;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkOnlyHasNonRegexpFlags(key, flags, true);
                    if ((flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) == 0) {
                        stopIndex = s.lastIndexOf(separatorString);
                    } else {
                        stopIndex = s.toLowerCase().lastIndexOf(separatorString.toLowerCase());
                    }
                } else {
                    if (separatorString.length() == 0) {
                        stopIndex = s.length();
                    } else {
                        Pattern pattern = RegexpHelper.getPattern(separatorString, (int) flags);
                        final Matcher matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            stopIndex = matcher.start();
                            while (matcher.find(stopIndex + 1)) {
                                stopIndex = matcher.start();
                            }
                        } else {
                            stopIndex = -1;
                        }
                    }
                } 
                return stopIndex == -1 ? new SimpleScalar(s) : new SimpleScalar(s.substring(0, stopIndex));
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new KeepUntilMethod(s);
        }
        
    }
    
    static class lengthBI extends BuiltInForString {
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new SimpleNumber(s.length());
        }
        
    }    

    static class lower_caseBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.toLowerCase(env.getLocale()));
        }
    }

    static class c_lower_caseBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.toLowerCase(Locale.ROOT));
        }
    }

    static class padBI extends BuiltInForString {
        
        private class BIMethod implements TemplateMethodModelEx {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt  = args.size();
                checkMethodArgCount(argCnt, 1, 2);
    
                int width = getNumberMethodArg(args, 0).intValue();
    
                if (argCnt > 1) {
                    String filling = getStringMethodArg(args, 1);
                    try {
                        return new SimpleScalar(
                                leftPadder
                                        ? StringUtil.leftPad(s, width, filling)
                                        : StringUtil.rightPad(s, width, filling));
                    } catch (IllegalArgumentException e) {
                        if (filling.length() == 0) {
                            throw new _TemplateModelException(
                                    "?", key, "(...) argument #2 can't be a 0-length string.");
                        } else {
                            throw new _TemplateModelException(e,
                                    "?", key, "(...) failed: ", e);
                        }
                    }
                } else {
                    return new SimpleScalar(leftPadder ? StringUtil.leftPad(s, width) : StringUtil.rightPad(s, width));
                }
            }
        }
    
        private final boolean leftPadder;
    
        padBI(boolean leftPadder) {
            this.leftPadder = leftPadder;
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }
    
    static class remove_beginningBI extends BuiltInForString {
        
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                String prefix = getStringMethodArg(args, 0);
                return new SimpleScalar(s.startsWith(prefix) ? s.substring(prefix.length()) : s);
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class remove_endingBI extends BuiltInForString {
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                String suffix = getStringMethodArg(args, 0);
                return new SimpleScalar(s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s);
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }
    
    static class split_BI extends BuiltInForString {
        class SplitMethod implements TemplateMethodModel {
            private String s;

            SplitMethod(String s) {
                this.s = s;
            }

            @Override
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String splitString = (String) args.get(0);
                long flags = argCnt > 1 ? RegexpHelper.parseFlagString((String) args.get(1)) : 0;
                String[] result = null;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkNonRegexpFlags(key, flags);
                    result = StringUtil.split(s, splitString,
                            (flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) != 0);
                } else {
                    Pattern pattern = RegexpHelper.getPattern(splitString, (int) flags);
                    result = pattern.split(s);
                } 
                return ObjectWrapper.DEFAULT_WRAPPER.wrap(result);
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new SplitMethod(s);
        }
        
    }
    
    static class starts_withBI extends BuiltInForString {
    
        private class BIMethod implements TemplateMethodModelEx {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }
    
            @Override
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1);
                return s.startsWith(getStringMethodArg(args, 0)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class substringBI extends BuiltInForString {
        
        @Override
        TemplateModel calculateResult(final String s, final Environment env) throws TemplateException {
            return new TemplateMethodModelEx() {
                
                @Override
                public Object exec(java.util.List args) throws TemplateModelException {
                    int argCount = args.size();
                    checkMethodArgCount(argCount, 1, 2);
    
                    int beginIdx = getNumberMethodArg(args, 0).intValue();
    
                    final int len = s.length();
    
                    if (beginIdx < 0) {
                        throw newIndexLessThan0Exception(0, beginIdx);
                    } else if (beginIdx > len) {
                        throw newIndexGreaterThanLengthException(0, beginIdx, len);
                    }
    
                    if (argCount > 1) {
                        int endIdx = getNumberMethodArg(args, 1).intValue();
                        if (endIdx < 0) {
                            throw newIndexLessThan0Exception(1, endIdx);
                        } else if (endIdx > len) {
                            throw newIndexGreaterThanLengthException(1, endIdx, len);
                        }
                        if (beginIdx > endIdx) {
                            throw _MessageUtil.newMethodArgsInvalidValueException("?" + key,
                                    "The begin index argument, ", Integer.valueOf(beginIdx),
                                    ", shouldn't be greater than the end index argument, ",
                                    Integer.valueOf(endIdx), ".");
                        }
                        return new SimpleScalar(s.substring(beginIdx, endIdx));
                    } else {
                        return new SimpleScalar(s.substring(beginIdx));
                    }
                }
    
                private TemplateModelException newIndexGreaterThanLengthException(
                        int argIdx, int idx, final int len) throws TemplateModelException {
                    return _MessageUtil.newMethodArgInvalidValueException(
                            "?" + key, argIdx,
                            "The index mustn't be greater than the length of the string, ",
                            Integer.valueOf(len),
                            ", but it was ", Integer.valueOf(idx), ".");
                }
    
                private TemplateModelException newIndexLessThan0Exception(
                        int argIdx, int idx) throws TemplateModelException {
                    return _MessageUtil.newMethodArgInvalidValueException(
                            "?" + key, argIdx,
                            "The index must be at least 0, but was ", Integer.valueOf(idx), ".");
                }
                
            };
        }
    }

    static class trimBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.trim());
        }
    }

    static abstract class AbstractTruncateBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(final String s, final Environment env) {
            return new TemplateMethodModelEx() {
                @Override
                public Object exec(java.util.List args) throws TemplateModelException {
                    int argCount = args.size();
                    checkMethodArgCount(argCount, 1, 3);

                    int maxLength = getNumberMethodArg(args, 0).intValue();
                    if (maxLength < 0) {
                        throw new _TemplateModelException("?", key, "(...) argument #1 can't be negative.");
                    }

                    TemplateModel terminator;
                    Integer terminatorLength;
                    if (argCount > 1) {
                        terminator = (TemplateModel) args.get(1);
                        if (!(terminator instanceof  TemplateScalarModel)) {
                            if (allowMarkupTerminator()) {
                                if (!(terminator instanceof TemplateMarkupOutputModel)) {
                                    throw _MessageUtil.newMethodArgMustBeStringOrMarkupOutputException(
                                            "?" + key, 1, terminator);
                                }
                            } else {
                                throw _MessageUtil.newMethodArgMustBeStringException(
                                        "?" + key, 1, terminator);
                            }
                        }

                        Number terminatorLengthNum = getOptNumberMethodArg(args, 2);
                        terminatorLength = terminatorLengthNum != null ? terminatorLengthNum.intValue() : null;
                        if (terminatorLength != null && terminatorLength < 0) {
                            throw new _TemplateModelException("?", key, "(...) argument #3 can't be negative.");
                        }
                    } else {
                        terminator = null;
                        terminatorLength = null;
                    }
                    try {
                        TruncateBuiltinAlgorithm algorithm = env.getTruncateBuiltinAlgorithm();
                        return truncate(algorithm, s, maxLength, terminator, terminatorLength, env);
                    } catch (TemplateException e) {
                        throw new _TemplateModelException(
                                AbstractTruncateBI.this, e, env, "Truncation failed; see cause exception");
                    }
                }
            };
        }

        protected abstract TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException;

        protected abstract boolean allowMarkupTerminator();
    }

    static class truncateBI extends AbstractTruncateBI {
        @Override
        protected TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException {
            return algorithm.truncate(s, maxLength, (TemplateScalarModel) terminator, terminatorLength, env);
        }

        @Override
        protected boolean allowMarkupTerminator() {
            return false;
        }
    }

    static class truncate_wBI extends AbstractTruncateBI {
        @Override
        protected TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException {
            return algorithm.truncateW(s, maxLength, (TemplateScalarModel) terminator, terminatorLength, env);
        }

        @Override
        protected boolean allowMarkupTerminator() {
            return false;
        }
    }

    static class truncate_cBI extends AbstractTruncateBI {
        @Override
        protected TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException {
            return algorithm.truncateC(s, maxLength, (TemplateScalarModel) terminator, terminatorLength, env);
        }

        @Override
        protected boolean allowMarkupTerminator() {
            return false;
        }
    }

    static class truncate_mBI extends AbstractTruncateBI {
        @Override
        protected TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException {
            return algorithm.truncateM(s, maxLength, terminator, terminatorLength, env);
        }

        @Override
        protected boolean allowMarkupTerminator() {
            return true;
        }
    }

    static class truncate_w_mBI extends AbstractTruncateBI {
        @Override
        protected TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException {
            return algorithm.truncateWM(s, maxLength, terminator, terminatorLength, env);
        }

        @Override
        protected boolean allowMarkupTerminator() {
            return true;
        }
    }

    static class truncate_c_mBI extends AbstractTruncateBI {
        @Override
        protected TemplateModel truncate(
                TruncateBuiltinAlgorithm algorithm, String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength, Environment env)
                throws TemplateException {
            return algorithm.truncateCM(s, maxLength, terminator, terminatorLength, env);
        }

        @Override
        protected boolean allowMarkupTerminator() {
            return true;
        }
    }

    static class uncap_firstBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            int i = 0;
            int ln = s.length();
            while (i < ln  &&  Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            if (i < ln) {
                StringBuilder b = new StringBuilder(s);
                b.setCharAt(i, Character.toLowerCase(s.charAt(i)));
                s = b.toString();
            }
            return new SimpleScalar(s);
        }
    }

    static class upper_caseBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.toUpperCase(env.getLocale()));
        }
    }

    static class c_upper_caseBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleScalar(s.toUpperCase(Locale.ROOT));
        }
    }

    static class word_listBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            SimpleSequence result = new SimpleSequence(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
            StringTokenizer st = new StringTokenizer(s);
            while (st.hasMoreTokens()) {
               result.add(st.nextToken());
            }
            return result;
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsBasic() { }
    
}
