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

package freemarker.core;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import freemarker.template.utility.StringUtil;

@RunWith(Parameterized.class)
public class OptInTemplateClassResolverNameValidationTest {

    private final String slash;
    private final String dot;

    public OptInTemplateClassResolverNameValidationTest(String slash, String dot) {
        this.slash = slash;
        this.dot = dot;
    }

    @Parameterized.Parameters(name = "slash=\"{0}\", dot=\"{1}\"")
    public static Collection<Object[]> parameters() {
        return List.of(
                new Object[]{"/", "."},
                new Object[]{"/", "%2E"},
                new Object[]{"\\", "."},
                new Object[]{"\\", "%2e"},
                new Object[]{"%2f", "."},
                new Object[]{"%2F", "."},
                new Object[]{"%2F", "%2e"},
                new Object[]{"%5c", "%2E"},
                new Object[]{"%5c", "."},
                new Object[]{"%5C", "."}
        );
    }

    private String applyParams(String s) {
        return s.replace("/", slash).replace(".", dot);
    }

    @Test
    public void testMayContainsBackStep() {
        for (String path : List.of(
                "..", "../", "/..", "/../",
                "../a", "a/..", "a/../b", "a../..",
                "a\u0000c", "%\u0000c",
                "%/..", "%/..", "%5/..", "%2/..", "%5b/..", "%2b/..")) {
            assertTrue(
                    "Expected " + StringUtil.jQuote(path) + " -> true",
                    OptInTemplateClassResolver.mayContainsBackStep(applyParams(path)));
        }
        for (String path : List.of(
                "./", "/.", "/./", "./a", "a/.", "a/./b", "a/b.", ".a/b", "a//b",
                "a..", "..a", "a../", "/..a", "././.", "abc", "%xx", "..%..", "%30%30/x",
                "...", "a/...", ".../a", "a/.../b",
                "///", "%%%", "\\\\\\", "%", "%5", "%2", ".", "/", "//", "")) {
            assertFalse(
                    "Expected " + StringUtil.jQuote(path) + " -> false",
                    OptInTemplateClassResolver.mayContainsBackStep(applyParams(path)));
        }
    }
}
