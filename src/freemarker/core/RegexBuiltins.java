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

import java.util.*;
import java.util.regex.*;
import freemarker.template.*;
import freemarker.template.utility.StringUtil;


/**
 * This is a first-pass implementation of regular expression support.
 * It is subject to change based on community feedback. 
 * In that sense, use it at your own risk.
 * @version $Id: RegexBuiltins.java,v 1.14 2005/06/01 22:39:07 ddekany Exp $
 * @author Jonathan Revusky
 */
abstract class RegexBuiltins {
    
    static HashMap patternLookup = new HashMap();
    static LinkedList patterns = new LinkedList();
    static final int PATTERN_CACHE_SIZE=100;
    
    static Pattern getPattern(String patternString, String flagString) throws TemplateModelException {
        int flags = 0;
        String patternKey = patternString + (char) 0 + flagString;
        Pattern result = (Pattern) patternLookup.get(patternKey);
        if (result != null) {
            return result;
        }
        if (flagString == null || flagString.length() == 0) {
            try {
                result = Pattern.compile(patternString);
            } catch (PatternSyntaxException e) {
                throw new TemplateModelException(e);
            }
        }
        else {
            if (flagString.indexOf('i') >=0) {
                flags = flags | Pattern.CASE_INSENSITIVE;
            }
            if (flagString.indexOf('m') >=0) {
                flags = flags | Pattern.MULTILINE;
            }
            if (flagString.indexOf('c') >=0) {
                flags = flags | Pattern.COMMENTS;
            }
            if (flagString.indexOf('s') >=0) {
                flags = flags | Pattern.DOTALL;
            }
            try {
                result = Pattern.compile(patternString, flags);
            } catch (PatternSyntaxException e) {
                throw new TemplateModelException(e);
            }
        }
        patterns.add(patternKey);
        patternLookup.put(patternKey, result);
        if (patterns.size() > PATTERN_CACHE_SIZE) {
            Object first = patterns.removeFirst();
            patterns.remove(first);
        }
        return result;
    }
    
    static class matchesBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel targetModel = target.getAsTemplateModel(env);
            assertNonNull(targetModel, this, env);
            if (!(targetModel instanceof TemplateScalarModel)) {
                throw invalidTypeException(targetModel, target, env, "string");
            }
            return new MatcherBuilder((TemplateScalarModel) targetModel);
        }
    }
    
    static class groupsBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env) throws TemplateException {
            TemplateModel targetModel = target.getAsTemplateModel(env);
            assertNonNull(targetModel, this, env);
            if (targetModel instanceof RegexMatchModel) {
                return ((RegexMatchModel) targetModel).getGroups();
            }
            if (targetModel instanceof RegexMatchModel.Match) {
                return ((RegexMatchModel.Match) targetModel).subs;
            }
            throw invalidTypeException(targetModel, target, env, "a regular expression matcher");
        }
    }
    
    
    static class replace_reBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new ReplaceMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }
    }
    
    static class split_reBI extends BuiltIn {
        TemplateModel _getAsTemplateModel(Environment env)
                throws TemplateException {
            TemplateModel model = target.getAsTemplateModel(env);
            if (model instanceof TemplateScalarModel) {
                return new SplitMethod(((TemplateScalarModel) model).getAsString());
            }
            throw invalidTypeException(model, target, env, "string");
        }
    }
    
    // Represents the match
  
    static class RegexMatchModel 
    implements TemplateBooleanModel, TemplateCollectionModel, TemplateSequenceModel {
        Matcher matcher;
        String input;
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
                            throw new TemplateModelException(e);
                        }
                    }
                    public TemplateModel get(int i) throws TemplateModelException {
                        try {
                            return new SimpleScalar(matcher.group(i));
                        }
                        catch (Exception e) {
                            throw new TemplateModelException(e);
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
                    if (!hasNext()) throw new TemplateModelException("No more matches");
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
    
    static class MatcherBuilder implements TemplateMethodModel {
        
        String matchString;
        
        MatcherBuilder(TemplateScalarModel match) throws TemplateModelException {
            this.matchString = match.getAsString();
        }
        
        public Object exec(List args) throws TemplateModelException {
            int numArgs = args.size();
            if (numArgs == 0) {
                throw new TemplateModelException("Expecting at least one argument");
            }
            if (numArgs > 2) {
                throw new TemplateModelException("Expecting at most two argumnets");
            }
            String patternString = (String) args.get(0);
            String flagString = (numArgs >1) ? (String) args.get(1) : "";
            Pattern pattern = getPattern(patternString, flagString);
            Matcher matcher = pattern.matcher(matchString);
            return new RegexMatchModel(matcher, matchString);
        }
    }
    
    static class ReplaceMethod implements TemplateMethodModel {
        private String s;

        ReplaceMethod(String s) {
            this.s = s;
        }

        public Object exec(List args) throws TemplateModelException {
            int numArgs = args.size();
            if (numArgs < 2 || numArgs >3 ) {
                throw new TemplateModelException(
                        "?replace(...) needs 2 or 3 arguments.");
            }
            String first = (String) args.get(0);
            String second = (String) args.get(1);
            String flags = numArgs >2 ? (String) args.get(2) : "";
            boolean caseInsensitive = flags.indexOf('i') >=0;
            boolean useRegexp = flags.indexOf('r') >=0;
            boolean firstOnly = flags.indexOf('f') >=0;
            String result = null;
            if (!useRegexp) {
                result = StringUtil.replace(s, first, second, caseInsensitive, firstOnly);
            } else {
                Pattern pattern = getPattern(first, flags);
                Matcher matcher = pattern.matcher(s);
                result = firstOnly ? matcher.replaceFirst(second) : matcher.replaceAll(second);
            } 
            return new SimpleScalar(result);
        }
    }
    
    static class SplitMethod implements TemplateMethodModel {
        private String s;

        SplitMethod(String s) {
            this.s = s;
        }

        public Object exec(List args) throws TemplateModelException {
            int numArgs = args.size();
            if (numArgs < 1 || numArgs >2 ) {
                throw new TemplateModelException(
                        "?replace(...) needs 1 or 2 arguments.");
            }
            String splitString = (String) args.get(0);
            String flags = numArgs >1 ? (String) args.get(1) : "";
            boolean caseInsensitive = flags.indexOf('i') >=0;
            boolean useRegexp = flags.indexOf('r') >=0;
            String[] result = null;
            if (!useRegexp) {
                result = StringUtil.split(s, splitString, caseInsensitive);
            } else {
                Pattern pattern = getPattern(splitString, flags);
                result = pattern.split(s);
            } 
            return ObjectWrapper.DEFAULT_WRAPPER.wrap(result);
        }
    }
}
