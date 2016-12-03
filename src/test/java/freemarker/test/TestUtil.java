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
package freemarker.test;

import freemarker.template.Configuration;
import freemarker.template.Version;

public final class TestUtil {
    
    private TestUtil() {
        // Not meant to be instantiated
    }

    public static String removeTxtCopyrightComment(String s) {
        if (!s.startsWith("/*")) {
            return s;
        }
        
        int commentEnd = s.indexOf("*/");
        if (commentEnd == -1) {
            return s;
        }
        commentEnd += 2;
        if (commentEnd < s.length()) {
            char c = s.charAt(commentEnd);
            if (c == '\n' || c == '\r') {
                commentEnd++;
                if (c == '\r' && commentEnd < s.length()) {
                    if (s.charAt(commentEnd) == '\n') {
                        commentEnd++;
                    }
                }
            }
        }
        
        String comment = s.substring(0, commentEnd);
        int copyrightIdx = comment.indexOf("copyright");
        if (copyrightIdx == -1) {
            copyrightIdx = comment.indexOf("Copyright");
        }
        if (copyrightIdx == -1) {
            return s;
        }
        
        return s.substring(commentEnd);
    }
    
    public static String removeFTLCopyrightComment(String ftl) {
        if (ftl.contains("<#ftl ns_prefixes = {\"D\" : \"http://example.com/eBook\"}>")) {
            System.out.println();
        }
        
        int copyrightIdx = ftl.indexOf("copyright");
        if (copyrightIdx == -1) {
            copyrightIdx = ftl.indexOf("Copyright");
        }
        if (copyrightIdx == -1) {
            return ftl;
        }
        
        final int commentFirstIdx;
        final boolean squareBracketTagSyntax;
        {
            String ftlBeforeCopyright = ftl.substring(0, copyrightIdx);
            int abCommentStart = ftlBeforeCopyright.lastIndexOf("<#--");
            int sbCommentStart = ftlBeforeCopyright.lastIndexOf("[#--");
            squareBracketTagSyntax = sbCommentStart > abCommentStart;
            commentFirstIdx = squareBracketTagSyntax ? sbCommentStart : abCommentStart;
            if (commentFirstIdx == -1) {
                throw new AssertionError("Can't find copyright comment start");
            }
        }
        
        final int commentLastIdx;
        {
            int commentEndStart = ftl.indexOf(squareBracketTagSyntax ? "--]" : "-->", copyrightIdx);
            if (commentEndStart == -1) {
                throw new AssertionError("Can't find copyright comment end");
            }
            commentLastIdx = commentEndStart + 2;
        }
        
        final int afterCommentNLChars;
        if (commentLastIdx + 1 < ftl.length()) {
            char afterCommentChar = ftl.charAt(commentLastIdx + 1);
            if (afterCommentChar == '\n' || afterCommentChar == '\r') {
                if (afterCommentChar == '\r' && commentLastIdx + 2 < ftl.length() && ftl.charAt(commentLastIdx + 2) == '\n') {
                    afterCommentNLChars = 2;
                } else {
                    afterCommentNLChars = 1;
                }
            } else {
                afterCommentNLChars = 0;
            }
        } else {
            afterCommentNLChars = 0;
        }
            
        return ftl.substring(0, commentFirstIdx) + ftl.substring(commentLastIdx + afterCommentNLChars + 1);
    }

    /**
     * Returns the closes FreeMarker version number that doesn't exit yet (so it's illegal).
     */
    public static Version getClosestFutureVersion() {
        Version v = Configuration.getVersion();
        return new Version(v.getMajor(), v.getMinor(), v.getMicro() + 1);
    }

}
