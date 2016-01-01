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

public final class TestUtil {
    
    private TestUtil() {
        // Not meant to be instantiated
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

}
