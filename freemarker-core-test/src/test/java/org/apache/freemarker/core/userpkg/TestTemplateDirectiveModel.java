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

package org.apache.freemarker.core.userpkg;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.model.TemplateDirectiveModel2;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.FTLUtil;

public abstract class TestTemplateDirectiveModel implements TemplateDirectiveModel2 {

    protected void printParam(String name, TemplateModel value, Writer out) throws IOException, TemplateModelException {
        printParam(name, value, out, false);
    }

    protected void printParam(String name, TemplateModel value, Writer out, boolean first)
            throws IOException, TemplateModelException {
        if (!first) {
            out.write(", ");
        }
        out.write(name);
        out.write("=");
        printValue(value, out);
    }

    private void printValue(TemplateModel value, Writer out) throws IOException, TemplateModelException {
        if (value == null) {
            out.write("null");
        } else if (value instanceof TemplateNumberModel) {
            out.write(((TemplateNumberModel) value).getAsNumber().toString());
        } else if (value instanceof TemplateScalarModel) {
            out.write(FTLUtil.toStringLiteral(((TemplateScalarModel) value).getAsString()));
        } else if (value instanceof TemplateSequenceModel) {
            int len = ((TemplateSequenceModel) value).size();
            out.write('[');
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    out.write(", ");
                }
                printValue(((TemplateSequenceModel) value).get(i), out);
            }
            out.write(']');
        } else if (value instanceof TemplateHashModelEx2) {
            TemplateHashModelEx2.KeyValuePairIterator it = ((TemplateHashModelEx2) value).keyValuePairIterator();
            out.write('{');
            while (it.hasNext()) {
                TemplateHashModelEx2.KeyValuePair kvp = it.next();

                printValue(kvp.getKey(), out);
                out.write(": ");
                printValue(kvp.getValue(), out);

                if (it.hasNext()) {
                    out.write(", ");
                }
            }
            out.write('}');
        } else {
            throw new IllegalArgumentException("Unsupported value class: " + value.getClass().getName());
        }
    }


}
