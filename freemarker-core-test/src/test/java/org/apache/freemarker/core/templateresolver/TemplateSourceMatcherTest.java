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
package org.apache.freemarker.core.templateresolver;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class TemplateSourceMatcherTest {
    
    @Test
    public void testPathGlobMatcher() throws IOException {
        PathGlobMatcher m = new PathGlobMatcher("**/a/?.f3ah");
        assertTrue(m.matches("a/b.f3ah", "dummy"));
        assertTrue(m.matches("x/a/c.f3ah", "dummy"));
        assertFalse(m.matches("a/b.F3ah", "dummy"));
        assertFalse(m.matches("b.f3ah", "dummy"));
        assertFalse(m.matches("a/bc.f3ah", "dummy"));
        
        m = new PathGlobMatcher("**/a/?.f3ah").caseInsensitive(true);
        assertTrue(m.matches("A/B.F3AH", "dummy"));
        m.setCaseInsensitive(false);
        assertFalse(m.matches("A/B.F3AH", "dummy"));
        
        try {
            new PathGlobMatcher("/b.f3ah");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testFileNameGlobMatcher() throws IOException {
        FileNameGlobMatcher m = new FileNameGlobMatcher("a*.f3ah");
        assertTrue(m.matches("ab.f3ah", "dummy"));
        assertTrue(m.matches("dir/ab.f3ah", "dummy"));
        assertTrue(m.matches("/dir/dir/ab.f3ah", "dummy"));
        assertFalse(m.matches("Ab.f3ah", "dummy"));
        assertFalse(m.matches("bb.f3ah", "dummy"));
        assertFalse(m.matches("ab.f3ah/x", "dummy"));

        m = new FileNameGlobMatcher("a*.f3ah").caseInsensitive(true);
        assertTrue(m.matches("AB.F3AH", "dummy"));
        m.setCaseInsensitive(false);
        assertFalse(m.matches("AB.F3AH", "dummy"));
        
        m = new FileNameGlobMatcher("\u00E1*.f3ah").caseInsensitive(true);
        assertTrue(m.matches("\u00C1b.f3ah", "dummy"));
        
        try {
            new FileNameGlobMatcher("dir/a*.f3ah");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testFileExtensionMatcher() throws IOException {
        FileExtensionMatcher m = new FileExtensionMatcher("f3ax");
        assertTrue(m.matches("a.f3ax", "dummy"));
        assertTrue(m.matches(".f3ax", "dummy"));
        assertTrue(m.matches("b/a.b.f3ax", "dummy"));
        assertTrue(m.matches("b/a.f3ax", "dummy"));
        assertTrue(m.matches("c.b/a.f3ax", "dummy"));
        assertFalse(m.matches("a.f3ah", "dummy"));
        assertFalse(m.matches("f3ax", "dummy"));
        assertFalse(m.matches("b.f3ax/a.f3ah", "dummy"));
        
        assertTrue(m.isCaseInsensitive());
        assertTrue(m.matches("a.f3aX", "dummy"));
        m.setCaseInsensitive(false);
        assertFalse(m.matches("a.f3aX", "dummy"));
        assertTrue(m.matches("A.f3ax", "dummy"));
        
        m = new FileExtensionMatcher("");
        assertTrue(m.matches("a.", "dummy"));
        assertTrue(m.matches(".", "dummy"));
        assertFalse(m.matches("a", "dummy"));
        assertFalse(m.matches("", "dummy"));
        assertFalse(m.matches("a.x", "dummy"));
        
        m = new FileExtensionMatcher("html.t");
        assertTrue(m.matches("a.html.t", "dummy"));
        assertFalse(m.matches("a.xhtml.t", "dummy"));
        assertFalse(m.matches("a.html", "dummy"));
        assertFalse(m.matches("a.t", "dummy"));
        
        try {
            new FileExtensionMatcher("*.f3ax");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new FileExtensionMatcher("f3a?");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new FileExtensionMatcher(".f3ax");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new FileExtensionMatcher("dir/a.f3ah");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testPathRegexMatcher() throws IOException {
        PathRegexMatcher m = new PathRegexMatcher("a/[a-z]+\\.f3ah");
        assertTrue(m.matches("a/b.f3ah", "dummy"));
        assertTrue(m.matches("a/abc.f3ah", "dummy"));
        assertFalse(m.matches("b.f3ah", "dummy"));
        assertFalse(m.matches("b/b.f3ah", "dummy"));
        
        try {
            new PathRegexMatcher("/b.f3ah");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testAndMatcher() throws IOException {
        AndMatcher m = new AndMatcher(new PathGlobMatcher("a*.*"), new PathGlobMatcher("*.t"));
        assertTrue(m.matches("ab.t", "dummy"));
        assertFalse(m.matches("bc.t", "dummy"));
        assertFalse(m.matches("ab.x", "dummy"));
        
        try {
            new AndMatcher();
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testOrMatcher() throws IOException {
        OrMatcher m = new OrMatcher(new PathGlobMatcher("a*.*"), new PathGlobMatcher("*.t"));
        assertTrue(m.matches("ab.t", "dummy"));
        assertTrue(m.matches("bc.t", "dummy"));
        assertTrue(m.matches("ab.x", "dummy"));
        assertFalse(m.matches("bc.x", "dummy"));
        
        try {
            new OrMatcher();
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testNotMatcher() throws IOException {
        NotMatcher m = new NotMatcher(new PathGlobMatcher("a*.*"));
        assertFalse(m.matches("ab.t", "dummy"));
        assertTrue(m.matches("bc.t", "dummy"));
    }
    
}
