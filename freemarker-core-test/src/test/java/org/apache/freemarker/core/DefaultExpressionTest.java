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

import java.util.Collections;

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class DefaultExpressionTest extends TemplateTest {

    @Test
    public void testSimpleChaining() throws Exception {
        assertErrorContains("${a!b!c}", InvalidReferenceException.class, "a!b!c");
        addToDataModel("c", "C");
        assertOutput("${a!b!c}", "C");
        addToDataModel("b", "B");
        assertOutput("${a!b!c}", "B");
        addToDataModel("a", "A");
        assertOutput("${a!b!c}", "A");
        addToDataModel("b", null);
        addToDataModel("c", null);
        assertOutput("${a!b!c}", "A");
    }

    @Test
    public void testPrecedenceHighEnough() throws Exception {
        assertOutput("${a!1 * 2}", "2");
        addToDataModel("a", 2);
        assertOutput("${(a!1) * 2}", "4");
        assertOutput("${a!(1 * 2)}", "2");
        assertOutput("${a!1 * 2}", "4");
        
        assertOutput("${a!1 * (b!3)}", "6");
        assertOutput("${a!(1 * b)!3}", "2");
        assertOutput("${a!1 * b!3}", "6");
        addToDataModel("a", null);
        assertOutput("${a!(1 * b)!3}", "3"); // This will change in FM3 when (exp)!defExp won't be special anymore
        assertOutput("${a!1 * b!3}", "3");
    }

    @Test
    public void testPrecedenceLowEnough() throws Exception {
        addToDataModel("a", Collections.emptyMap());
        addToDataModel("b", Collections.emptyMap());
        addToDataModel("c", Collections.singletonMap("cs", "CS"));
        assertOutput("${a.as!b.bs!c.cs}", "CS");
        assertOutput("${a['as']!b['bs']!c['cs']}", "CS");
        
        addToDataModel("b", Collections.singletonMap("bs", "BS"));
        assertOutput("${a.as!b.bs!c.cs}", "BS");
        assertOutput("${a['as']!b['bs']!c['cs']}", "BS");
        
        addToDataModel("a", Collections.singletonMap("as", "AS"));
        assertOutput("${a.as!b.bs!c.cs}", "AS");
        assertOutput("${a['as']!b['bs']!c['cs']}", "AS");
        addToDataModel("b", Collections.emptyMap());
        assertOutput("${a.as!b.bs!c.cs}", "AS");
        assertOutput("${a['as']!b['bs']!c['cs']}", "AS");
        addToDataModel("c", Collections.singletonMap("cs", "CS"));
        assertOutput("${a.as!b.bs!c.cs}", "AS");
        assertOutput("${a['as']!b['bs']!c['cs']}", "AS");
    }
    
    @Test
    public void testWithUnaryPrefixOps() throws Exception {
        assertOutput("${a!(-1)}", "-1");
        assertOutput("${a!(+1)}", "1");
        assertErrorContains("${a!-1}", "number");
        assertOutput("${a!+1}", "1"); // Because: "" + 1
        addToDataModel("a", 3);
        assertOutput("${a!-1}", "2");
        assertOutput("${a!+1}", "4");

        // Why prefix operators has lower precedence:
        assertOutput("${'x' + u! + v! + 'y'}", "xy");
        addToDataModel("u", "U");
        assertOutput("${'x' + u! + v! + 'y'}", "xUy");
        addToDataModel("v", "V");
        assertOutput("${'x' + u! + v! + 'y'}", "xUVy");
        addToDataModel("u", null);
        assertOutput("${'x' + u! + v! + 'y'}", "xVy");
    }
    
    @Test
    public void testTerminatesBeforeParam() throws Exception {
        assertOutput(
                "<#macro m a b c>[${a}][${b}][${c}]</#macro>"
                + "<@m a=x! b=y! c=z! /> "
                + "<@m a=x!'x' b=y!'y' c=z!'z' /> "                        
                + "<#assign y='Y'>"
                + "<@m a=x! b=y! c=z! />",
                "[][][] [x][y][z] [][Y][]");
    }

    @Test
    public void testDefaultNothing() throws Exception {
        assertOutput("${missing!}", "");
        assertOutput("<#if missing!>t<#else>f</#if>", "f");
        assertOutput("${(missing!)(1, x=2)!'null'}", "null");
        assertOutput("<@missing! 1 x=2>x</@>", "");
        assertOutput("<#list xs! as x>x</#list>", "");
        assertOutput("<#list xs! as k, v>x</#list>", "");
        assertOutput("${xs!?length}", "0");
        assertOutput("${(xs!)?length}", "0"); // same
        assertOutput("${xs!?size}", "0");
    }
    
}
