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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import junit.framework.TestCase;
import freemarker.core.ParseException;

public class ExceptionTest extends TestCase {
    
    public ExceptionTest(String name) {
        super(name);
    }

    public void testParseExceptionSerializable() throws IOException, ClassNotFoundException {
        Configuration cfg = new Configuration();
        try {
            new Template("<string>", new StringReader("<@>"), cfg);
            fail();
        } catch (ParseException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }

    public void testTemplateErrorSerializable() throws IOException, ClassNotFoundException {
        Configuration cfg = new Configuration();
        Template tmp = new Template("<string>", new StringReader("${noSuchVar}"), cfg);
        try {
            tmp.process(Collections.EMPTY_MAP, new StringWriter());
            fail();
        } catch (TemplateException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }
    
}
