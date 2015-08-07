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
        assertFalse(m.matches("b.ftl", "dummy"));
        assertFalse(m.matches("a/bc.ftl", "dummy"));
        
        try {
            new PathGlobMatcher("/b.ftl");
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
