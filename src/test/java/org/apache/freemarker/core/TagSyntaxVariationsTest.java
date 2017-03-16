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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.freemarker.core.util._StringUtil;

import junit.framework.TestCase;

/**
 * Test various generated templates (permutations), including some deliberately
 * wrong ones, with various tag_syntax settings.  
 */
public class TagSyntaxVariationsTest extends TestCase {
    
    private static final String HDR_ANG = "<#ftl>";
    private static final String HDR_SQU = squarify(HDR_ANG);
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
    private static final String CUST_ANG = "<@compress> z </@>";
    private static final String CUST_SQU = squarify(CUST_ANG);
    private static final String CUST_OUT = "z";
    
    public TagSyntaxVariationsTest(String name) {
        super(name);
    }
    
    private static String squarify(String s) {
        return s.replace('<', '[').replace('>', ']');
    }

    public final void test()
            throws TemplateException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);

        // Permutations 
        for (int ifOrAssign = 0; ifOrAssign < 2; ifOrAssign++) {
            String dir_ang = ifOrAssign == 0 ? IF_ANG : ASSIGN_ANG; 
            String dir_squ = ifOrAssign == 0 ? IF_SQU : ASSIGN_SQU; 
            String dir_out = ifOrAssign == 0 ? IF_OUT : ASSIGN_OUT; 
            
            // Permutations 
            for (int angOrSqu = 0; angOrSqu < 2; angOrSqu++) {
                cfg.setTagSyntax(angOrSqu == 0
                        ? Configuration.ANGLE_BRACKET_TAG_SYNTAX
                        : Configuration.SQUARE_BRACKET_TAG_SYNTAX);
                
                String dir_xxx = angOrSqu == 0 ? dir_ang : dir_squ;
                String cust_xxx = angOrSqu == 0 ? CUST_ANG : CUST_SQU;
                String hdr_xxx = angOrSqu == 0 ? HDR_ANG : HDR_SQU;
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
                            hdr_xxx + wrongx_xxx,
                            null);
                    
                    test(cfg,
                            cust_xxx + wrongx_xxx + dir_xxx,
                            null);
                } // for wrongc
            } // for squ
            
            cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
            for (int perm = 0; perm < 4; perm++) {
                // All 4 permutations
                String wrong_xxx = (perm & 1) == 0 ? WRONG_ANG : WRONG_SQU;
                String dir_xxx = (perm & 2) == 0 ? dir_ang : dir_squ;
                
                test(cfg,
                        wrong_xxx + dir_xxx,
                        null);
            } // for perm
    
            // Permutations 
            for (int angOrSquStart = 0; angOrSquStart < 2; angOrSquStart++) {
                String hdr_xxx = angOrSquStart == 0 ? HDR_ANG : HDR_SQU;
                String cust_xxx = angOrSquStart == 0 ? CUST_ANG : CUST_SQU;
                String wrong_yyy = angOrSquStart != 0 ? WRONG_ANG : WRONG_SQU;
                String dir_xxx = angOrSquStart == 0 ? dir_ang : dir_squ;
                String dir_yyy = angOrSquStart != 0 ? dir_ang : dir_squ;
                
                test(cfg,
                        cust_xxx + wrong_yyy + dir_xxx,
                        CUST_OUT + wrong_yyy + dir_out);
                
                test(cfg,
                        hdr_xxx + wrong_yyy + dir_xxx,
                        wrong_yyy + dir_out);
                
                test(cfg,
                        cust_xxx + wrong_yyy + dir_yyy,
                        CUST_OUT + wrong_yyy + dir_yyy);
                
                test(cfg,
                        hdr_xxx + wrong_yyy + dir_yyy,
                        wrong_yyy + dir_yyy);
                
                test(cfg,
                        dir_xxx + wrong_yyy + dir_yyy,
                        dir_out + wrong_yyy + dir_yyy);
            } // for squStart
            
        } // for assign
    }
    
    /**
     * @param expected the expected output or <tt>null</tt> if we expect
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
                        + _StringUtil.jQuote(template) + ": " + e);
            } else {
                return;
            }
        }
        if (expected == null) fail("Template parsing should have fail for "
                + _StringUtil.jQuote(template));
        
        StringWriter out = new StringWriter();
        t.process(new Object(), out);
        assertEquals(expected, out.toString());
    }

}
