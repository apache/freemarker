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
package freemarker.cache;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class TemplateSourceMatcherTest {
    
    @Test
    public void testPathGlobMatcher() throws IOException {
        PathGlobMatcher m = new PathGlobMatcher("**/a/?.ftl");
        assertTrue(m.matches("a/b.ftl", "dummy"));
        assertTrue(m.matches("x/a/c.ftl", "dummy"));
        assertFalse(m.matches("a/b.Ftl", "dummy"));
        assertFalse(m.matches("b.ftl", "dummy"));
        assertFalse(m.matches("a/bc.ftl", "dummy"));
        
        m = new PathGlobMatcher("**/a/?.ftl").caseInsensitive(true);
        assertTrue(m.matches("A/B.FTL", "dummy"));
        m.setCaseInsensitive(false);
        assertFalse(m.matches("A/B.FTL", "dummy"));
        
        try {
            new PathGlobMatcher("/b.ftl");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testFileNameGlobMatcher() throws IOException {
        FileNameGlobMatcher m = new FileNameGlobMatcher("a*.ftl");
        assertTrue(m.matches("ab.ftl", "dummy"));
        assertTrue(m.matches("dir/ab.ftl", "dummy"));
        assertTrue(m.matches("/dir/dir/ab.ftl", "dummy"));
        assertFalse(m.matches("Ab.ftl", "dummy"));
        assertFalse(m.matches("bb.ftl", "dummy"));
        assertFalse(m.matches("ab.ftl/x", "dummy"));

        m = new FileNameGlobMatcher("a*.ftl").caseInsensitive(true);
        assertTrue(m.matches("AB.FTL", "dummy"));
        m.setCaseInsensitive(false);
        assertFalse(m.matches("AB.FTL", "dummy"));
        
        m = new FileNameGlobMatcher("\u00E1*.ftl").caseInsensitive(true);
        assertTrue(m.matches("\u00C1b.ftl", "dummy"));
        
        try {
            new FileNameGlobMatcher("dir/a*.ftl");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testFileExtensionMatcher() throws IOException {
        FileExtensionMatcher m = new FileExtensionMatcher("ftlx");
        assertTrue(m.matches("a.ftlx", "dummy"));
        assertTrue(m.matches(".ftlx", "dummy"));
        assertTrue(m.matches("b/a.b.ftlx", "dummy"));
        assertTrue(m.matches("b/a.ftlx", "dummy"));
        assertTrue(m.matches("c.b/a.ftlx", "dummy"));
        assertFalse(m.matches("a.ftl", "dummy"));
        assertFalse(m.matches("ftlx", "dummy"));
        assertFalse(m.matches("b.ftlx/a.ftl", "dummy"));
        
        assertTrue(m.isCaseInsensitive());
        assertTrue(m.matches("a.fTlX", "dummy"));
        m.setCaseInsensitive(false);
        assertFalse(m.matches("a.fTlX", "dummy"));
        assertTrue(m.matches("A.ftlx", "dummy"));
        
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
            new FileExtensionMatcher("*.ftlx");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new FileExtensionMatcher("ftl?");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new FileExtensionMatcher(".ftlx");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new FileExtensionMatcher("dir/a.ftl");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testPathRegexMatcher() throws IOException {
        PathRegexMatcher m = new PathRegexMatcher("a/[a-z]+\\.ftl");
        assertTrue(m.matches("a/b.ftl", "dummy"));
        assertTrue(m.matches("a/abc.ftl", "dummy"));
        assertFalse(m.matches("b.ftl", "dummy"));
        assertFalse(m.matches("b/b.ftl", "dummy"));
        
        try {
            new PathRegexMatcher("/b.ftl");
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
        assertFalse(m.matches("ab.ftl", "dummy"));
        
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
        assertTrue(m.matches("ab.ftl", "dummy"));
        assertFalse(m.matches("bc.ftl", "dummy"));
        
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
