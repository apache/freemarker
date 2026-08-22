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

import java.util.List;

import org.junit.Test;

import freemarker.template.utility.StringUtil;

public class TemplatePathUtilsTest {

    @Test
    public void testIsInsideBaseDir() {
        List.of(
                        "..", "/..", "a/../..", "/a//b/./c/../../../..",
                        "a///b/../../..", "/a///b/../../..",
                        "a/../b/../c/../../", "a/../../b/c", "aa/bb/../cc/../../dd/../..",
                        ".. ", "a/../.. ", "..\t\t", "a/../..\t\t" // Odd trailing WS behavior on Windows
                )
                .forEach(name -> assertFalse(
                        "Expected false for " + StringUtil.jQuote(name),
                        _TemplatePathUtils.isInsideBaseDir(name)));
        List.of(
                        ".", "", "a", "a/b", "a/b/", "a/..", "a///b/../..", "/a///b/../..",
                        "/a//b/./c/../../..", "a/../b/../c/../",
                        ".../.../...", "././.", "aa/bb/../cc/../../dd", "aa/bb/../cc/../../dd/..",
                        "a/.. /.. "
                )
                .forEach(name -> assertTrue(
                        "Expected true for " + StringUtil.jQuote(name),
                        _TemplatePathUtils.isInsideBaseDir(name)));
    }
}
