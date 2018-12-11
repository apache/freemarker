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

import static org.apache.freemarker.core.util.CallableUtils.*;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util._StringUtils;

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
            return new SimpleString(s);
        }
    }

    static class capitalizeBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.capitalize(s));
        }
    }

    static class chop_linebreakBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(_StringUtils.chomp(s));
        }
    }

    static class containsBI extends ASTExpBuiltIn {
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                return s.contains(CallableUtils.getStringArgument(args, 0, this))
                        ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }
        }
    
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToStringOrUnsupportedMarkup(env,
                    "For iterables (like sequences) use \"?seqContains\" instead."));
        }
    }

    static class ends_withBI extends BuiltInForString {
    
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                return s.endsWith(getStringArgument(args, 0, this)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class ensure_ends_withBI extends BuiltInForString {
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String suffix = getStringArgument(args, 0, this);
                return new SimpleString(s.endsWith(suffix) ? s : s + suffix);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class ensure_starts_withBI extends BuiltInForString {
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                final String checkedPrefix = getStringArgument(args, 0, this);

                final boolean startsWithPrefix;
                final String addedPrefix;
                String addedPrefixArg = getOptionalStringArgument(args, 1, this);
                String flagsArg = getOptionalStringArgument(args, 2, this);
                if (addedPrefixArg != null) {
                    addedPrefix = addedPrefixArg;
                    long flags = flagsArg != null
                            ? RegexpHelper.parseFlagString(flagsArg)
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
                    if (flagsArg != null) {
                        throw new TemplateException(
                                "The 2nd parameter must be non-null when the 3rd parameter is non-null");
                    }
                    startsWithPrefix = s.startsWith(checkedPrefix);
                    addedPrefix = checkedPrefix;
                }
                return new SimpleString(startsWithPrefix ? s : addedPrefix + s);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.THREE_POSITIONAL_PARAMETERS;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class index_ofBI extends ASTExpBuiltIn {
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            private final String s;
            
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String subStr = getStringArgument(args, 0, this);
                Number indexModel = getOptionalNumberArgument(args, 1, this);
                if (indexModel != null) {
                    int startIdx = indexModel.intValue();
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr, startIdx) : s.indexOf(subStr, startIdx));
                } else {
                    return new SimpleNumber(findLast ? s.lastIndexOf(subStr) : s.indexOf(subStr));
                }
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        private final boolean findLast;
    
        index_ofBI(boolean findLast) {
            this.findLast = findLast;
        }
        
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            return new BIMethod(target.evalAndCoerceToStringOrUnsupportedMarkup(env,
                    "For iterables (like seqiences) use \"?seqIndexOf\" instead."));
        }
    }
    
    static class keep_afterBI extends BuiltInForString {

        class KeepAfterMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;

            KeepAfterMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String separatorString = getStringArgument(args, 0, this);

                String flagsStr = getOptionalStringArgument(args, 1, this);
                long flags = flagsStr != null ? RegexpHelper.parseFlagString(flagsStr) : 0;

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
                return startIndex == -1 ? TemplateStringModel.EMPTY_STRING : new SimpleString(s.substring(startIndex));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new KeepAfterMethod(s);
        }
        
    }
    
    static class keep_after_lastBI extends BuiltInForString {
        class KeepAfterMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;

            KeepAfterMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String separatorString = getStringArgument(args, 0, this);
                String flagString = getOptionalStringArgument(args, 1, this);
                long flags = flagString != null ? RegexpHelper.parseFlagString(flagString) : 0;

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
                return startIndex == -1 ? TemplateStringModel.EMPTY_STRING : new SimpleString(s.substring(startIndex));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new KeepAfterMethod(s);
        }
        
    }
    
    static class keep_beforeBI extends BuiltInForString {

        class KeepUntilMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;

            KeepUntilMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String separatorString = getStringArgument(args, 0, this);
                String flagString = getOptionalStringArgument(args, 1, this);
                long flags = flagString != null ? RegexpHelper.parseFlagString(flagString) : 0;

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
                return stopIndex == -1 ? new SimpleString(s) : new SimpleString(s.substring(0, stopIndex));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new KeepUntilMethod(s);
        }
        
    }
    
    static class keep_before_lastBI extends BuiltInForString {

        class KeepUntilMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;

            KeepUntilMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String separatorString = getStringArgument(args, 0, this);
                String flagString = getOptionalStringArgument(args, 1, this);
                long flags = flagString != null ? RegexpHelper.parseFlagString(flagString) : 0;

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
                return stopIndex == -1 ? new SimpleString(s) : new SimpleString(s.substring(0, stopIndex));
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
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
            return new SimpleString(s.toLowerCase(env.getLocale()));
        }
    }    

    static class padBI extends BuiltInForString {
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            private final String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                int width = getNumberArgument(args, 0, this).intValue();

                String filling = getOptionalStringArgument(args, 1, this);
                if (filling != null) {
                    try {
                        return new SimpleString(
                                leftPadder
                                        ? _StringUtils.leftPad(s, width, filling)
                                        : _StringUtils.rightPad(s, width, filling));
                    } catch (IllegalArgumentException e) {
                        if (filling.length() == 0) {
                            throw new TemplateException(
                                    "?", key, "(...) argument #2 can't be a 0-length string.");
                        } else {
                            throw new TemplateException(e,
                                    "?", key, "(...) failed: ", e);
                        }
                    }
                } else {
                    return new SimpleString(leftPadder ? _StringUtils.leftPad(s, width) : _StringUtils.rightPad(s, width));
                }
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
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
        
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String prefix = getStringArgument(args, 0, this);
                return new SimpleString(s.startsWith(prefix) ? s.substring(prefix.length()) : s);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }

    static class remove_endingBI extends BuiltInForString {
    
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String suffix = getStringArgument(args, 0, this);
                return new SimpleString(s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
            }
        }
    
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new BIMethod(s);
        }
    }
    
    static class split_BI extends BuiltInForString {

        class SplitMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;

            SplitMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String splitString = getStringArgument(args, 0, this);
                TemplateModel arg2 = args[1];
                long flags = arg2 != null
                        ? RegexpHelper.parseFlagString(getStringArgument(args, 1, this))
                        : 0;
                String[] result;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkNonRegexpFlags(key, flags);
                    result = _StringUtils.split(s, splitString,
                            (flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) != 0);
                } else {
                    Pattern pattern = RegexpHelper.getPattern(splitString, (int) flags);
                    result = pattern.split(s);
                }
                return new NativeStringArraySequence(result);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new SplitMethod(s);
        }
        
    }
    
    static class starts_withBI extends BuiltInForString {
    
        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;
    
            private BIMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                return s.startsWith(getStringArgument(args, 0, this)) ?
                        TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.SINGLE_POSITIONAL_PARAMETER;
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
            return new MyTemplateFunctionModel(s);
        }

        private class MyTemplateFunctionModel extends BuiltInCallableImpl implements TemplateFunctionModel {

            private final String s;

            public MyTemplateFunctionModel(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                int beginIdx = getNumberArgument(args, 0, this).intValue();

                final int len = s.length();

                if (beginIdx < 0) {
                    throw newIndexLessThan0Exception(0, beginIdx);
                } else if (beginIdx > len) {
                    throw newIndexGreaterThanLengthException(0, beginIdx, len);
                }

                Number endIdxNumber = getOptionalNumberArgument(args, 1, this);
                if (endIdxNumber != null) {
                    int endIdx = endIdxNumber.intValue();
                    if (endIdx < 0) {
                        throw newIndexLessThan0Exception(1, endIdx);
                    } else if (endIdx > len) {
                        throw newIndexGreaterThanLengthException(1, endIdx, len);
                    }
                    if (beginIdx > endIdx) {
                        throw newGenericExecuteException(
                                "The begin index argument, " + beginIdx
                                + ", shouldn't be greater than the end index argument, " + endIdx + ".",
                                this);
                    }
                    return new SimpleString(s.substring(beginIdx, endIdx));
                } else {
                    return new SimpleString(s.substring(beginIdx));
                }
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }

            private TemplateException newIndexGreaterThanLengthException(
                    int argIdx, int idx, final int len) throws TemplateException {
                return newArgumentValueException(
                        argIdx,
                        "mustn't be greater than the length of the string, " + len + ", but it was " + idx + ".",
                        this);
            }

            private TemplateException newIndexLessThan0Exception(
                    int argIdx, int idx) throws TemplateException {
                return newArgumentValueException(
                        argIdx, "must be at least 0, but was " + idx + ".", this);
            }

        }
    }

    static class trimBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(s.trim());
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
            return new SimpleString(s);
        }
    }

    static class upper_caseBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            return new SimpleString(s.toUpperCase(env.getLocale()));
        }
    }

    static class word_listBI extends BuiltInForString {
        @Override
        TemplateModel calculateResult(String s, Environment env) {
            ArrayList<String> result = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(s);
            while (st.hasMoreTokens()) {
               result.add(st.nextToken());
            }
            return new  NativeStringListSequence(result);
        }
    }

    // Can't be instantiated
    private BuiltInsForStringsBasic() { }
    
}
