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

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * A transform that captures the output of a block of FTL code and stores that in a variable.
 *
 * <p>As this transform is initially present in the shared variable set, you can always
 * access it from the templates:</p>
 *
 * <pre>
 * &lt;@capture_output var="captured"&gt;
 *   ...
 * &lt;/@capture_output&gt;
 * </pre>
 *
 * <p>And later in the template you can use the captured output:</p>
 *
 * ${captured}
 *
 * <p>This transform requires one of three parameters: <code>var</code>, <code>local</code>, or <code>global</code>.
 * Each of them specifies the name of the variable that stores the captured output, but the first creates a
 * variable in a name-space (as &lt;#assign&gt;), the second creates a macro-local variable (as &lt;#local&gt;),
 * and the last creates a global variable (as &lt;#global&gt;).
 * </p>
 * <p>In the case of an assignment within a namespace, there is an optional parameter
 * <code>namespace</code> that indicates in which namespace to do the assignment.
 * if this is omitted, the current namespace is used, and this will be, by far, the most
 * common usage pattern.</p>
 *
 * @deprecated Use block-assignments instead, like <code>&lt;assign x&gt;...&lt;/assign&gt;</code>.
 */
public class CaptureOutput implements TemplateTransformModel {

    public Writer getWriter(final Writer out, final Map args) throws TemplateModelException {
        String errmsg = "Must specify the name of the variable in "
                + "which to capture the output with the 'var' or 'local' or 'global' parameter.";
        if (args == null) throw new TemplateModelException(errmsg);

        boolean local = false, global=false;
        final TemplateModel nsModel = (TemplateModel) args.get("namespace");
        Object varNameModel = args.get("var");
        if (varNameModel == null) {
            varNameModel = args.get("local");
            if (varNameModel == null) {
                varNameModel = args.get("global");
                global = true;
            } else {
                local = true;
            }
            if (varNameModel == null) {
                throw new TemplateModelException(errmsg);
            }
        }
        if (args.size()==2) {
            if (nsModel == null) {
                throw new TemplateModelException("Second parameter can only be namespace");
            }
            if (local) {
                throw new TemplateModelException("Cannot specify namespace for a local assignment");
            }
            if (global) {
                throw new TemplateModelException("Cannot specify namespace for a global assignment");
            }
            if (!(nsModel instanceof Environment.Namespace)) {
                throw new TemplateModelException("namespace parameter does not specify a namespace. It is a " + nsModel.getClass().getName());
            }
        }
        else if (args.size() != 1) throw new TemplateModelException(
                "Bad parameters. Use only one of 'var' or 'local' or 'global' parameters.");

        if(!(varNameModel instanceof TemplateScalarModel)) {
            throw new TemplateModelException("'var' or 'local' or 'global' parameter doesn't evaluate to a string");
        }
        final String varName = ((TemplateScalarModel) varNameModel).getAsString();
        if(varName == null) {
            throw new TemplateModelException("'var' or 'local' or 'global' parameter evaluates to null string");
        }

        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        final boolean localVar = local;
        final boolean globalVar = global;

        return new Writer() {

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                SimpleScalar result = new SimpleScalar(buf.toString());
                try {
                    if (localVar) {
                        env.setLocalVariable(varName, result);
                    } else if (globalVar) {
                        env.setGlobalVariable(varName, result);
                    }
                    else {
                        if (nsModel == null) {
                            env.setVariable(varName, result);
                        } else {
                            ((Environment.Namespace) nsModel).put(varName, result);
                        }
                    }
                } catch (java.lang.IllegalStateException ise) { // if somebody uses 'local' outside a macro
                    throw new IOException("Could not set variable " + varName + ": " + ise.getMessage());
                }
            }
        };
    }
}
