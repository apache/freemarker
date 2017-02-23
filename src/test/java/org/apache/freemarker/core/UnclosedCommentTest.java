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

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class UnclosedCommentTest extends TemplateTest {
    
    @Test
    public void test() throws IOException, TemplateException {
        setConfiguration(new Configuration(Configuration.VERSION_3_0_0));
        assertErrorContains("foo<#--", "end of file");  // Not too good...
        assertErrorContains("foo<#-- ", "Unclosed", "<#--");
        assertErrorContains("foo<#--bar", "Unclosed", "<#--");
        assertErrorContains("foo\n<#--\n", "Unclosed", "<#--");
        assertErrorContains("foo<#noparse>", "end of file");  // Not too good...
        assertErrorContains("foo<#noparse> ", "Unclosed", "#noparse");
        assertErrorContains("foo<#noparse>bar", "Unclosed", "#noparse");
        assertErrorContains("foo\n<#noparse>\n", "Unclosed", "#noparse");
    }
    
}
