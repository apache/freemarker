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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import freemarker.cache.MruCacheStorage;
import freemarker.core.StringBuiltins.StringBuiltIn;
import freemarker.log.Logger;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.StringUtil;


/**
 * Contains the string built-ins that deal with regular expressions.
 */
// TODO: I think that the implementator has neglected that Matcher-s are
// stateful and hence the result of ?matches can produce glitches if it's
// accessed while iterated through. (This is a single-thread issue.)
class RegexBuiltins {

    // Can't be instantiated
    private RegexBuiltins() { }
    
    private static final Logger logger = Logger.getLogger("freemarker.runtime");
    
    private static volatile boolean flagWarningsEnabled = logger.isWarnEnabled();
    private static final int MAX_FLAG_WARNINGS_LOGGED = 25;
    private static final Object flagWarningsCntSync = new Object();
    private static int flagWarningsCnt;
    
    static final MruCacheStorage patternCache = new MruCacheStorage(50, 150);

    // Standard regular expression flags converted to long:
    private static final long RE_FLAG_CASE_INSENSITIVE = intFlagToLong(Pattern.CASE_INSENSITIVE);
    private static final long RE_FLAG_MULTILINE = intFlagToLong(Pattern.MULTILINE);
    private static final long RE_FLAG_COMMENTS = intFlagToLong(Pattern.COMMENTS);
    private static final long RE_FLAG_DOTALL = intFlagToLong(Pattern.DOTALL);
    
    // FreeMarker-specific regular expression flags (using the higher 32 bits):
    private static final long RE_FLAG_REGEXP = 0x100000000L;
    private static final long RE_FLAG_FIRST_ONLY = 0x200000000L;
    
    static private long intFlagToLong(int flag) {
        return flag & 0x0000FFFFL;
    }
    
    static Pattern getPattern(String patternString, int flags)
    throws TemplateModelException {
        PatternCacheKey patternKey = new PatternCacheKey(patternString, flags);
        
        Pattern result;
        
        synchronized (patternCache) {
            result = (Pattern) patternCache.get(patternKey);
        }
        if (result != null) {
            return result;
        }
        
        try {
            result = Pattern.compile(patternString, flags);
        } catch (PatternSyntaxException e) {
            throw new _TemplateModelException(e, new Object[] {
                    "Malformed regular expression: ", new _DelayedGetMessage(e) });
        }
        synchronized (patternCache) {
            patternCache.put(patternKey, result);
        }
        return result;
    }
    
    private static class PatternCacheKey {
        private final String patternString;
        private final int flags;
        private final int hashCode;
        
        public PatternCacheKey(String patternString, int flags) {
            this.patternString = patternString;
            this.flags = flags;
            hashCode = patternString.hashCode() + 31 * flags;
        }
        
        public boolean equals(Object that) {
            if (that instanceof PatternCacheKey) {
                PatternCacheKey thatPCK = (PatternCacheKey) that; 
                return thatPCK.flags == flags
                        && thatPCK.patternString.equals(patternString);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return hashCode;
        }
        
    }
    
    private static long parseFlagString(String flagString) {
        long flags = 0;
        for (int i = 0; i < flagString.length(); i++) {
            char c = flagString.charAt(i);
            switch (c) {
                case 'i':
                    flags |= RE_FLAG_CASE_INSENSITIVE;
                    break;
                case 'm':
                    flags |= RE_FLAG_MULTILINE;
                    break;
                case 'c':
                    flags |= RE_FLAG_COMMENTS;
                    break;
                case 's':
                    flags |= RE_FLAG_DOTALL;
                    break;
                case 'r':
                    flags |= RE_FLAG_REGEXP;
                    break;
                case 'f':
                    flags |= RE_FLAG_FIRST_ONLY;
                    break;
                default:
                    if (flagWarningsEnabled) {
                        logFlagWarning(
                                "Unrecognized regular expression flag: "
                                + StringUtil.jQuote(String.valueOf(c)) + ".");
                    }
            }  // switch
        }
        return flags;
    }

    /**
     * Logs flag warning for a limited number of times. This is used to prevent
     * log flooding.
     */
    private static void logFlagWarning(String message) {
        if (!flagWarningsEnabled) return;
        
        int cnt;
        synchronized (flagWarningsCntSync) {
            cnt = flagWarningsCnt;
            if (cnt < MAX_FLAG_WARNINGS_LOGGED) {
                flagWarningsCnt++;
            } else {
                flagWarningsEnabled = false;
                return;
            }
        }
        message += " This will be an error in FreeMarker 2.4!";
        if (cnt + 1 == MAX_FLAG_WARNINGS_LOGGED) {
            message += " [Will not log more regular expression flag problems until restart!]";
        }
        logger.warn(message);
    }
    
    public static class matchesBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new MatcherBuilder(s);
        }
        
        class MatcherBuilder implements TemplateMethodModel {
            
            String matchString;
            
            MatcherBuilder(String matchString) throws TemplateModelException {
                this.matchString = matchString;
            }
            
            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                
                String patternString = (String) args.get(0);
                long flags = argCnt > 1 ? parseFlagString((String) args.get(1)) : 0;
                if ((flags & RE_FLAG_FIRST_ONLY) != 0) {
                    logFlagWarning("?" + key + " doesn't support the \"f\" flag.");
                }
                Pattern pattern = getPattern(patternString, (int) flags);
                return new RegexMatchModel(pattern, matchString);
            }
        }
        
    }
    
    public static class groupsBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel targetModel = target.eval(env);
            assertNonNull(targetModel, env);
            if (targetModel instanceof RegexMatchModel) {
                return ((RegexMatchModel) targetModel).getGroups();
            } else if (targetModel instanceof RegexMatchModel.MatchWithGroups) {
                return ((RegexMatchModel.MatchWithGroups) targetModel).groupsSeq;
            } else {
                throw new UnexpectedTypeException(target, targetModel,
                        "regular expression matcher",
                        new Class[] { RegexMatchModel.class, RegexMatchModel.MatchWithGroups.class },
                        env);
            }
        }
    }
    
    public static class replace_reBI extends StringBuiltIn {
        
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new ReplaceMethod(s);
        }
        
        class ReplaceMethod implements TemplateMethodModel {
            private String s;

            ReplaceMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 2, 3);
                String arg1 = (String) args.get(0);
                String arg2 = (String) args.get(1);
                long flags = argCnt > 2 ? parseFlagString((String) args.get(2)) : 0;
                String result;
                if ((flags & RE_FLAG_REGEXP) == 0) {
                    checkNonRegexpFlags("replace", flags);
                    result = StringUtil.replace(s, arg1, arg2,
                            (flags & RE_FLAG_CASE_INSENSITIVE) != 0,
                            (flags & RE_FLAG_FIRST_ONLY) != 0);
                } else {
                    Pattern pattern = getPattern(arg1, (int) flags);
                    Matcher matcher = pattern.matcher(s);
                    result = (flags & RE_FLAG_FIRST_ONLY) != 0
                            ? matcher.replaceFirst(arg2)
                            : matcher.replaceAll(arg2);
                } 
                return new SimpleScalar(result);
            }

        }
        
    }
    
    public static class split_reBI extends StringBuiltIn {
        TemplateModel calculateResult(String s, Environment env) throws TemplateModelException {
            return new SplitMethod(s);
        }
        
        class SplitMethod implements TemplateMethodModel {
            private String s;

            SplitMethod(String s) {
                this.s = s;
            }

            public Object exec(List args) throws TemplateModelException {
                int argCnt = args.size();
                checkMethodArgCount(argCnt, 1, 2);
                String splitString = (String) args.get(0);
                long flags = argCnt > 1 ? parseFlagString((String) args.get(1)) : 0;
                String[] result = null;
                if ((flags & RE_FLAG_REGEXP) == 0) {
                    checkNonRegexpFlags("split", flags);
                    result = StringUtil.split(s, splitString,
                            (flags & RE_FLAG_CASE_INSENSITIVE) != 0);
                } else {
                    Pattern pattern = getPattern(splitString, (int) flags);
                    result = pattern.split(s);
                } 
                return ObjectWrapper.DEFAULT_WRAPPER.wrap(result);
            }
        }
        
    }
    
    // Represents the match
  
    static class RegexMatchModel 
    implements TemplateBooleanModel, TemplateCollectionModel, TemplateSequenceModel {
        final Pattern pattern;
        final String input;
        
        private Matcher firedEntireInputMatcher;
        private Boolean entireInputMatched;
        private TemplateSequenceModel entireInputMatchGroups;
        
        private ArrayList matchingInputParts;
        
        RegexMatchModel(Pattern pattern, String input) {
            this.pattern = pattern;
            this.input = input;
        }
        
        public boolean getAsBoolean() {
            Boolean result = entireInputMatched;
            return result != null ? result.booleanValue() : isEntrieInputMatchesAndStoreResults();
        }
        
        private boolean isEntrieInputMatchesAndStoreResults() {
            Matcher matcher = pattern.matcher(input);
            boolean matches = matcher.matches();
            firedEntireInputMatcher = matcher;
            entireInputMatched = Boolean.valueOf(matches);
            return matches;
        }
        
        public TemplateModel get(int i) throws TemplateModelException {
            ArrayList matchingInputParts = this.matchingInputParts;
            if (matchingInputParts == null) {
                matchingInputParts = getMatchingInputPartsAndStoreResults();
            }
            return (TemplateModel) matchingInputParts.get(i);
        }
        
        public int size() throws TemplateModelException {
            ArrayList matchingInputParts = this.matchingInputParts;
            if (matchingInputParts == null) {
                matchingInputParts = getMatchingInputPartsAndStoreResults();
            }
            return matchingInputParts.size();
        }
        
        private ArrayList getMatchingInputPartsAndStoreResults() throws TemplateModelException {
            ArrayList matchingInputParts = new ArrayList();
            
            Matcher matcher = pattern.matcher(input);
            while (matcher.find()) {
                matchingInputParts.add(new MatchWithGroups(input, matcher));
            }

            this.matchingInputParts = matchingInputParts;
            return matchingInputParts;
        }
        
        public TemplateModel getGroups() {
           TemplateSequenceModel entireInputMatchGroups = this.entireInputMatchGroups;
           if (entireInputMatchGroups == null) {
               Matcher t = this.firedEntireInputMatcher;
               if (t == null) {
                   isEntrieInputMatchesAndStoreResults();
                   t = this.firedEntireInputMatcher;
               }
               final Matcher firedEntireInputMatcher = t;
               
                entireInputMatchGroups = new TemplateSequenceModel() {
                    
                    public int size() throws TemplateModelException {
                        try {
                            return firedEntireInputMatcher.groupCount() + 1;
                        }
                        catch (Exception e) {
                            throw new _TemplateModelException(e, "Failed to get match group count");
                        }
                    }
                    
                    public TemplateModel get(int i) throws TemplateModelException {
                        try {
                            return new SimpleScalar(firedEntireInputMatcher.group(i));
                        } catch (Exception e) {
                            throw new _TemplateModelException(e, "Failed to read match group");
                        }
                    }
                    
                };
                this.entireInputMatchGroups = entireInputMatchGroups;
            }
            return entireInputMatchGroups;
        }
        
        public TemplateModelIterator iterator() {
            final ArrayList matchingInputParts = this.matchingInputParts;
            if (matchingInputParts == null) {
                final Matcher matcher = pattern.matcher(input);
                return new TemplateModelIterator() {
                    
                    private int nextIdx = 0;
                    boolean hasFindInfo = matcher.find();
                    
                    public boolean hasNext() {
                        final ArrayList matchingInputParts = RegexMatchModel.this.matchingInputParts;
                        if (matchingInputParts == null) {
                            return hasFindInfo;
                        } else {
                            return nextIdx < matchingInputParts.size();
                        }
                    }
                    
                    public TemplateModel next() throws TemplateModelException {
                        final ArrayList matchingInputParts = RegexMatchModel.this.matchingInputParts;
                        if (matchingInputParts == null) {
                            if (!hasFindInfo) throw new _TemplateModelException("There were no more matches");
                            MatchWithGroups result = new MatchWithGroups(input, matcher);
                            nextIdx++;
                            hasFindInfo = matcher.find();
                            return result;
                        } else {
                            try {
                                return (TemplateModel) matchingInputParts.get(nextIdx++);
                            } catch (IndexOutOfBoundsException e) {
                                throw new _TemplateModelException(e, "There were no more matches");
                            }
                        }
                    }
                    
                };
            } else {
                return new TemplateModelIterator() {
                    
                    private int nextIdx = 0;
                    
                    public boolean hasNext() {
                        return nextIdx < matchingInputParts.size();
                    }
                    
                    public TemplateModel next() throws TemplateModelException {
                        try {
                            return (TemplateModel) matchingInputParts.get(nextIdx++);
                        } catch (IndexOutOfBoundsException e) {
                            throw new _TemplateModelException(e, "There were no more matches");
                        }
                    }
                };
            }
        }
        
        static class MatchWithGroups implements TemplateScalarModel {
            final String matchedInputPart;
            final SimpleSequence groupsSeq;
            
            MatchWithGroups(String input, Matcher matcher) {
                matchedInputPart = input.substring(matcher.start(), matcher.end());
                final int grpCount = matcher.groupCount() + 1;
                groupsSeq = new SimpleSequence(grpCount);
                for (int i = 0; i < grpCount; i++) {
                    groupsSeq.add(matcher.group(i));
                }
            }
            
            public String getAsString() {
                return matchedInputPart;
            }
        }
    }
    
    private static void checkNonRegexpFlags(String biName, long flags) {
        if (!flagWarningsEnabled) return;
        
        if ((flags & RE_FLAG_MULTILINE) != 0) {
            logFlagWarning("?" + biName + " doesn't support the \"m\" flag "
                    + "without the \"r\" flag.");
        }
        if ((flags & RE_FLAG_DOTALL) != 0) {
            logFlagWarning("?" + biName + " doesn't support the \"s\" flag "
                    + "without the \"r\" flag.");
        }
        if ((flags & RE_FLAG_COMMENTS) != 0) {
            logFlagWarning("?" + biName + " doesn't support the \"c\" flag "
                    + "without the \"r\" flag.");
        }
    }
    
}
