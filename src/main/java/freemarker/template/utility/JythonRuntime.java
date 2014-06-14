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

package freemarker.template.utility;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * A crude first pass at an embeddable Jython interpreter
 */

public class JythonRuntime extends PythonInterpreter
    implements TemplateTransformModel
{
    public Writer getWriter(final Writer out,
                            final Map args)
    {
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        return new Writer() {
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                interpretBuffer();
                out.flush();
            }

            public void close() {
                interpretBuffer();
            }

            private void interpretBuffer() {
                synchronized(JythonRuntime.this) {
                    PyObject prevOut = systemState.stdout;
                    try {
                        setOut(out);
                        set("env", env);
                        exec(buf.toString());
                        buf.setLength(0);
                    } finally {
                        setOut(prevOut);
                    }
                }
            }
        };
    }
}
