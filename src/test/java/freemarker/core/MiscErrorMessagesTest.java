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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.cache.TemplateNameFormat;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class MiscErrorMessagesTest extends TemplateTest {

    @Test
    public void stringIndexOutOfBounds() {
        assertErrorContains("${'foo'[10]}", "length", "3", "10", "String index out of");
    }
    
    @Test
    public void wrongTemplateNameFormat() {
        getConfiguration().setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        
        assertErrorContains("<#include 'foo:/bar:baaz'>", "Malformed template name", "':'");
        assertErrorContains("<#include '../baaz'>", "Malformed template name", "root");
        assertErrorContains("<#include '\u0000'>", "Malformed template name", "\\u0000");
    }

    @Test
    public void numericalKeyHint() {
        assertErrorContains("${{}[10]}", "[]", "?api");
    }
    
    @Test
    public void aritheticException() {
        Throwable e = assertErrorContains("<#assign x = 0>\n${1 / x}", "Arithmetic");
        assertThat(e, instanceOf(TemplateException.class));
        assertEquals((Integer) 2, ((TemplateException) e).getLineNumber());
    }
    
    @Test
    public void incrementalAssignmentsTest() throws Exception {
        assertErrorContains("<#assign x++>", "\"x\"", "++", "template namespace");
        assertErrorContains("<#global x += 2>", "\"x\"", "+=", "global scope");
        assertErrorContains("<#macro m><#local x--></#macro><@m/>", "\"x\"", "--", "local scope");
    }

    @Test
    public void assignmentNamespaceChecks() {
        assertErrorContains("<#assign x = 1 in noSuchVar>", InvalidReferenceException.class, "noSuchVar");
        assertErrorContains("<#assign x =1 in 'notANamespace'>", NonNamespaceException.class, "notANamespace");
    }

    @Test
    public void blockAssignmentNamespaceChecks() {
        assertErrorContains("<#assign x in noSuchVar>1</#assign>", InvalidReferenceException.class, "noSuchVar");
        assertErrorContains("<#assign x in 'notANamespace'>1</#assign>", NonNamespaceException.class, "notANamespace");
    }

}
