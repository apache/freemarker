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
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 * 
 * Contains the string built-ins that require J2SE 1.4 regular expression classes.
 */
// TODO: I think that the implementator has neglected that Matcher-s are
// stateful and hence the result of ?matches can produce glitches if it's
// accessed while iterated through. (This is a single-thread issue.)
public class _RegexBuiltins {

    // Can't be instantiated
    private _RegexBuiltins() { }
    
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
                Matcher matcher = pattern.matcher(matchString);
                return new RegexMatchModel(matcher, matchString);
            }
        }
        
    }
    
    public static class groupsBI extends BuiltIn {
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel targetModel = target.eval(env);
            assertNonNull(targetModel, env);
            if (targetModel instanceof RegexMatchModel) {
                return ((RegexMatchModel) targetModel).getGroups();
            } else if (targetModel instanceof RegexMatchModel.Match) {
                return ((RegexMatchModel.Match) targetModel).subs;
            } else {
                throw new UnexpectedTypeException(target, targetModel, "regular expression matcher", env);
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
        final Matcher matcher;
        final String input;
        final boolean matches;
        TemplateSequenceModel groups;
        private ArrayList data;
        
        RegexMatchModel(Matcher matcher, String input) {
            this.matcher = matcher;
            this.input = input;
            this.matches = matcher.matches();
        }
        
        public boolean getAsBoolean() {
            return matches;
        }
        
        public TemplateModel get(int i) throws TemplateModelException {
            if (data == null) initSequence();
            return (TemplateModel) data.get(i);
        }
        
        public int size() throws TemplateModelException {
            if (data == null) initSequence();
            return data.size();
        }
        
        private void initSequence() throws TemplateModelException {
            data = new ArrayList();
            TemplateModelIterator it = iterator();
            while (it.hasNext()) {
                data.add(it.next());
            }
        }
        
        public TemplateModel getGroups() {
           if (groups == null) {
                groups = new TemplateSequenceModel() {
                    public int size() throws TemplateModelException {
                        try {
                            return matcher.groupCount() + 1;
                        }
                        catch (Exception e) {
                            throw new _TemplateModelException(e);
                        }
                    }
                    public TemplateModel get(int i) throws TemplateModelException {
                        try {
                            return new SimpleScalar(matcher.group(i));
                        }
                        catch (Exception e) {
                            throw new _TemplateModelException(e);
                        }
                    }
                };
            }
            return groups;
        }
        
        public TemplateModelIterator iterator() {
            matcher.reset();
            return new TemplateModelIterator() {
                boolean hasFindInfo = matcher.find();
                
                public boolean hasNext() {
                    return hasFindInfo;
                }
                
                public TemplateModel next() throws TemplateModelException {
                    if (!hasNext()) throw new _TemplateModelException("No more matches");
                    Match result = new Match();
                    hasFindInfo = matcher.find();
                    return result;
                }
            };
        }
        
        class Match implements TemplateScalarModel {
            String match;
            SimpleSequence subs = new SimpleSequence();
            Match() {
                match = input.substring(matcher.start(), matcher.end());
                for (int i=0; i< matcher.groupCount() + 1; i++) {
                    subs.add(matcher.group(i));
                }
            }
            public String getAsString() {
                return match;
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
