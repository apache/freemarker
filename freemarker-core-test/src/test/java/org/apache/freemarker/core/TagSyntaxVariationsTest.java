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

import junit.framework.TestCase;
import org.apache.freemarker.core.userpkg.UpperCaseDirective;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.test.TestConfigurationBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

/**
 * Test various generated templates (permutations), including some deliberately
 * wrong ones, with various tagSyntax settings.
 */
public class TagSyntaxVariationsTest extends TestCase {
    
    private static final String IF_ANG = "<#if true>i</#if>";
    private static final String IF_SQU = squarify(IF_ANG);
    private static final String IF_OUT = "i";
    private static final String ASSIGN_ANG = "<#assign x = 1>a";
    private static final String ASSIGN_SQU = squarify(ASSIGN_ANG);
    private static final String ASSIGN_OUT = "a";
    private static final String WRONG_ANG = "<#wrong>";
    private static final String WRONG_SQU = squarify(WRONG_ANG);
    private static final String WRONGC_ANG = "</#wrong>";
    private static final String WRONGC_SQU = squarify(WRONGC_ANG );
    private static final String CUST_ANG = "<@upperCase>z</@>";
    private static final String CUST_SQU = squarify(CUST_ANG);
    private static final String CUST_OUT = "Z";
    
    public TagSyntaxVariationsTest(String name) {
        super(name);
    }
    
    private static String squarify(String s) {
        return s.replace('<', '[').replace('>', ']');
    }

    public final void test()
            throws TemplateException, IOException {
        Map<String, UpperCaseDirective> sharedVariables =
                Collections.singletonMap("upperCase", UpperCaseDirective.INSTANCE);

        // Permutations
        for (int ifOrAssign = 0; ifOrAssign < 2; ifOrAssign++) {
            String dir_ang = ifOrAssign == 0 ? IF_ANG : ASSIGN_ANG;
            String dir_squ = ifOrAssign == 0 ? IF_SQU : ASSIGN_SQU;
            String dir_out = ifOrAssign == 0 ? IF_OUT : ASSIGN_OUT;

            // Permutations 
            for (int angOrSqu = 0; angOrSqu < 2; angOrSqu++) {
                Configuration cfg = new TestConfigurationBuilder()
                        .templateLanguage(angOrSqu == 0
                                ? DefaultTemplateLanguage.F3AU
                                : DefaultTemplateLanguage.F3SU)
                        .sharedVariables(sharedVariables)
                        .build();

                String dir_xxx = angOrSqu == 0 ? dir_ang : dir_squ;
                String cust_xxx = angOrSqu == 0 ? CUST_ANG : CUST_SQU;
                String wrong_xxx = angOrSqu == 0 ? WRONG_ANG : WRONG_SQU;
                String wrongc_xxx = angOrSqu == 0 ? WRONGC_ANG : WRONGC_SQU;

                test(cfg,
                        dir_xxx + cust_xxx,
                        dir_out + CUST_OUT);

                // Permutations 
                for (int wrongOrWrongc = 0; wrongOrWrongc < 2; wrongOrWrongc++) {
                    String wrongx_xxx = wrongOrWrongc == 0 ? wrong_xxx : wrongc_xxx;

                    test(cfg,
                            wrongx_xxx + dir_xxx,
                            null);

                    test(cfg,
                            dir_xxx + wrongx_xxx,
                            null);

                    test(cfg,
                            cust_xxx + wrongx_xxx + dir_xxx,
                            null);
                } // for wrongc
            } // for assign
        }
    }
    
    /**
     * @param expected the expected output or {@code null} if we expect
     * a parsing error.
     */
    private static void test(
            Configuration cfg, String template, String expected)
            throws TemplateException, IOException {
        Template t = null;
        try {
            t = new Template("string", new StringReader(template), cfg);
        } catch (ParseException e) {
            if (expected != null) {
                fail("Couldn't invoke Template from "
                        + _StringUtils.jQuote(template) + ": " + e);
            } else {
                return;
            }
        }
        if (expected == null) fail("Template parsing should have fail for "
                + _StringUtils.jQuote(template));
        
        StringWriter out = new StringWriter();
        t.process(new Object(), out);
        assertEquals(expected, out.toString());
    }

}
