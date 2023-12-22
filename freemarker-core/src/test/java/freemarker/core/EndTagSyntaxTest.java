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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class EndTagSyntaxTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        return new Configuration(Configuration.VERSION_2_3_29);
    }

    @Before
    public void setup() {
        addTemplate("common.ftl",
                "<#macro m a=1>${a}[<#nested />]</#macro>" +
                "<#assign ns={'m':m}>");
        getConfiguration().addAutoInclude("common.ftl");
    }

    @Test
    public void testSimple() throws IOException, TemplateException {
        assertOutput("<@m>nested</@>", "1[nested]");
        assertOutput("<@m a=2>nested</@>", "2[nested]");

        assertOutput("<@ns.m>nested</@ns.m>", "1[nested]");
        assertOutput("<@ns.m a=2>nested</@ns.m>", "2[nested]");

        assertOutput("<@m>nested</@m>", "1[nested]");
        assertOutput("<@m a=2>nested</@m>", "2[nested]");

        assertOutput("<@ns.m>nested</@ns.m>", "1[nested]");
        assertOutput("<@ns.m a=2>nested</@ns.m>", "2[nested]");

        assertErrorContains("<@ns.m a=2>nested</@m>", "</@ns.m>");
        assertErrorContains("<@m a=2>nested</@n>", "</@m>");
    }

    @Test
    public void testWithArgs() throws IOException, TemplateException {
        assertOutput("<@m?withArgs({})>nested</@m>", "1[nested]");
        assertOutput("<@m?withArgs({}) a=2>nested</@m>", "2[nested]");

        assertOutput("<@ns.m?withArgs({})>nested</@ns.m>", "1[nested]");
        assertOutput("<@ns.m?withArgs({}) a=2>nested</@ns.m>", "2[nested]");

        assertErrorContains("<@ns.m?withArgs({})>nested</@m>", "</@ns.m>");
        assertErrorContains("<@m?withArgs({})>nested</@n>", "</@m>");
    }

}
