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

import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.TestConfigurationBuilder;

import junit.framework.TestCase;

public class IncudeFromNamelessTest extends TestCase {

    public IncudeFromNamelessTest(String name) {
        super(name);
    }
    
    public void test() throws IOException, TemplateException {
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("i.f3ah", "[i]");
        loader.putTemplate("sub/i.f3ah", "[sub/i]");
        loader.putTemplate("import.f3ah", "<#assign x = 1>");

        Configuration cfg = new TestConfigurationBuilder().templateLoader(loader).build();

        Template t = new Template(null, new StringReader(
                    "<#include 'i.f3ah'>\n"
                    + "<#include '/i.f3ah'>\n"
                    + "<#include 'sub/i.f3ah'>\n"
                    + "<#include '/sub/i.f3ah'>"
                    + "<#import 'import.f3ah' as i>${i.x}"
                ),
                cfg);
        StringWriter out = new StringWriter();
        t.process(null, out);
        assertEquals("[i][i][sub/i][sub/i]1", out.toString());
    }

}
