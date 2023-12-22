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

import static freemarker.core.DefaultTruncateBuiltinAlgorithm.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

public class DefaultTruncateBuiltinAlgorithmTest {

    private static final DefaultTruncateBuiltinAlgorithm EMPTY_TERMINATOR_INSTANCE =
            new DefaultTruncateBuiltinAlgorithm("", false);
    private static final DefaultTruncateBuiltinAlgorithm DOTS_INSTANCE =
            new DefaultTruncateBuiltinAlgorithm("...", true);
    private static final DefaultTruncateBuiltinAlgorithm DOTS_NO_W_SPACE_INSTANCE =
            new DefaultTruncateBuiltinAlgorithm("...", false);
    private static final DefaultTruncateBuiltinAlgorithm ASCII_NO_W_SPACE_INSTANCE =
            new DefaultTruncateBuiltinAlgorithm("[...]", false);
    private static final DefaultTruncateBuiltinAlgorithm M_TERM_INSTANCE;

    static {
        try {
            M_TERM_INSTANCE = new DefaultTruncateBuiltinAlgorithm(
                    "...", null, true,
                    HTMLOutputFormat.INSTANCE.fromMarkup("<r>...</r>"), null, true,
                    true, 0.75);
        } catch (TemplateModelException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConstructorIllegalArguments() throws TemplateException {
        try {
            new DefaultTruncateBuiltinAlgorithm(
                    null, null, true,
                    HTMLOutputFormat.INSTANCE.fromMarkup("<r>...</r>"), null, true,
                    true, 0.75);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("defaultTerminator"));
        }
    }

    @Test
    public void testTruncateIllegalArguments() throws TemplateException {
        Environment env = createEnvironment();

        ASCII_INSTANCE.truncate("", 0, new SimpleScalar("."), 1, env);

        try {
            ASCII_INSTANCE.truncate("", -1, new SimpleScalar("."), 1, env);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("maxLength"));
        }

        try {
            ASCII_INSTANCE.truncateM("sss", 2, new SimpleNumber(1), 1, env);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("SimpleNumber"));
        }

        try {
            ASCII_INSTANCE.truncate("sss", 2, new SimpleScalar("."), -1, env);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("terminatorLength"));
        }
    }

    private Environment createEnvironment() {
        try {
            return new Template("", "", new Configuration(Configuration.VERSION_2_3_28)).createProcessingEnvironment(null,
                    null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCSimple() {
        assertC(ASCII_INSTANCE, "12345678", 9, "12345678");
        assertC(ASCII_INSTANCE, "12345678", 8, "12345678");
        assertC(ASCII_INSTANCE, "12345678", 7, "12[...]");
        assertC(ASCII_INSTANCE, "12345678", 6, "1[...]");
        for (int maxLength = 5; maxLength >= 0; maxLength--) {
            assertC(ASCII_INSTANCE, "12345678", maxLength, "[...]");
        }

        assertC(UNICODE_INSTANCE, "12345678", 9, "12345678");
        assertC(UNICODE_INSTANCE, "12345678", 8, "12345678");
        assertC(UNICODE_INSTANCE, "12345678", 7, "1234[\u2026]");
        assertC(UNICODE_INSTANCE, "12345678", 6, "123[\u2026]");
        assertC(UNICODE_INSTANCE, "12345678", 5, "12[\u2026]");
        assertC(UNICODE_INSTANCE, "12345678", 4, "1[\u2026]");
        for (int maxLength = 3; maxLength >= 0; maxLength--) {
            assertC(UNICODE_INSTANCE, "12345678", maxLength, "[\u2026]");
        }

        assertC(EMPTY_TERMINATOR_INSTANCE, "12345678", 9, "12345678");
        for (int length = 8; length >= 0; length--) {
            assertC(EMPTY_TERMINATOR_INSTANCE, "12345678", length, "12345678".substring(0, length));
        }
    }

    @Test
    public void testCSpaceAndDot() {
        assertC(ASCII_INSTANCE, "123456  ", 9, "123456  ");
        assertC(ASCII_INSTANCE, "123456  ", 8, "123456  ");
        assertC(ASCII_INSTANCE, "123456  ", 7, "12[...]");
        assertC(ASCII_INSTANCE, "123456  ", 6, "1[...]");
        assertC(ASCII_INSTANCE, "123456  ", 5, "[...]");
        assertC(ASCII_INSTANCE, "123456  ", 4, "[...]");

        assertC(ASCII_INSTANCE, "1 345        ", 13, "1 345        ");
        assertC(ASCII_INSTANCE, "1 345        ", 12, "1 345 [...]"); // Not "1 345  [...]"
        assertC(ASCII_INSTANCE, "1 345        ", 11, "1 345 [...]");
        assertC(ASCII_INSTANCE, "1 345        ", 10, "1 34[...]"); // Not "12345[...]"
        assertC(ASCII_INSTANCE, "1 345        ", 9,  "1 34[...]");
        assertC(ASCII_INSTANCE, "1 345        ", 8,  "1 3[...]");
        assertC(ASCII_INSTANCE, "1 345        ", 7,  "1 [...]");
        assertC(ASCII_INSTANCE, "1 345        ", 6,  "[...]"); // Not "1[...]"
        assertC(ASCII_INSTANCE, "1 345        ", 5,  "[...]");
        assertC(ASCII_INSTANCE, "1 345        ", 4,  "[...]");

        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 13, "1 345        ");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 12, "1 345[...]"); // Differs!
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 11, "1 345[...]"); // Differs!
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 10, "1 345[...]"); // Differs!
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 9,  "1 34[...]");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 8,  "1 3[...]");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 7,  "1[...]"); // Differs!
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 6,  "1[...]"); // Differs!
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 5,  "[...]");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1 345        ", 4,  "[...]");

        assertC(ASCII_INSTANCE, "1  4567890", 9,  "1  4[...]");
        assertC(ASCII_INSTANCE, "1  4567890", 8,  "1 [...]");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1  4567890", 9,  "1  4[...]");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "1  4567890", 8,  "1[...]");

        assertC(ASCII_INSTANCE, "  3456789", 9,  "  3456789");
        assertC(ASCII_INSTANCE, "  3456789", 8,  "  3[...]");
        assertC(ASCII_INSTANCE, "  3456789", 7,  "[...]");
        assertC(ASCII_INSTANCE, "  3456789", 6,  "[...]");

        assertC(ASCII_NO_W_SPACE_INSTANCE, "  3456789", 8,  "  3[...]");
        assertC(ASCII_NO_W_SPACE_INSTANCE, "  3456789", 7,  "[...]");

        // Dots aren't treated specially by default:
        assertC(ASCII_INSTANCE, "1.  56...012345", 15, "1.  56...012345");
        assertC(ASCII_INSTANCE, "1.  56...012345", 14, "1.  56...[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 13, "1.  56..[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 12, "1.  56.[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 11, "1.  56[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 10, "1.  5[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 9,  "1. [...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 8,  "1. [...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 7,  "1[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 6,  "1[...]");
        assertC(ASCII_INSTANCE, "1.  56...012345", 5,  "[...]");

        // Dots are treated specially here:
        assertC(DOTS_INSTANCE, "1.  56...012345", 15, "1.  56...012345");
        assertC(DOTS_INSTANCE, "1.  56...012345", 14, "1.  56...01...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 13, "1.  56...0...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 12, "1.  56...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 11, "1.  56...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 10, "1.  56...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 9,  "1.  56...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 8,  "1.  5...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 7,  "1. ...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 6,  "1. ...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 5,  "1...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 4,  "1...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 3,  "...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 2,  "...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 1,  "...");
        assertC(DOTS_INSTANCE, "1.  56...012345", 0,  "...");

        assertC(DOTS_NO_W_SPACE_INSTANCE, "1.  56...012345", 8,  "1.  5...");
        assertC(DOTS_NO_W_SPACE_INSTANCE, "1.  56...012345", 7,  "1...");
        assertC(DOTS_NO_W_SPACE_INSTANCE, "1.  56...012345", 6,  "1...");
        assertC(DOTS_NO_W_SPACE_INSTANCE, "1.  56...012345", 5,  "1...");
        assertC(DOTS_NO_W_SPACE_INSTANCE, "1.  56...012345", 4,  "1...");
        assertC(DOTS_NO_W_SPACE_INSTANCE, "1.  56...012345", 3,  "...");

        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 6, "ab. cd");
        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 5, "ab. c");
        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 4, "ab.");
        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 3, "ab.");
        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 2, "ab");
        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 1, "a");
        assertC(EMPTY_TERMINATOR_INSTANCE, "ab. cd", 0, "");
    }

    @Test
    public void testWSimple() {
        assertW(ASCII_INSTANCE, "word1 word2 word3", 18, "word1 word2 word3");
        assertW(ASCII_INSTANCE, "word1 word2 word3", 17, "word1 word2 word3");
        assertW(ASCII_INSTANCE, "word1 word2 word3", 16, "word1 [...]");
        assertW(ASCII_INSTANCE, "word1 word2 word3", 11, "word1 [...]");
        for (int maxLength = 10; maxLength >= 0; maxLength--) {
            assertW(ASCII_INSTANCE, "word1 word2 word3", maxLength, "[...]");
        }

        assertW(UNICODE_INSTANCE, "word1 word2 word3", 18, "word1 word2 word3");
        assertW(UNICODE_INSTANCE, "word1 word2 word3", 17, "word1 word2 word3");
        assertW(UNICODE_INSTANCE, "word1 word2 word3", 16, "word1 word2 [\u2026]");
        assertW(UNICODE_INSTANCE, "word1 word2 word3", 15, "word1 word2 [\u2026]");
        assertW(UNICODE_INSTANCE, "word1 word2 word3", 14, "word1 [\u2026]");
        assertW(UNICODE_INSTANCE, "word1 word2 word3", 9, "word1 [\u2026]");
        for (int maxLength = 8; maxLength >= 0; maxLength--) {
            assertW(UNICODE_INSTANCE, "word1 word2 word3", maxLength, "[\u2026]");
        }

        assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", 18, "word1 word2 word3");
        assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", 17, "word1 word2 word3");
        assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", 16, "word1 word2");
        assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", 11, "word1 word2");
        assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", 10, "word1");
        assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", 5, "word1");
        for (int maxLength = 4; maxLength >= 0; maxLength--) {
            assertW(EMPTY_TERMINATOR_INSTANCE, "word1 word2 word3", maxLength, "");
        }
    }

    @Test
    public void testWSpaceAndDot() {
        assertW(DOTS_INSTANCE, "  word1  word2  ", 16, "  word1  word2  ");
        assertW(DOTS_INSTANCE, "  word1  word2  ", 15, "  word1 ...");
        assertW(DOTS_INSTANCE, "  word1  word2  ", 11, "  word1 ...");
        for (int maxLength = 10; maxLength >= 0; maxLength--) {
            assertW(DOTS_INSTANCE, "  word1  word2  ", maxLength, "...");
        }

        assertW(DOTS_NO_W_SPACE_INSTANCE, "  word1  word2  ", 16, "  word1  word2  ");
        assertW(DOTS_NO_W_SPACE_INSTANCE, "  word1  word2  ", 15, "  word1...");
        assertW(DOTS_NO_W_SPACE_INSTANCE, "  word1  word2  ", 10, "  word1...");
        for (int maxLength = 9; maxLength >= 0; maxLength--) {
            assertW(DOTS_NO_W_SPACE_INSTANCE, "  word1  word2  ", maxLength, "...");
        }

        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 23, " . . word1..  word2    ");
        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 22, " . . word1.. ...");
        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 16, " . . word1.. ...");
        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 15, " . . ...");
        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 8, " . . ...");
        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 7, " . ...");
        assertW(DOTS_INSTANCE, " . . word1..  word2    ", 6, " . ...");
        for (int maxLength = 5; maxLength >= 0; maxLength--) {
            assertW(DOTS_INSTANCE, " . . word1..  word2    ", maxLength, "...");
        }

        assertW(DOTS_NO_W_SPACE_INSTANCE, " . . word1..  word2    ", 23, " . . word1..  word2    ");
        assertW(DOTS_NO_W_SPACE_INSTANCE, " . . word1..  word2    ", 22, " . . word1..  word2...");
        assertW(DOTS_NO_W_SPACE_INSTANCE, " . . word1..  word2    ", 21, " . . word1...");
        for (int maxLength = 13; maxLength >= 0; maxLength--) {
            assertW(DOTS_NO_W_SPACE_INSTANCE, " . . word1..  word2    ", maxLength, "...");
        }
    }

    /**
     * "Auto" means plain trunce(..) call, because the tested implementation chooses between CB and WB automatically.
     */
    @Test
    public void testAuto() {
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 24, "1 234567 90ABCDEFGHIJKL");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 23, "1 234567 90ABCDEFGHIJKL");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 21, "1 234567 90ABCDE[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 20, "1 234567 90ABCD[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 19, "1 234567 90ABC[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 18, "1 234567 [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 17, "1 234567 [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 16, "1 234567 [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 15, "1 234567 [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 14, "1 234567 [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 13, "1 23456[...]"); // wb space
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJKL", 12, "1 23456[...]");

        assertAuto(ASCII_INSTANCE, "1 234567  0ABCDEFGHIJKL", 22, "1 234567  0ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 9 ABCDEFGHIJKL", 22, "1 234567 9 ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90 BCDEFGHIJKL", 22, "1 234567 90 [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90A CDEFGHIJKL", 22, "1 234567 90A [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90AB DEFGHIJKL", 22, "1 234567 90AB [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABC EFGHIJKL", 22, "1 234567 90ABC [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCD FGHIJKL", 22, "1 234567 90ABCD [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDE GHIJKL", 22, "1 234567 90ABCDE [...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEF HIJKL", 22, "1 234567 90ABCDE[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFG IJKL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGH JKL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHI KL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJ L", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_INSTANCE, "1 234567 90ABCDEFGHIJK ", 22, "1 234567 90ABCDEF[...]");

        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567  0ABCDEFGHIJKL", 22, "1 234567  0ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 9 ABCDEFGHIJKL", 22, "1 234567 9 ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90 BCDEFGHIJKL", 22, "1 234567 90 BCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90A CDEFGHIJKL", 22, "1 234567 90A[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90AB DEFGHIJKL", 22, "1 234567 90AB[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABC EFGHIJKL", 22, "1 234567 90ABC[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCD FGHIJKL", 22, "1 234567 90ABCD[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDE GHIJKL", 22, "1 234567 90ABCDE[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDEF HIJKL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDEFG IJKL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDEFGH JKL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDEFGHI KL", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDEFGHIJ L", 22, "1 234567 90ABCDEF[...]");
        assertAuto(ASCII_NO_W_SPACE_INSTANCE, "1 234567 90ABCDEFGHIJK ", 22, "1 234567 90ABCDEF[...]");

        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 24, "12390ABCD..  . EFGHIJK .");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 23, "12390ABCD..  . ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 22, "12390ABCD..  . ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 21, "12390ABCD..  . ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 20, "12390ABCD..  . ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 19, "12390ABCD..  . ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 18, "12390ABCD..  . ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 17, "12390ABCD.. ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 16, "12390ABCD.. ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 15, "12390ABCD.. ...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 14, "12390ABCD...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 13, "12390ABCD...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 12, "12390ABCD...");
        assertAuto(DOTS_INSTANCE, "12390ABCD..  . EFGHIJK .", 11, "12390ABC...");

        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 27, "word0 word1. word2 w3 . ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 26, "word0 word1. word2 w3 ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 25, "word0 word1. word2 w3 ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 24, "word0 word1. word2 ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 22, "word0 word1. word2 ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 21, "word0 word1. ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 16, "word0 word1. ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 15, "word0 word1...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 14, "word0 word1...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 13, "word0 word...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 12, "word0 ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 9,  "word0 ...");
        assertAuto(DOTS_INSTANCE, "word0 word1. word2 w3 . . w4", 8,  "word...");
    }

    @Test
    public void testExtremeWordBoundaryMinLengths() {
        assertC(ASCII_INSTANCE, "1 3456789", 8,  "1 3[...]");
        assertW(ASCII_INSTANCE, "1 3456789", 8,  "1 [...]");
        DefaultTruncateBuiltinAlgorithm wbMinLen1Algo = new DefaultTruncateBuiltinAlgorithm(
                ASCII_INSTANCE.getDefaultTerminator(), null, null,
                null, null, null,
                true, 1.0);
        assertAuto(wbMinLen1Algo, "1 3456789", 8,  "1 3[...]");

        assertAuto(ASCII_INSTANCE, "123456789", 8,  "123[...]");
        DefaultTruncateBuiltinAlgorithm wbMinLen0Algo = new DefaultTruncateBuiltinAlgorithm(
                ASCII_INSTANCE.getDefaultTerminator(), null, null,
                null, null, null,
                true, 0.0);
        assertAuto(wbMinLen0Algo, "123456789", 8,  "[...]");
    }

    @Test
    public void testSimpleEdgeCases() throws TemplateException {
        Environment env = createEnvironment();
        for (final DefaultTruncateBuiltinAlgorithm alg : new DefaultTruncateBuiltinAlgorithm[] {
                ASCII_INSTANCE, UNICODE_INSTANCE,
                EMPTY_TERMINATOR_INSTANCE, DOTS_INSTANCE, ASCII_NO_W_SPACE_INSTANCE, M_TERM_INSTANCE }) {
            for (TruncateCaller tc : new TruncateCaller[] {
                    new TruncateCaller() {
                        public TemplateModel truncate(String s, int maxLength, TemplateModel terminator,
                                Integer terminatorLength, Environment env) throws
                                TemplateException {
                            return alg.truncateM(s, maxLength, terminator, terminatorLength, env);
                        }
                    },
                    new TruncateCaller() {
                        public TemplateModel truncate(String s, int maxLength, TemplateModel terminator,
                                Integer terminatorLength, Environment env) throws
                                TemplateException {
                            return alg.truncateCM(s, maxLength, terminator, terminatorLength, env);
                        }
                    },
                    new TruncateCaller() {
                        public TemplateModel truncate(String s, int maxLength, TemplateModel terminator,
                                Integer terminatorLength, Environment env) throws
                                TemplateException {
                            return alg.truncateWM(s, maxLength, terminator, terminatorLength, env);
                        }
                    }
            }) {
                assertEquals("", tc.truncate("", 0, null, null, env).toString(), "");
                assertEquals("", tc.truncate("", 0, null, null, env).toString(), "");
                if (alg.getDefaultMTerminator() != null) {
                    TemplateModel truncated = tc.truncate("x", 0, null, null, env);
                    assertThat(truncated, instanceOf(TemplateMarkupOutputModel.class));
                    assertSame(alg.getDefaultMTerminator(), truncated);
                } else {
                    TemplateModel truncated = tc.truncate("x", 0, null, null, env);
                    assertThat(truncated, instanceOf(TemplateScalarModel.class));
                    assertEquals(alg.getDefaultTerminator(), ((TemplateScalarModel) truncated).getAsString());
                }
                SimpleScalar stringTerminator = new SimpleScalar("|");
                assertSame(stringTerminator, tc.truncate("x", 0, stringTerminator, null, env));
                TemplateHTMLOutputModel htmlTerminator = HTMLOutputFormat.INSTANCE.fromMarkup("<x>.</x>");
                assertSame(htmlTerminator, tc.truncate("x", 0, htmlTerminator, null, env));
            }
        }
    }

    @Test
    public void testStandardInstanceSettings() throws TemplateException {
        Environment env = createEnvironment();

        assertEquals(
                "123[...]",
                ASCII_INSTANCE.truncate("1234567890", 8, null, null, env)
                    .getAsString());
        assertEquals(
                "12345<span class='truncateTerminator'>[&#8230;]</span>",
                HTMLOutputFormat.INSTANCE.getMarkupString(
                ((TemplateHTMLOutputModel) ASCII_INSTANCE
                        .truncateM("1234567890", 8, null, null, env))
                ));

        assertEquals(
                "12345[\u2026]",
                UNICODE_INSTANCE.truncate("1234567890", 8, null, null, env)
                        .getAsString());
        assertEquals(
                "12345<span class='truncateTerminator'>[&#8230;]</span>",
                HTMLOutputFormat.INSTANCE.getMarkupString(
                        ((TemplateHTMLOutputModel) UNICODE_INSTANCE
                                .truncateM("1234567890", 8, null, null, env))
                ));
    }

    private void assertC(TruncateBuiltinAlgorithm algorithm, String in, int maxLength, String expected) {
        try {
            TemplateScalarModel actual = algorithm.truncateC(in, maxLength, null, null, null);
            assertEquals(expected, actual.getAsString());
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertW(TruncateBuiltinAlgorithm algorithm, String in, int maxLength, String expected) {
        try {
            TemplateScalarModel actual = algorithm.truncateW(in, maxLength, null, null, null);
            assertEquals(expected, actual.getAsString());
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertAuto(TruncateBuiltinAlgorithm algorithm, String in, int maxLength, String expected) {
        try {
            TemplateScalarModel actual = algorithm.truncate(
                    in, maxLength, null, null, null);
            assertEquals(expected, actual.getAsString());
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    interface TruncateCaller {
        TemplateModel truncate(
                String s, int maxLength,
                TemplateModel terminator, Integer terminatorLength,
                Environment env) throws TemplateException;
    }

    @Test
    public void testGetLengthWithoutTags() {
        assertEquals(0,  getLengthWithoutTags(""));
        assertEquals(1,  getLengthWithoutTags("a"));
        assertEquals(2,  getLengthWithoutTags("ab"));
        assertEquals(0,  getLengthWithoutTags("<tag>"));
        assertEquals(1,  getLengthWithoutTags("<tag>a"));
        assertEquals(2,  getLengthWithoutTags("<tag>a</tag>b"));
        assertEquals(4,  getLengthWithoutTags("ab<tag>cd</tag>"));
        assertEquals(2,  getLengthWithoutTags("ab<tag></tag>"));

        assertEquals(2,  getLengthWithoutTags("&chr;a"));
        assertEquals(4,  getLengthWithoutTags("&chr;a&chr;b"));
        assertEquals(6,  getLengthWithoutTags("ab&chr;cd&chr;"));
        assertEquals(4,  getLengthWithoutTags("ab&chr;&chr;"));
        assertEquals(4,  getLengthWithoutTags("ab<tag>&chr;</tag>&chr;"));

        assertEquals(2,  getLengthWithoutTags("<!--c-->ab"));
        assertEquals(2,  getLengthWithoutTags("a<!--c-->b<!--c-->"));
        assertEquals(2,  getLengthWithoutTags("a<!-->--><!---->b"));

        assertEquals(3,  getLengthWithoutTags("a<![CDATA[b]]>c"));
        assertEquals(2,  getLengthWithoutTags("a<![CDATA[]]>b"));
        assertEquals(0,  getLengthWithoutTags("<![CDATA[]]>"));
        assertEquals(3,  getLengthWithoutTags("<![CDATA[123"));
        assertEquals(4,  getLengthWithoutTags("<![CDATA[123]"));
        assertEquals(5,  getLengthWithoutTags("<![CDATA[123]]"));
        assertEquals(3,  getLengthWithoutTags("<![CDATA[123]]>"));

        assertEquals(2,  getLengthWithoutTags("ab<!--"));
        assertEquals(2,  getLengthWithoutTags("ab<tag"));
        assertEquals(3,  getLengthWithoutTags("ab&chr"));
        assertEquals(2,  getLengthWithoutTags("ab<!-"));
        assertEquals(2,  getLengthWithoutTags("ab<"));
        assertEquals(3,  getLengthWithoutTags("ab&"));
        assertEquals(3,  getLengthWithoutTags("a&;c"));
    }

    @Test
    public void testGetCodeFromNumericalCharReferenceName() {
        assertEquals(0, getCodeFromNumericalCharReferenceName("#0"));
        assertEquals(0, getCodeFromNumericalCharReferenceName("#00"));
        assertEquals(0, getCodeFromNumericalCharReferenceName("#x0"));
        assertEquals(0, getCodeFromNumericalCharReferenceName("#x00"));
        assertEquals(1, getCodeFromNumericalCharReferenceName("#1"));
        assertEquals(1, getCodeFromNumericalCharReferenceName("#01"));
        assertEquals(1, getCodeFromNumericalCharReferenceName("#x1"));
        assertEquals(1, getCodeFromNumericalCharReferenceName("#x01"));
        assertEquals(1, getCodeFromNumericalCharReferenceName("#X1"));
        assertEquals(1, getCodeFromNumericalCharReferenceName("#X01"));
        assertEquals(123409, getCodeFromNumericalCharReferenceName("#123409"));
        assertEquals(123409, getCodeFromNumericalCharReferenceName("#00123409"));
        assertEquals(0x123A0F, getCodeFromNumericalCharReferenceName("#x123A0F"));
        assertEquals(0x123A0F, getCodeFromNumericalCharReferenceName("#x123a0f"));
        assertEquals(0x123A0F, getCodeFromNumericalCharReferenceName("#X00123A0f"));
        assertEquals(-1, getCodeFromNumericalCharReferenceName("#x1G"));
        assertEquals(-1, getCodeFromNumericalCharReferenceName("#1A"));
    }

    @Test
    public void testIsDotCharReference() {
        assertTrue(isDotCharReference("#46"));
        assertTrue(isDotCharReference("#x2E"));
        assertTrue(isDotCharReference("#x2026"));
        assertTrue(isDotCharReference("hellip"));
        assertTrue(isDotCharReference("period"));

        assertFalse(isDotCharReference(""));
        assertFalse(isDotCharReference("foo"));
        assertFalse(isDotCharReference("#x46"));
        assertFalse(isDotCharReference("#boo"));
    }

    @Test
    public void testIsHtmlOrXmlStartsWithDot() {
        assertTrue(doesHtmlOrXmlStartWithDot("."));
        assertTrue(doesHtmlOrXmlStartWithDot(".etc"));
        assertTrue(doesHtmlOrXmlStartWithDot("&hellip;"));
        assertTrue(doesHtmlOrXmlStartWithDot("<tag x='y'/>&hellip;"));
        assertTrue(doesHtmlOrXmlStartWithDot("<span class='t'>...</span>"));
        assertTrue(doesHtmlOrXmlStartWithDot("<span class='t'>&#x2026;</span>"));
        assertTrue(doesHtmlOrXmlStartWithDot("<span class='t'>&#46;</span>"));
        assertTrue(doesHtmlOrXmlStartWithDot("<foo><!-- -->.etc"));

        assertFalse(doesHtmlOrXmlStartWithDot(""));
        assertFalse(doesHtmlOrXmlStartWithDot("[...]"));
        assertFalse(doesHtmlOrXmlStartWithDot("etc."));
        assertFalse(doesHtmlOrXmlStartWithDot("<span class='t'>[...]</span>"));
        assertFalse(doesHtmlOrXmlStartWithDot("<span class='t'>etc.</span>"));
        assertFalse(doesHtmlOrXmlStartWithDot("<span class='t'>&46;</span>"));
    }

    @Test
    public void testTruncateAdhocHtmlTerminator() throws TemplateException {
        Environment env = createEnvironment();
        TemplateHTMLOutputModel htmlEllipsis = HTMLOutputFormat.INSTANCE.fromMarkup("<i>&#x2026;</i>");
        TemplateHTMLOutputModel htmlSquEllipsis = HTMLOutputFormat.INSTANCE.fromMarkup("<i>[&#x2026;]</i>");

        // Length detection
        {
            TemplateModel actual = ASCII_INSTANCE.truncateM("abcd", 3, htmlEllipsis, null, env);
            assertThat(actual, instanceOf(TemplateHTMLOutputModel.class));
            assertEquals(
                    "ab<i>&#x2026;</i>",
                    HTMLOutputFormat.INSTANCE.getMarkupString((TemplateHTMLOutputModel) actual));
        }
        {
            TemplateModel actual = ASCII_INSTANCE.truncateM("abcdef", 5, htmlSquEllipsis, null, env);
            assertThat(actual, instanceOf(TemplateHTMLOutputModel.class));
            assertEquals(
                    "ab<i>[&#x2026;]</i>",
                    HTMLOutputFormat.INSTANCE.getMarkupString((TemplateHTMLOutputModel) actual));
        }
        {
            TemplateModel actual = ASCII_INSTANCE.truncateM("abcdef", 5, htmlSquEllipsis, 1, env);
            assertThat(actual, instanceOf(TemplateHTMLOutputModel.class));
            assertEquals(
                    "abcd<i>[&#x2026;]</i>",
                    HTMLOutputFormat.INSTANCE.getMarkupString((TemplateHTMLOutputModel) actual));
        }

        // Dot removal
        {
            TemplateModel actual = ASCII_INSTANCE.truncateM("a.cd", 3, htmlEllipsis, null, env);
            assertThat(actual, instanceOf(TemplateHTMLOutputModel.class));
            assertEquals(
                    "a<i>&#x2026;</i>",
                    HTMLOutputFormat.INSTANCE.getMarkupString((TemplateHTMLOutputModel) actual));
        }
        {
            TemplateModel actual = ASCII_INSTANCE.truncateM("a.cdef", 5, htmlSquEllipsis, null, env);
            assertThat(actual, instanceOf(TemplateHTMLOutputModel.class));
            assertEquals(
                    "a.<i>[&#x2026;]</i>",
                    HTMLOutputFormat.INSTANCE.getMarkupString((TemplateHTMLOutputModel) actual));
        }
    }

    @Test
    public void testTruncateAdhocPlainTextTerminator() throws TemplateException {
        Environment env = createEnvironment();
        TemplateScalarModel ellipsis = new SimpleScalar("\u2026");
        TemplateScalarModel squEllipsis = new SimpleScalar("[\u2026]");

        // Length detection
        {
            TemplateScalarModel actual = ASCII_INSTANCE.truncate("abcd", 3, ellipsis, null, env);
            assertEquals("ab\u2026", actual.getAsString());
        }
        {
            TemplateScalarModel actual = ASCII_INSTANCE.truncate("abcdef", 5, squEllipsis, null, env);
            assertEquals("ab[\u2026]", actual.getAsString());
        }
        {
            TemplateScalarModel actual = ASCII_INSTANCE.truncate("abcdef", 5, squEllipsis, 1, env);
            assertEquals("abcd[\u2026]", actual.getAsString());
        }

        // Dot removal
        {
            TemplateScalarModel actual = ASCII_INSTANCE.truncate("a.cd", 3, ellipsis, null, env);
            assertEquals("a\u2026", actual.getAsString());
        }
        {
            TemplateScalarModel actual = ASCII_INSTANCE.truncate("a.cdef", 5, squEllipsis, null, env);
            assertEquals("a.[\u2026]", actual.getAsString());
        }
    }

}