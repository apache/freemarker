/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.template;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;
import freemarker.cache.StringTemplateLoader;

public class IncudeFromNamelessTest extends TestCase {

    public IncudeFromNamelessTest(String name) {
        super(name);
    }
    
    public void test() throws IOException, TemplateException {
        Configuration cfg = new Configuration();
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("i.ftl", "[i]");
        tl.putTemplate("sub/i.ftl", "[sub/i]");
        tl.putTemplate("import.ftl", "<#assign x = 1>");
        cfg.setTemplateLoader(tl);
        
        Template t = new Template(null, new StringReader(
                    "<#include 'i.ftl'>\n"
                    + "<#include '/i.ftl'>\n"
                    + "<#include 'sub/i.ftl'>\n"
                    + "<#include '/sub/i.ftl'>"
                    + "<#import 'import.ftl' as i>${i.x}"
                ),
                cfg);
        StringWriter out = new StringWriter();
        t.process(null, out);
        assertEquals("[i][i][sub/i][sub/i]1", out.toString());
    }

}
