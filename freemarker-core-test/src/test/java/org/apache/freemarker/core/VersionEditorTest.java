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

package org.apache.freemarker.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class VersionEditorTest {

    @Test
    public void testFromString() {
        VersionEditor editor = new VersionEditor();
        editor.setAsText("1.2.3-beta2");
        Version v = (Version) editor.getValue();
        assertEquals("1.2.3-beta2", v.toString());
        assertEquals("1.2.3-beta2", editor.getAsText());
    }

    @Test
    public void testFromString2() {
        VersionEditor editor = new VersionEditor();
        editor.setAsText("01.002.0003-20130524");
        Version v = (Version) editor.getValue();
        assertEquals("01.002.0003-20130524", v.toString());
        assertEquals("01.002.0003-20130524", editor.getAsText());

        editor.setAsText("01.002.0003.4");
        v = (Version) editor.getValue();
        assertEquals("01.002.0003.4", v.toString());
        assertEquals("01.002.0003.4", editor.getAsText());

        editor.setAsText("1.2.3.FC");
        v = (Version) editor.getValue();
        assertEquals("1.2.3.FC", v.toString());
        assertEquals("1.2.3.FC", editor.getAsText());

        editor.setAsText("1.2.3mod");
        v = (Version) editor.getValue();
        assertEquals("1.2.3mod", v.toString());
        assertEquals("1.2.3mod", editor.getAsText());

    }

    @Test
    public void testFromStringIncubating() {
        VersionEditor editor = new VersionEditor();
        editor.setAsText("2.3.24-rc01-incubating");
        Version v = (Version) editor.getValue();
        assertEquals("2.3.24-rc01-incubating", v.toString());
        assertEquals("2.3.24-rc01-incubating", editor.getAsText());
    }

    @Test
    public void testMalformed() {
        VersionEditor editor = new VersionEditor();

        try {
            editor.setAsText("1.2.");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            editor.setAsText("1.2.3.");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            editor.setAsText("1..3");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            editor.setAsText(".2");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            editor.setAsText("a");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            editor.setAsText("-a");
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
