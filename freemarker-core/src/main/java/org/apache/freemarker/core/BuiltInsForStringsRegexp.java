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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SequenceTemplateModelIterator;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util._StringUtils;


/**
 * Contains the string built-ins that correspond to basic regular expressions operations.
 */
class BuiltInsForStringsRegexp {

    static class groupsBI extends ASTExpBuiltIn {
        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel targetModel = target.eval(env);
            assertNonNull(targetModel, env);
            if (targetModel instanceof RegexMatchModel) {
                return ((RegexMatchModel) targetModel).getGroups();
            } else if (targetModel instanceof RegexMatchModel.MatchWithGroups) {
                return new NativeStringArraySequence(((RegexMatchModel.MatchWithGroups) targetModel).groups);

            } else {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, targetModel,
                        "regular expression matcher",
                        new Class[] { RegexMatchModel.class, RegexMatchModel.MatchWithGroups.class },
                        null,
                        env);
            }
        }
    }
    
    static class matchesBI extends BuiltInForString {
        class MatcherBuilder extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            String matchString;
            
            MatcherBuilder(String matchString) throws TemplateException {
                this.matchString = matchString;
            }


            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String patternString = getStringArgument(args, 0, this);
                String flagString = getOptionalStringArgument(args, 1, this);
                long flags = flagString != null
                        ? RegexpHelper.parseFlagString(flagString)
                        : 0;
                if ((flags & RegexpHelper.RE_FLAG_FIRST_ONLY) != 0) {
                    // TODO [FM3] Should be an error?
                    RegexpHelper.logFlagWarning("?" + key + " doesn't support the \"f\" flag.");
                }
                Pattern pattern = RegexpHelper.getPattern(patternString, (int) flags);
                return new RegexMatchModel(pattern, matchString);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.TWO_POSITIONAL_PARAMETERS;
            }
        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new MatcherBuilder(s);
        }
        
    }
    
    static class replace_reBI extends BuiltInForString {
        
        class ReplaceMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            private String s;

            ReplaceMethod(String s) {
                this.s = s;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                String arg1 = getStringArgument(args, 0, this);
                String arg2 = getStringArgument(args, 1, this);
                String flagString = getOptionalStringArgument(args, 2, this);
                long flags = flagString != null
                        ? RegexpHelper.parseFlagString(flagString)
                        : 0;
                String result;
                if ((flags & RegexpHelper.RE_FLAG_REGEXP) == 0) {
                    RegexpHelper.checkNonRegexpFlags("replace", flags);
                    result = _StringUtils.replace(s, arg1, arg2,
                            (flags & RegexpHelper.RE_FLAG_CASE_INSENSITIVE) != 0,
                            (flags & RegexpHelper.RE_FLAG_FIRST_ONLY) != 0);
                } else {
                    Pattern pattern = RegexpHelper.getPattern(arg1, (int) flags);
                    Matcher matcher = pattern.matcher(s);
                    result = (flags & RegexpHelper.RE_FLAG_FIRST_ONLY) != 0
                            ? matcher.replaceFirst(arg2)
                            : matcher.replaceAll(arg2);
                }
                return new SimpleString(result);
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return ArgumentArrayLayout.THREE_POSITIONAL_PARAMETERS;
            }

        }
        
        @Override
        TemplateModel calculateResult(String s, Environment env) throws TemplateException {
            return new ReplaceMethod(s);
        }
        
    }
    
    // Represents the match
  
    static class RegexMatchModel implements TemplateBooleanModel, TemplateSequenceModel {
        static class MatchWithGroups implements TemplateStringModel {
            final String matchedInputPart;
            final String[] groups;

            MatchWithGroups(String input, Matcher matcher) {
                matchedInputPart = input.substring(matcher.start(), matcher.end());
                final int grpCount = matcher.groupCount() + 1;
                groups = new String[grpCount];
                for (int i = 0; i < grpCount; i++) {
                    groups[i] = matcher.group(i);
                }
            }
            
            @Override
            public String getAsString() {
                return matchedInputPart;
            }
        }

        final Pattern pattern;
        final String input;

        private Matcher firedEntireInputMatcher;
        private Boolean entireInputMatched;
        private TemplateSequenceModel entireInputMatchGroups;
        private ArrayList<TemplateModel> matchingInputParts;
        
        RegexMatchModel(Pattern pattern, String input) {
            this.pattern = pattern;
            this.input = input;
        }
        
        @Override
        public TemplateModel get(int i) throws TemplateException {
            if (i < 0) {
                return  null;
            }
            ArrayList<TemplateModel> matchingInputParts = this.matchingInputParts;
            if (matchingInputParts == null) {
                matchingInputParts = getMatchingInputPartsAndStoreResults();
            }
            return i < matchingInputParts.size() ? matchingInputParts.get(i) : null;
        }

        @Override
        public int getCollectionSize() throws TemplateException {
            ArrayList<TemplateModel> matchingInputParts = this.matchingInputParts;
            if (matchingInputParts == null) {
                matchingInputParts = getMatchingInputPartsAndStoreResults();
            }
            return matchingInputParts.size();
        }

        @Override
        public boolean isEmptyCollection() throws TemplateException {
            return getCollectionSize() == 0;
        }

        @Override
        public boolean getAsBoolean() {
            Boolean result = entireInputMatched;
            return result != null ? result : isEntireInputMatchesAndStoreResults();
        }
        
        TemplateModel getGroups() {
           TemplateSequenceModel entireInputMatchGroups = this.entireInputMatchGroups;
           if (entireInputMatchGroups == null) {
               Matcher t = firedEntireInputMatcher;
               if (t == null) {
                   isEntireInputMatchesAndStoreResults();
                   t = firedEntireInputMatcher;
               }
               final Matcher firedEntireInputMatcher = t;
               
                entireInputMatchGroups = new TemplateSequenceModel() {
                    
                    @Override
                    public TemplateModel get(int i) throws TemplateException {
                        try {
                            if (i < 0 || i > firedEntireInputMatcher.groupCount()) {
                                return null;
                            }

                            return new SimpleString(firedEntireInputMatcher.group(i));
                        } catch (Exception e) {
                            throw new TemplateException("Failed to read regular expression match group", e);
                        }
                    }
                    
                    @Override
                    public int getCollectionSize() throws TemplateException {
                        return firedEntireInputMatcher.groupCount() + 1;
                    }

                    @Override
                    public boolean isEmptyCollection() throws TemplateException {
                        return getCollectionSize() == 0;
                    }

                    @Override
                    public TemplateModelIterator iterator() throws TemplateException {
                        return new SequenceTemplateModelIterator(this);
                    }
                };
                this.entireInputMatchGroups = entireInputMatchGroups;
            }
            return entireInputMatchGroups;
        }
        
        private ArrayList<TemplateModel> getMatchingInputPartsAndStoreResults() throws TemplateException {
            ArrayList<TemplateModel> matchingInputParts = new ArrayList<>();
            
            Matcher matcher = pattern.matcher(input);
            while (matcher.find()) {
                matchingInputParts.add(new MatchWithGroups(input, matcher));
            }
    
            this.matchingInputParts = matchingInputParts;
            return matchingInputParts;
        }
        
        private boolean isEntireInputMatchesAndStoreResults() {
            Matcher matcher = pattern.matcher(input);
            boolean matches = matcher.matches();
            firedEntireInputMatcher = matcher;
            entireInputMatched = matches;
            return matches;
        }
        
        @Override
        public TemplateModelIterator iterator() {
            final ArrayList<TemplateModel> matchingInputParts = this.matchingInputParts;
            if (matchingInputParts == null) {
                final Matcher matcher = pattern.matcher(input);
                return new TemplateModelIterator() {
                    
                    private int nextIdx = 0;
                    boolean hasFindInfo = matcher.find();
                    
                    @Override
                    public boolean hasNext() {
                        final ArrayList<TemplateModel> matchingInputParts = RegexMatchModel.this.matchingInputParts;
                        if (matchingInputParts == null) {
                            return hasFindInfo;
                        } else {
                            return nextIdx < matchingInputParts.size();
                        }
                    }
                    
                    @Override
                    public TemplateModel next() throws TemplateException {
                        final ArrayList<TemplateModel> matchingInputParts = RegexMatchModel.this.matchingInputParts;
                        if (matchingInputParts == null) {
                            if (!hasFindInfo) {
                                throw new TemplateException("There were no more regular expression matches");
                            }
                            MatchWithGroups result = new MatchWithGroups(input, matcher);
                            nextIdx++;
                            hasFindInfo = matcher.find();
                            return result;
                        } else {
                            return matchingInputParts.get(nextIdx++);
                        }
                    }
                    
                };
            } else {
                return new TemplateModelIterator() {
                    
                    private int nextIdx = 0;
                    
                    @Override
                    public boolean hasNext() {
                        return nextIdx < matchingInputParts.size();
                    }
                    
                    @Override
                    public TemplateModel next() throws TemplateException {
                        return matchingInputParts.get(nextIdx++);
                    }
                };
            }
        }
        
    }

    // Can't be instantiated
    private BuiltInsForStringsRegexp() { }
    
}
